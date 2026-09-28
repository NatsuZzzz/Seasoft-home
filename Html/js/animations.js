/*
 * SeaSoft - hieu ung cuon trang (GSAP + ScrollTrigger + Lenis)
 *
 * Gan hieu ung bang data-attribute, trang nao cung dung duoc:
 *   data-reveal="fade-up|fade-down|fade-left|fade-right|scale|blur"  hien khi cuon toi
 *   data-reveal-delay="0.2"                                          tre (giay)
 *   data-stagger="fade-up"          cac con truc tiep hien lan luot
 *   data-split                      headline hien tung chu
 *   data-parallax="0.15"            troi cham/nhanh hon khi cuon (desktop)
 *   data-counter="50" data-suffix="+"   chay so
 *   data-tilt                       nghieng 3D theo chuot
 *   data-magnetic                   nut hut theo chuot
 *   data-clip-reveal                section mo rong tu khung bo tron ra full man hinh
 *   data-process                    khu "Quy trinh": pin + scrub (desktop)
 *
 * Can: <html> duoc them class "js-anim" boi script trong <head> (xem Page.html).
 */
(function () {
  "use strict";

  const root = document.documentElement;
  const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  // Khong co GSAP (CDN loi) hoac nguoi dung tat chuyen dong -> hien het, dung lai
  if (reduced || !window.gsap || !window.ScrollTrigger) {
    root.classList.remove("js-anim");
    initStaticFallbacks();
    return;
  }

  // Trang thai bat dau cua tung kieu reveal
  const FROM = {
    "fade-up": { y: 48 },
    "fade-down": { y: -48 },
    "fade-left": { x: 64 },
    "fade-right": { x: -64 },
    scale: { scale: 0.88 },
    blur: { filter: "blur(12px)", y: 24 },
  };

  gsap.registerPlugin(ScrollTrigger);
  gsap.defaults({ ease: "power3.out", duration: 0.9 });

  try {
    const lenis = initSmoothScroll();
    initScrollProgress();
    initHeader();
    initSplitText();
    initReveals();
    initCounters();
    initProcess();
    initClipReveal();
    initParallax();
    initTilt();
    initMagnetic();
    initAnchorLinks(lenis);
    initScrollSpy();
    root.classList.add("anim-ready"); // bao cho failsafe trong <head> la da chay xong
  } catch (err) {
    // Loi giua chung -> dung het hieu ung, tra trang ve trang thai tinh day du
    console.error("[animations] init failed", err);
    ScrollTrigger.getAll().forEach((t) => t.kill());
    gsap.set("[data-reveal], [data-stagger] > *, .split-word > span, [data-parallax]", { clearProps: "all" });
    root.classList.remove("js-anim");
    initStaticFallbacks();
  }

  // Anh/font tai xong co the doi chieu cao trang -> tinh lai vi tri trigger
  window.addEventListener("load", () => ScrollTrigger.refresh());
  document.fonts?.ready.then(() => ScrollTrigger.refresh());

  /* ------------------------------------------------------------------ */

  function initSmoothScroll() {
    if (!window.Lenis) return null;
    // Lenis tu xu ly cuon muot, bo scroll-behavior:smooth cua CSS de khong danh nhau
    root.classList.remove("scroll-smooth");
    const lenis = new Lenis({ lerp: 0.1, wheelMultiplier: 1 });
    lenis.on("scroll", ScrollTrigger.update);
    // Pin cua ScrollTrigger lam trang dai them -> bao Lenis tinh lai gioi han cuon
    ScrollTrigger.addEventListener("refresh", () => lenis.resize());
    gsap.ticker.add((time) => lenis.raf(time * 1000));
    gsap.ticker.lagSmoothing(0);
    return lenis;
  }

  function initScrollProgress() {
    const bar = document.createElement("div");
    bar.className = "scroll-progress";
    bar.setAttribute("aria-hidden", "true");
    document.body.appendChild(bar);
    gsap.to(bar, {
      scaleX: 1,
      ease: "none",
      scrollTrigger: { start: 0, end: "max", scrub: 0.3 },
    });
  }

  function initHeader() {
    const header = document.querySelector(".site-header");
    if (!header) return;
    ScrollTrigger.create({
      start: 40,
      end: "max",
      onToggle: (self) => header.classList.toggle("is-scrolled", self.isActive),
    });
  }

  // Tach headline thanh tung tu (giu dau tieng Viet, khong tach ky tu)
  function initSplitText() {
    document.querySelectorAll("[data-split]").forEach((el) => {
      const words = el.textContent.trim().split(/\s+/);
      el.setAttribute("aria-label", words.join(" "));
      el.innerHTML = words
        .map((w) => `<span class="split-word" aria-hidden="true"><span>${escapeHtml(w)}</span></span>`)
        .join(" ");
      el.classList.add("is-split");
      gsap.fromTo(
        el.querySelectorAll(".split-word > span"),
        { yPercent: 110, opacity: 0, rotate: 4 },
        {
          yPercent: 0,
          opacity: 1,
          rotate: 0,
          duration: 1,
          ease: "power4.out",
          stagger: 0.06,
          delay: parseFloat(el.dataset.revealDelay || 0.1),
          scrollTrigger: triggerFor(el),
        },
      );
    });
  }

  function fromVars(type) {
    return { opacity: 0, ...(FROM[type] || FROM["fade-up"]) };
  }

  function toVars(type) {
    const v = { opacity: 1, x: 0, y: 0, scale: 1 };
    if (type === "blur") v.filter = "blur(0px)";
    return v;
  }

  function triggerFor(el) {
    // Phan tu da nam trong man hinh luc tai trang -> chay ngay
    return { trigger: el, start: "top 88%", once: true };
  }

  function initReveals() {
    document.querySelectorAll("[data-reveal]").forEach((el) => {
      const type = el.dataset.reveal;
      gsap.fromTo(el, fromVars(type), {
        ...toVars(type),
        delay: parseFloat(el.dataset.revealDelay || 0),
        scrollTrigger: triggerFor(el),
        clearProps: "transform,filter",
      });
    });

    document.querySelectorAll("[data-stagger]").forEach((parent) => {
      const type = parent.dataset.stagger || "fade-up";
      gsap.fromTo(parent.children, fromVars(type), {
        ...toVars(type),
        stagger: parseFloat(parent.dataset.staggerEach || 0.12),
        delay: parseFloat(parent.dataset.revealDelay || 0),
        scrollTrigger: triggerFor(parent),
        clearProps: "transform,filter",
      });
    });
  }

  function initCounters() {
    document.querySelectorAll("[data-counter]").forEach((el) => {
      const target = parseFloat(el.dataset.counter);
      const suffix = el.dataset.suffix || "";
      const obj = { value: 0 };
      el.textContent = "0" + suffix;
      gsap.to(obj, {
        value: target,
        duration: 1.8,
        ease: "power2.out",
        scrollTrigger: triggerFor(el),
        onUpdate: () => (el.textContent = Math.round(obj.value) + suffix),
      });
    });
  }

  // Quy trinh: desktop ghim section, cuon toi dau buoc sang toi do
  function initProcess() {
    const section = document.querySelector("[data-process]");
    if (!section) return;
    const steps = [...section.querySelectorAll(".process-step")];
    const fill = section.querySelector(".process-line-fill");
    const mm = gsap.matchMedia();

    mm.add("(min-width: 1024px)", () => {
      section.classList.add("process-pinned");
      const setActive = (progress) => {
        // Chia deu thanh tien trinh cho so buoc; buoc dau sang ngay khi bat dau
        const count = Math.min(steps.length, Math.floor(progress * steps.length + 0.35) + 1);
        steps.forEach((s, i) => s.classList.toggle("is-active", i < count));
      };
      setActive(0);
      const tween = gsap.to(fill, {
        scaleX: 1,
        ease: "none",
        scrollTrigger: {
          trigger: section,
          start: "center center",
          end: () => "+=" + window.innerHeight * 1.2,
          pin: true,
          scrub: 0.6,
          onUpdate: (self) => setActive(self.progress),
        },
      });
      return () => {
        section.classList.remove("process-pinned");
        steps.forEach((s) => s.classList.remove("is-active"));
        tween.scrollTrigger?.kill();
      };
    });

    // Mobile/tablet: khong ghim, tung buoc tu sang len khi cuon toi
    mm.add("(max-width: 1023px)", () => {
      const triggers = steps.map((s) =>
        ScrollTrigger.create({
          trigger: s,
          start: "top 75%",
          onEnter: () => s.classList.add("is-active"),
        }),
      );
      return () => triggers.forEach((t) => t.kill());
    });
  }

  // Section "chuyen canh": luc dau nhu 1 the bo tron, cuon toi thi no ra full man hinh
  function initClipReveal() {
    document.querySelectorAll("[data-clip-reveal]").forEach((el) => {
      gsap.fromTo(
        el,
        { clipPath: "inset(6% 5% 6% 5% round 36px)" },
        {
          clipPath: "inset(0% 0% 0% 0% round 0px)",
          ease: "none",
          scrollTrigger: { trigger: el, start: "top 95%", end: "top 25%", scrub: 0.5 },
        },
      );
    });
  }

  function initParallax() {
    const mm = gsap.matchMedia();
    mm.add("(min-width: 1024px)", () => {
      document.querySelectorAll("[data-parallax]").forEach((el) => {
        const speed = parseFloat(el.dataset.parallax || 0.15);
        gsap.fromTo(
          el,
          { yPercent: -speed * 50 },
          {
            yPercent: speed * 50,
            ease: "none",
            scrollTrigger: {
              trigger: el.closest("section") || el,
              start: "top bottom",
              end: "bottom top",
              scrub: true,
            },
          },
        );
      });
    });
  }

  // Nghieng 3D nhe theo vi tri chuot (chi thiet bi co chuot)
  function initTilt() {
    if (!window.matchMedia("(hover: hover) and (pointer: fine)").matches) return;
    document.querySelectorAll("[data-tilt]").forEach((el) => {
      const max = parseFloat(el.dataset.tilt || 6);
      const rx = gsap.quickTo(el, "rotationX", { duration: 0.5, ease: "power2.out" });
      const ry = gsap.quickTo(el, "rotationY", { duration: 0.5, ease: "power2.out" });
      gsap.set(el, { transformPerspective: 900 });
      el.addEventListener("pointermove", (e) => {
        const r = el.getBoundingClientRect();
        const px = (e.clientX - r.left) / r.width - 0.5;
        const py = (e.clientY - r.top) / r.height - 0.5;
        rx(-py * max * 2);
        ry(px * max * 2);
      });
      el.addEventListener("pointerleave", () => {
        rx(0);
        ry(0);
      });
    });
  }

  // Nut "hut" nhe theo chuot
  function initMagnetic() {
    if (!window.matchMedia("(hover: hover) and (pointer: fine)").matches) return;
    document.querySelectorAll("[data-magnetic]").forEach((el) => {
      const strength = parseFloat(el.dataset.magnetic || 0.3);
      const x = gsap.quickTo(el, "x", { duration: 0.4, ease: "power3.out" });
      const y = gsap.quickTo(el, "y", { duration: 0.4, ease: "power3.out" });
      el.addEventListener("pointermove", (e) => {
        const r = el.getBoundingClientRect();
        x((e.clientX - (r.left + r.width / 2)) * strength);
        y((e.clientY - (r.top + r.height / 2)) * strength);
      });
      el.addEventListener("pointerleave", () => {
        x(0);
        y(0);
      });
    });
  }

  // Link #anchor: cuon muot bang Lenis, tru chieu cao header dinh
  function initAnchorLinks(lenis) {
    if (!lenis) return;
    document.addEventListener("click", (e) => {
      const a = e.target.closest('a[href^="#"]');
      if (!a) return;
      const id = a.getAttribute("href");
      if (id === "#") return;
      const target = document.querySelector(id);
      if (!target) return;
      e.preventDefault();
      lenis.resize();
      const header = document.querySelector(".site-header");
      lenis.scrollTo(target, { offset: -(header ? 64 : 0), duration: 1.2 });
      history.replaceState(null, "", id);
    });
  }

  // Menu desktop sang theo section dang xem
  function initScrollSpy() {
    const ACTIVE = ["text-primary", "font-semibold", "border-b-2", "border-primary", "pb-1"];
    const IDLE = ["text-text-secondary"];
    const links = [...document.querySelectorAll('.site-header nav a[href^="#"]')];
    const setActive = (id) =>
      links.forEach((a) => {
        const on = a.getAttribute("href") === "#" + id;
        a.classList.toggle("is-current", on);
        ACTIVE.forEach((c) => a.classList.toggle(c, on));
        IDLE.forEach((c) => a.classList.toggle(c, !on));
      });
    const triggers = [];
    links.forEach((a) => {
      const section = document.querySelector(a.getAttribute("href"));
      if (!section) return;
      triggers.push(
        ScrollTrigger.create({
          trigger: section,
          start: "top center",
          end: "bottom center",
          onToggle: (self) => {
            if (self.isActive) return setActive(section.id);
            // Roi section ma khong vao section nao khac (vd: ve hero) -> tat highlight
            const current = triggers.find((t) => t.isActive);
            setActive(current ? current.trigger.id : null);
          },
        }),
      );
    });
    setActive(null);
  }

  // Khi khong chay GSAP: van cho so hien dung va buoc quy trinh sang het
  function initStaticFallbacks() {
    document.querySelectorAll(".process-step").forEach((s) => s.classList.add("is-active"));
    document.querySelectorAll(".process-line-fill").forEach((f) => (f.style.transform = "scaleX(1)"));
  }

  function escapeHtml(s) {
    return s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
  }
})();
