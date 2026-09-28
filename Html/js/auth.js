/*
 * SeaSoft - quan ly dang nhap phia frontend (dung chung cho moi trang)
 *
 * - Luu access token + refresh token (localStorage neu "ghi nho", nguoc lai sessionStorage)
 * - Auth.api(): fetch kem Bearer token, gap 401 thi tu goi /api/auth/refresh roi thu lai 1 lan
 * - Auth.requireAuth(): trang can dang nhap goi o dau trang
 * - Header: phan tu co data-auth="guest" / data-auth="user" tu an/hien,
 *   data-auth-name hien ten, data-auth-logout la nut dang xuat
 */
(function () {
  "use strict";

  // Mo bang file:// hoac Live Server (cong khac 8080) -> goi API o localhost:8080.
  // Khi Spring Boot serve frontend (dev :8080 hoac prod) -> cung origin.
  const isLocal = ["localhost", "127.0.0.1", ""].includes(location.hostname);
  const API_BASE_URL =
    window.SEASOFT_API_BASE_URL ??
    (location.protocol === "file:" || (isLocal && location.port !== "8080")
      ? "http://localhost:8080"
      : "");

  const KEYS = {
    token: "seasoft_token",
    refresh: "seasoft_refresh_token",
    user: "seasoft_user",
  };

  function store() {
    // Token dang nam o dau thi doc/ghi o do
    return localStorage.getItem(KEYS.refresh) ? localStorage : sessionStorage;
  }

  function safeParse(json) {
    try {
      return JSON.parse(json);
    } catch {
      return null;
    }
  }

  const Auth = {
    API_BASE_URL,

    // data = response cua /login, /register, /refresh
    save(data, remember = true) {
      Auth.clear();
      const s = remember ? localStorage : sessionStorage;
      s.setItem(KEYS.token, data.token);
      s.setItem(KEYS.refresh, data.refreshToken);
      s.setItem(
        KEYS.user,
        JSON.stringify({ email: data.email, fullName: data.fullName, role: data.role }),
      );
    },

    clear() {
      for (const s of [localStorage, sessionStorage]) {
        Object.values(KEYS).forEach((k) => s.removeItem(k));
      }
    },

    // Chi de an/hien giao dien; quyen that do backend kiem tra
    isStaff: () => ["STAFF", "MANAGER", "ADMIN"].includes(Auth.user()?.role),
    isManager: () => ["MANAGER", "ADMIN"].includes(Auth.user()?.role),

    token: () => store().getItem(KEYS.token),
    refreshToken: () => store().getItem(KEYS.refresh),
    user: () => safeParse(store().getItem(KEYS.user)),
    isLoggedIn: () => !!store().getItem(KEYS.refresh),

    updateUser(patch) {
      const s = store();
      s.setItem(KEYS.user, JSON.stringify({ ...Auth.user(), ...patch }));
    },

    // Goi API khong can token; nem Error(message) neu that bai, err.status/err.errors kem theo
    async request(path, { method = "GET", body, headers = {} } = {}) {
      const res = await fetch(API_BASE_URL + path, {
        method,
        headers: {
          ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
          ...headers,
        },
        body: body !== undefined ? JSON.stringify(body) : undefined,
      });
      const data = res.status === 204 ? null : await res.json().catch(() => null);
      if (!res.ok) {
        const err = new Error(data?.message || "Đã xảy ra lỗi. Vui lòng thử lại.");
        err.status = res.status;
        err.errors = data?.errors || {};
        throw err;
      }
      return data;
    },

    // Goi API can dang nhap, tu refresh token khi het han
    async api(path, options = {}) {
      const withToken = () =>
        Auth.request(path, {
          ...options,
          headers: { ...(options.headers || {}), Authorization: "Bearer " + Auth.token() },
        });
      try {
        return await withToken();
      } catch (err) {
        if (err.status !== 401 || !Auth.refreshToken()) throw err;
        await Auth.refresh();
        return withToken();
      }
    },

    _refreshing: null,
    // Nhieu request cung 401 mot luc -> chi goi refresh 1 lan
    refresh() {
      if (!Auth._refreshing) {
        const remember = store() === localStorage;
        Auth._refreshing = Auth.request("/api/auth/refresh", {
          method: "POST",
          body: { refreshToken: Auth.refreshToken() },
        })
          .then((data) => Auth.save(data, remember))
          .catch((err) => {
            Auth.clear();
            Auth.goToLogin();
            throw err;
          })
          .finally(() => (Auth._refreshing = null));
      }
      return Auth._refreshing;
    },

    async logout() {
      const rt = Auth.refreshToken();
      Auth.clear();
      if (rt) {
        await Auth.request("/api/auth/logout", {
          method: "POST",
          body: { refreshToken: rt },
        }).catch(() => {});
      }
      location.href = "Page.html";
    },

    goToLogin() {
      const next = location.pathname.split("/").pop() + location.search;
      location.href = "login.html?next=" + encodeURIComponent(next);
    },

    requireAuth() {
      if (!Auth.isLoggedIn()) {
        Auth.goToLogin();
        return false;
      }
      return true;
    },

    // Chi cho redirect ve trang noi bo (chong open redirect qua ?next=)
    nextUrl(fallback = "Page.html") {
      const next = new URLSearchParams(location.search).get("next");
      return next && /^[\w-]+\.html(\?[^#]*)?$/.test(next) ? next : fallback;
    },

    renderHeader() {
      const loggedIn = Auth.isLoggedIn();
      const user = Auth.user();
      document.querySelectorAll('[data-auth="guest"]').forEach((el) => {
        el.classList.toggle("hidden", loggedIn);
      });
      document.querySelectorAll('[data-auth="user"]').forEach((el) => {
        el.classList.toggle("hidden", !loggedIn);
      });
      document.querySelectorAll('[data-auth="staff"]').forEach((el) => {
        el.classList.toggle("hidden", !(loggedIn && Auth.isStaff()));
      });
      document.querySelectorAll("[data-auth-name]").forEach((el) => {
        el.textContent = user?.fullName || user?.email || "";
      });
      document.querySelectorAll("[data-auth-logout]:not([data-bound])").forEach((el) => {
        el.dataset.bound = "1"; // renderHeader co the goi lai nhieu lan -> khong gan trung
        el.addEventListener("click", (e) => {
          e.preventDefault();
          Auth.logout();
        });
      });
    },
  };

  window.Auth = Auth;
  document.addEventListener("DOMContentLoaded", Auth.renderHeader);
})();
