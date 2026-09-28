/* SeaSoft - trang chu: tai "Du an tieu bieu" va "Danh gia khach hang" tu /api/public.
   API loi hoac rong -> giu nguyen noi dung tinh san co trong HTML. */
(function () {
  "use strict";

  const esc = window.Labels.esc;

  // Phan tu chen sau khi hieu ung cuon da khoi tao -> nho animations.js animate vao
  function reveal(parent) {
    if (!document.documentElement.classList.contains("js-anim")) return;
    if (window.SeaAnim) window.SeaAnim.stagger(parent);
    else document.addEventListener("seaanim:ready", () => window.SeaAnim.stagger(parent), { once: true });
  }

  function portfolioCard(p) {
    const link = p.projectUrl
      ? `<a class="inline-flex items-center text-sm font-semibold text-primary hover:text-dark-navy transition-colors"
           href="${esc(p.projectUrl)}" target="_blank" rel="noopener noreferrer">Xem website
           <span class="material-symbols-outlined text-base ml-1">open_in_new</span></a>`
      : `<a class="inline-flex items-center text-sm font-semibold text-primary hover:text-dark-navy transition-colors" href="#contact">
           Tư vấn dự án tương tự <span class="material-symbols-outlined text-base ml-1">arrow_forward</span></a>`;
    const cover = p.coverImageUrl
      ? `<img alt="${esc(p.clientName || p.title)}" loading="lazy" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" src="${esc(p.coverImageUrl)}" />`
      : `<div class="w-full h-full flex items-center justify-center text-primary"><span class="material-symbols-outlined text-5xl">web</span></div>`;
    return `<div data-tilt="4" class="bg-white rounded-xl overflow-hidden shadow-card border border-border/60 group hover:shadow-cardHover transition-shadow duration-300">
      <div class="relative aspect-video overflow-hidden bg-gradient-to-tr from-dark-navy/20 to-secondary/30">${cover}</div>
      <div class="p-6">
        ${p.category ? `<span class="inline-block px-3 py-1 rounded-full bg-surface-alt text-primary font-medium text-xs mb-3">${esc(p.category)}</span>` : ""}
        <h3 class="font-headline text-lg font-bold text-dark-navy mb-2 line-clamp-2 group-hover:text-primary transition-colors">${esc(p.title)}</h3>
        <p class="text-xs text-text-muted mb-4 line-clamp-2">${esc(p.summary || "")}</p>
        ${link}
      </div></div>`;
  }

  function stars(n) {
    return `<span class="text-amber-500" aria-label="${n} trên 5 sao">${"★".repeat(n)}<span class="text-gray-300">${"★".repeat(5 - n)}</span></span>`;
  }

  function testimonialCard(t) {
    const avatar = t.avatarUrl
      ? `<img src="${esc(t.avatarUrl)}" alt="" loading="lazy" class="w-11 h-11 rounded-full object-cover" />`
      : `<span class="w-11 h-11 rounded-full bg-primary/15 text-dark-navy font-bold flex items-center justify-center">${esc(
          t.customerName.trim().charAt(0).toUpperCase(),
        )}</span>`;
    return `<figure class="bg-surface rounded-2xl border border-border/70 p-6 flex flex-col gap-4 shadow-card">
      ${stars(t.rating)}
      <blockquote class="text-sm text-text-secondary leading-relaxed flex-1">“${esc(t.quote)}”</blockquote>
      <figcaption class="flex items-center gap-3">${avatar}
        <span><b class="block text-dark-navy text-sm">${esc(t.customerName)}</b>
        <span class="text-xs text-text-muted">${esc([t.position, t.company].filter(Boolean).join(" · "))}</span></span>
      </figcaption></figure>`;
  }

  fetch(Auth.API_BASE_URL + "/api/public/portfolio")
    .then((r) => (r.ok ? r.json() : []))
    .then((items) => {
      const grid = document.getElementById("portfolio-grid");
      if (!grid || !items.length) return;
      grid.innerHTML = items.map(portfolioCard).join("");
      reveal(grid);
    })
    .catch(() => {});

  fetch(Auth.API_BASE_URL + "/api/public/testimonials")
    .then((r) => (r.ok ? r.json() : []))
    .then((items) => {
      const section = document.getElementById("testimonials");
      if (!section || !items.length) return;
      const grid = document.getElementById("testimonial-grid");
      grid.dataset.stagger = "fade-up";
      grid.innerHTML = items.map(testimonialCard).join("");
      section.classList.remove("hidden");
      reveal(grid);
    })
    .catch(() => {});
})();
