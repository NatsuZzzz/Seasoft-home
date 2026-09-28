/*
 * SeaSoft - hieu ung chuyen trang giua cac trang .html noi bo.
 * Khong phu thuoc thu vien. Tat khi prefers-reduced-motion.
 */
(function () {
  "use strict";

  const FLAG = "seasoft_pt"; // danh dau: trang nay duoc mo qua hieu ung chuyen trang
  const LEAVE_MS = 450;
  const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  const overlay = document.createElement("div");
  overlay.className = "page-transition";
  overlay.setAttribute("aria-hidden", "true");
  overlay.innerHTML = '<span class="material-symbols-outlined">waves</span>';

  let cameFromTransition = false;
  try {
    cameFromTransition = sessionStorage.getItem(FLAG) === "1";
    sessionStorage.removeItem(FLAG);
  } catch {
    /* storage bi chan -> bo qua */
  }

  function mount() {
    if (reduced) return;
    // Luc nghi man che bi an han (display:none) -> du transition co ket
    // (tab an khong chay animation) cung khong bao gio che mat trang
    overlay.hidden = !cameFromTransition;
    document.body.appendChild(overlay);
    if (cameFromTransition) {
      overlay.classList.add("is-covering");
      // Ep trinh duyet tinh style "phu kin" roi moi doi class -> transition chay.
      // Khong dung requestAnimationFrame: tab an khong chay rAF.
      getComputedStyle(overlay).clipPath;
      overlay.classList.remove("is-covering");
      overlay.classList.add("is-revealing");
      setTimeout(() => {
        overlay.classList.remove("is-revealing");
        overlay.hidden = true;
      }, LEAVE_MS + 50);
    }
  }

  function isInternalPage(a) {
    if (!a || a.target === "_blank" || a.hasAttribute("download")) return false;
    const href = a.getAttribute("href");
    if (!href || href.startsWith("#") || href.startsWith("mailto:") || href.startsWith("tel:")) return false;
    const url = new URL(a.href, location.href);
    const samePage = url.pathname === location.pathname && url.search === location.search;
    return url.origin === location.origin && /\.html$/.test(url.pathname) && !samePage;
  }

  document.addEventListener("click", (e) => {
    if (reduced || e.defaultPrevented || e.button !== 0) return;
    if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return; // mo tab moi
    const a = e.target.closest("a");
    if (!isInternalPage(a)) return;
    e.preventDefault();
    try {
      sessionStorage.setItem(FLAG, "1");
    } catch {
      /* bo qua */
    }
    overlay.classList.remove("is-revealing", "is-covering");
    overlay.hidden = false;
    getComputedStyle(overlay).clipPath; // tinh trang thai nghi truoc -> transition dang len
    overlay.classList.add("is-leaving");
    setTimeout(() => (location.href = a.href), LEAVE_MS);
  });

  // Bam Back tu bfcache: trang hien lai voi man che dang phu -> go ra
  window.addEventListener("pageshow", (e) => {
    if (!e.persisted) return;
    overlay.classList.remove("is-leaving", "is-covering", "is-revealing");
    overlay.hidden = true;
  });

  if (document.body) mount();
  else document.addEventListener("DOMContentLoaded", mount);
})();
