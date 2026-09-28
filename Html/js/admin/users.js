/* SeaSoft admin - tab "Nguoi dung": danh sach, them nhan vien (email moi), khoa/mo, doi vai tro */
(function () {
  "use strict";

  const L = window.Labels;
  const esc = L.esc;
  const USER_STATUS = { ACTIVE: "Hoạt động", INACTIVE: "Chưa kích hoạt", SUSPENDED: "Đã khoá", PENDING_VERIFICATION: "Chờ xác thực" };
  const STATUS_BADGE = {
    ACTIVE: "bg-emerald-50 text-emerald-700 ring-emerald-200",
    SUSPENDED: "bg-rose-50 text-rose-700 ring-rose-200",
  };
  const STAFF_ROLES = ["STAFF", "MANAGER", "ADMIN"];
  const input = "w-full border border-border rounded-xl py-2.5 px-3 text-sm focus:border-primary focus:ring-primary/20 focus:ring-4";
  const label = "text-xs font-semibold text-text-muted";
  const state = { role: "", status: "", q: "" };
  let pager;

  const badge = (text, cls) =>
    `<span class="inline-flex px-2.5 py-1 rounded-full text-xs font-semibold ring-1 ${cls || "bg-gray-50 text-gray-600 ring-gray-200"}">${esc(text)}</span>`;
  const roleBadge = (r) =>
    badge(L.role[r] || r, r === "ADMIN" ? "bg-dark-navy text-white ring-dark-navy" : r === "CUSTOMER" ? "" : "bg-surface-alt text-dark-navy ring-secondary/60");

  async function load() {
    const p = new URLSearchParams({ page: pager.page, size: 15 });
    Object.entries(state).forEach(([k, v]) => v && p.set(k, v));
    try {
      const data = await Auth.api("/api/admin/users?" + p);
      document.getElementById("usr-total").textContent = data.totalItems;
      const tbody = document.getElementById("usr-rows");
      tbody.innerHTML = data.items
        .map(
          (u, i) => `<tr class="row-link row-in" style="animation-delay:${i * 30}ms" data-id="${u.id}" tabindex="0">
            <td class="px-4 py-3"><p class="font-semibold">${esc(u.fullName)}</p><p class="text-xs text-text-muted">${esc(u.email)}</p></td>
            <td class="px-4 py-3">${roleBadge(u.role)}</td>
            <td class="px-4 py-3">${badge(USER_STATUS[u.status] || u.status, STATUS_BADGE[u.status])}</td>
            <td class="px-4 py-3 hidden md:table-cell text-xs">${L.date(u.lastLoginAt)}</td>
            <td class="px-4 py-3 hidden lg:table-cell text-xs">${L.date(u.createdAt, false)}</td>
          </tr>`,
        )
        .join("");
      tbody.querySelectorAll("[data-id]").forEach((tr) => tr.addEventListener("click", () => openDetail(tr.dataset.id)));
      pager.update(data);
    } catch (err) {
      AdminApp.toast(err.message, true);
    }
  }

  function staffFields(p = {}) {
    return `<div class="space-y-1.5"><label class="${label}" for="f-code">Mã nhân viên</label>
        <input id="f-code" maxlength="30" value="${esc(p.employeeCode || "")}" class="${input}"></div>
      <div class="space-y-1.5"><label class="${label}" for="f-dept">Phòng ban</label>
        <input id="f-dept" maxlength="100" value="${esc(p.department || "")}" class="${input}" placeholder="Design, Dev, Sales…"></div>
      <div class="space-y-1.5"><label class="${label}" for="f-pos">Chức vụ</label>
        <input id="f-pos" maxlength="100" value="${esc(p.position || "")}" class="${input}"></div>
      <div class="space-y-1.5"><label class="${label}" for="f-hire">Ngày vào làm</label>
        <input id="f-hire" type="date" value="${p.hireDate || ""}" class="${input}"></div>`;
  }

  const readStaffFields = (b) => ({
    employeeCode: b.querySelector("#f-code").value.trim(),
    department: b.querySelector("#f-dept").value.trim(),
    position: b.querySelector("#f-pos").value.trim(),
    hireDate: b.querySelector("#f-hire").value || null,
  });

  function openCreate() {
    const roles = Auth.user().role === "ADMIN" ? STAFF_ROLES : ["STAFF"];
    const b = AdminApp.openDrawer("Thêm nhân viên", `
      <form id="n-form" class="grid grid-cols-2 gap-3" novalidate>
        <div class="col-span-2 space-y-1.5"><label class="${label}" for="n-name">Họ tên *</label><input id="n-name" maxlength="150" class="${input}"></div>
        <div class="space-y-1.5"><label class="${label}" for="n-email">Email *</label><input id="n-email" type="email" maxlength="150" class="${input}"></div>
        <div class="space-y-1.5"><label class="${label}" for="n-phone">Số điện thoại</label><input id="n-phone" maxlength="20" class="${input}"></div>
        <div class="col-span-2 space-y-1.5"><label class="${label}" for="n-role">Vai trò *</label>
          <select id="n-role" class="${input}">${roles.map((r) => `<option value="${r}">${L.role[r]}</option>`).join("")}</select></div>
        ${staffFields()}
        <p class="col-span-2 text-xs text-text-muted">Hệ thống gửi email mời đặt mật khẩu (link có hạn 48 giờ). Bạn không cần đặt mật khẩu cho nhân viên.</p>
        <p id="n-error" class="col-span-2 hidden text-sm text-red-600 bg-red-50 rounded-xl p-3"></p>
        <button class="col-span-2 bg-primary text-white rounded-xl py-3 font-semibold hover:bg-[#34b6d0] disabled:opacity-50">Tạo tài khoản & gửi lời mời</button>
      </form>`);
    b.querySelector("#n-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const btn = e.target.querySelector("button");
      const err = b.querySelector("#n-error");
      btn.disabled = true;
      try {
        const u = await Auth.api("/api/admin/users", {
          method: "POST",
          body: {
            fullName: b.querySelector("#n-name").value.trim(),
            email: b.querySelector("#n-email").value.trim(),
            phone: b.querySelector("#n-phone").value.trim(),
            role: b.querySelector("#n-role").value,
            ...readStaffFields(b),
          },
        });
        AdminApp.toast("Đã tạo tài khoản và gửi lời mời tới " + u.email);
        load();
        openDetail(u.id);
      } catch (ex) {
        err.textContent = Object.values(ex.errors || {}).join(" · ") || ex.message;
        err.classList.remove("hidden");
      } finally {
        btn.disabled = false;
      }
    });
  }

  async function openDetail(id) {
    let u;
    try {
      u = await Auth.api("/api/admin/users/" + id);
    } catch (err) {
      return AdminApp.toast(err.message, true);
    }
    const me = Auth.user();
    const isSelf = u.email === me.email;
    const isAdmin = me.role === "ADMIN";
    const canManage = isAdmin || !["ADMIN", "MANAGER"].includes(u.role);
    const isStaff = STAFF_ROLES.includes(u.role);
    const locked = u.status === "SUSPENDED";

    const b = AdminApp.openDrawer(u.fullName, `
      <div class="space-y-6">
        <div class="flex flex-wrap items-center gap-2">${roleBadge(u.role)} ${badge(USER_STATUS[u.status] || u.status, STATUS_BADGE[u.status])}</div>
        <dl class="grid grid-cols-2 gap-4 text-sm">
          <div><dt class="text-xs text-text-muted">Email</dt><dd class="break-all">${esc(u.email)}</dd></div>
          <div><dt class="text-xs text-text-muted">Điện thoại</dt><dd>${esc(u.phone || "—")}</dd></div>
          <div><dt class="text-xs text-text-muted">Ngày tạo</dt><dd>${L.date(u.createdAt)}</dd></div>
          <div><dt class="text-xs text-text-muted">Đăng nhập gần nhất</dt><dd>${L.date(u.lastLoginAt)}</dd></div>
        </dl>
        ${isSelf ? '<p class="text-xs text-amber-600">Đây là tài khoản của bạn — không thể tự khoá hoặc đổi vai trò.</p>' : ""}
        ${!canManage ? '<p class="text-xs text-amber-600">Quản lý không được thay đổi tài khoản quản lý / quản trị viên.</p>' : ""}

        ${!isSelf && canManage ? `<div class="grid grid-cols-2 gap-2">
            <button id="u-lock" class="rounded-xl py-2.5 text-sm font-semibold ${locked ? "bg-emerald-600 text-white" : "bg-rose-600 text-white"}">
              ${locked ? "Mở khoá tài khoản" : "Khoá tài khoản"}</button>
            <button id="u-reset" class="rounded-xl py-2.5 text-sm font-semibold border border-border hover:border-primary">Gửi link đặt lại mật khẩu</button>
          </div>` : ""}

        ${isAdmin && !isSelf ? `<div class="space-y-1.5"><label class="${label}" for="u-role">Vai trò</label>
            <div class="flex gap-2"><select id="u-role" class="${input}">${Object.entries(L.role)
              .map(([k, v]) => `<option value="${k}" ${k === u.role ? "selected" : ""}>${v}</option>`)
              .join("")}</select>
            <button id="u-role-save" class="px-4 rounded-xl bg-dark-navy text-white text-sm font-semibold">Đổi</button></div></div>` : ""}

        ${canManage ? `<form id="u-form" class="grid grid-cols-2 gap-3">
            <div class="col-span-2 space-y-1.5"><label class="${label}" for="u-name">Họ tên</label>
              <input id="u-name" maxlength="150" value="${esc(u.fullName)}" class="${input}"></div>
            <div class="col-span-2 space-y-1.5"><label class="${label}" for="u-phone">Số điện thoại</label>
              <input id="u-phone" maxlength="20" value="${esc(u.phone || "")}" class="${input}"></div>
            ${isStaff ? staffFields(u.staffProfile || {}) : ""}
            <p id="u-error" class="col-span-2 hidden text-sm text-red-600 bg-red-50 rounded-xl p-3"></p>
            <button class="col-span-2 bg-primary text-white rounded-xl py-2.5 font-semibold hover:bg-[#34b6d0]">Lưu thông tin</button>
          </form>` : ""}
      </div>`);

    const fail = (err) => AdminApp.toast(err.message, true);
    const refresh = () => {
      load();
      openDetail(id);
    };

    b.querySelector("#u-lock")?.addEventListener("click", () => {
      if (!locked && !confirm(`Khoá tài khoản ${u.email}? Người này sẽ bị đăng xuất khỏi mọi thiết bị.`)) return;
      Auth.api(`/api/admin/users/${id}/status`, { method: "PATCH", body: { status: locked ? "ACTIVE" : "SUSPENDED" } })
        .then(() => {
          AdminApp.toast(locked ? "Đã mở khoá" : "Đã khoá tài khoản");
          refresh();
        })
        .catch(fail);
    });
    b.querySelector("#u-reset")?.addEventListener("click", () =>
      Auth.api(`/api/admin/users/${id}/reset-password`, { method: "POST" })
        .then((r) => AdminApp.toast(r.message))
        .catch(fail),
    );
    b.querySelector("#u-role-save")?.addEventListener("click", () => {
      const role = b.querySelector("#u-role").value;
      if (role === u.role || !confirm(`Đổi vai trò ${u.email} thành ${L.role[role]}?`)) return;
      Auth.api(`/api/admin/users/${id}/role`, { method: "PATCH", body: { role } })
        .then(() => {
          AdminApp.toast("Đã đổi vai trò");
          refresh();
        })
        .catch(fail);
    });
    b.querySelector("#u-form")?.addEventListener("submit", async (e) => {
      e.preventDefault();
      const body = { fullName: b.querySelector("#u-name").value.trim(), phone: b.querySelector("#u-phone").value.trim() };
      if (isStaff) Object.assign(body, readStaffFields(b));
      try {
        await Auth.api("/api/admin/users/" + id, { method: "PATCH", body });
        AdminApp.toast("Đã lưu");
        refresh();
      } catch (err) {
        const p = b.querySelector("#u-error");
        p.textContent = Object.values(err.errors || {}).join(" · ") || err.message;
        p.classList.remove("hidden");
      }
    });
  }

  AdminApp.register({
    id: "users",
    label: "Người dùng",
    icon: "group",
    roles: ["MANAGER", "ADMIN"],
    init() {
      const roleSel = document.getElementById("usr-role");
      Object.entries(L.role).forEach(([k, v]) => roleSel.add(new Option(v, k)));
      const statusSel = document.getElementById("usr-status");
      Object.entries(USER_STATUS).forEach(([k, v]) => statusSel.add(new Option(v, k)));
      pager = AdminApp.pager("usr-prev", "usr-next", "usr-page", load);
      const reload = () => {
        pager.reset();
        load();
      };
      roleSel.addEventListener("change", (e) => {
        state.role = e.target.value;
        reload();
      });
      statusSel.addEventListener("change", (e) => {
        state.status = e.target.value;
        reload();
      });
      document.getElementById("usr-q").addEventListener(
        "input",
        AdminApp.debounce((e) => {
          state.q = e.target.value.trim();
          reload();
        }),
      );
      document.getElementById("usr-new").addEventListener("click", openCreate);
    },
    show: load,
  });
})();
