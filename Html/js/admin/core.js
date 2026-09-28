/*
 * SeaSoft admin - khung chung: dang ky tab, dieu huong bang #hash, drawer, toast.
 * Moi tab la 1 module goi AdminApp.register({ id, label, icon, roles, init, show }).
 */
(function () {
  "use strict";

  const tabs = [];
  let current = null;

  const App = {
    register(tab) {
      tabs.push(tab);
    },

    async start() {
      if (!Auth.isLoggedIn()) return; // requireAuth da chuyen trang
      // Role luu trong trinh duyet co the cu (admin vua doi vai tro) -> lay lai tu server
      try {
        const me = await Auth.api("/api/users/me");
        if (me.role !== Auth.user()?.role || me.fullName !== Auth.user()?.fullName) {
          Auth.updateUser({ role: me.role, fullName: me.fullName });
          Auth.renderHeader();
        }
      } catch {
        return; // phien het han -> Auth da chuyen sang trang dang nhap
      }
      if (!Auth.isStaff()) {
        document.getElementById("forbidden").classList.remove("hidden");
        return;
      }
      document.getElementById("console").classList.remove("hidden");
      document.getElementById("my-role").textContent = Labels.role[Auth.user().role] || "";

      const nav = document.getElementById("tab-nav");
      App.visibleTabs().forEach((t) => {
        const a = document.createElement("a");
        a.href = "#" + t.id;
        a.className = "tab-link";
        a.dataset.tabLink = t.id;
        a.innerHTML = `<span class="material-symbols-outlined text-xl">${t.icon}</span>${t.label}`;
        nav.appendChild(a);
      });

      document.getElementById("drawer-close").addEventListener("click", App.closeDrawer);
      document.getElementById("drawer-backdrop").addEventListener("click", App.closeDrawer);
      document.addEventListener("keydown", (e) => e.key === "Escape" && App.closeDrawer());
      window.addEventListener("hashchange", App.route);
      App.route();
    },

    visibleTabs() {
      const role = Auth.user()?.role;
      return tabs.filter((t) => !t.roles || t.roles.includes(role));
    },

    route() {
      const visible = App.visibleTabs();
      const id = location.hash.slice(1);
      const tab = visible.find((t) => t.id === id) || visible[0];
      if (!tab) return;
      document.querySelectorAll("[data-tab]").forEach((s) => s.classList.toggle("hidden", s.dataset.tab !== tab.id));
      document.querySelectorAll("[data-tab-link]").forEach((a) =>
        a.classList.toggle("is-active", a.dataset.tabLink === tab.id),
      );
      if (!tab._inited) {
        tab._inited = true;
        tab.init?.();
      }
      current = tab;
      tab.show?.();
    },

    openDrawer(title, html) {
      document.getElementById("drawer-title").textContent = title;
      document.getElementById("drawer-body").innerHTML = html;
      document.body.classList.add("drawer-open");
      return document.getElementById("drawer-body");
    },

    closeDrawer() {
      document.body.classList.remove("drawer-open");
    },

    toast(message, isError = false) {
      const t = document.getElementById("toast");
      t.textContent = message;
      t.classList.toggle("is-error", isError);
      t.classList.add("is-visible");
      clearTimeout(t._timer);
      t._timer = setTimeout(() => t.classList.remove("is-visible"), 2800);
    },

    debounce(fn, ms = 300) {
      let timer;
      return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), ms);
      };
    },

    // Phan trang chung: tra ve ham cap nhat nut Truoc/Sau
    pager(prevId, nextId, labelId, onChange) {
      const prev = document.getElementById(prevId);
      const next = document.getElementById(nextId);
      const label = document.getElementById(labelId);
      let page = 0;
      prev.addEventListener("click", () => onChange(--page));
      next.addEventListener("click", () => onChange(++page));
      return {
        reset: () => (page = 0),
        get page() {
          return page;
        },
        update(data) {
          page = data.page;
          prev.disabled = page <= 0;
          next.disabled = page >= data.totalPages - 1;
          label.textContent = data.totalPages ? `Trang ${page + 1} / ${data.totalPages}` : "";
        },
      };
    },

    current: () => current,
  };

  window.AdminApp = App;
})();
