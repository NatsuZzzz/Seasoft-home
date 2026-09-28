/* SeaSoft - phan dung chung cho trang khach (dashboard, chi tiet du an) */
(function () {
  "use strict";

  const Portal = {
    // Thanh tien do: render width 0 roi dat width that -> CSS transition chay
    progressBar(percent) {
      return `<div class="progress-track" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="${percent}">
        <div class="progress-fill" data-progress="${percent}"></div></div>`;
    },

    animateProgress(root = document) {
      // Ep trinh duyet ve trang thai width 0 truoc
      root.querySelectorAll("[data-progress]").forEach((el) => el.getBoundingClientRect());
      requestAnimationFrame(() =>
        root.querySelectorAll("[data-progress]").forEach((el) => (el.style.width = el.dataset.progress + "%")),
      );
      // Tab an khong chay rAF -> van dat gia tri cuoi
      setTimeout(
        () => root.querySelectorAll("[data-progress]").forEach((el) => (el.style.width = el.dataset.progress + "%")),
        300,
      );
    },

    header() {
      return `<header class="w-full px-6 pt-6 pb-4">
        <div class="max-w-screen-lg mx-auto flex items-center justify-between gap-4">
          <a href="Page.html" class="flex items-center gap-2 font-headline text-2xl font-bold tracking-tight text-dark-navy" aria-label="SeaSoft - Trang chủ">
            <span class="w-9 h-9 rounded-lg bg-surface-alt text-primary flex items-center justify-center">
              <span class="material-symbols-outlined text-xl">waves</span></span>
            <span>Sea<span class="text-primary">Soft</span></span>
          </a>
          <nav class="flex items-center gap-2 text-sm font-semibold">
            <a href="dashboard.html" class="px-3 py-2 rounded-lg hover:bg-surface-alt hidden sm:inline-flex">Dự án của tôi</a>
            <a href="profile.html" class="px-3 py-2 rounded-lg hover:bg-surface-alt inline-flex items-center gap-1">
              <span class="material-symbols-outlined text-lg">account_circle</span><span class="hidden sm:inline" data-auth-name></span></a>
            <button data-auth-logout class="px-3 py-2 rounded-lg border border-border hover:border-primary hover:text-primary" title="Đăng xuất">
              <span class="material-symbols-outlined text-lg align-middle">logout</span></button>
          </nav>
        </div>
      </header>`;
    },
  };

  window.Portal = Portal;
})();
