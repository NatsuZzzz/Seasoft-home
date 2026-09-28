/* SeaSoft admin - tab "Du an": danh sach, tao moi, chi tiet (moc tien do, cap nhat) */
(function () {
  "use strict";

  const L = window.Labels;
  const esc = L.esc;
  const state = { status: "", q: "", scope: "all" };
  let pager;
  let assignees = null;

  const input = "w-full border border-border rounded-xl py-2.5 px-3 text-sm focus:border-primary focus:ring-primary/20 focus:ring-4";
  const label = "text-xs font-semibold text-text-muted";

  async function loadAssignees() {
    if (!assignees) assignees = await Auth.api("/api/staff/assignees").catch(() => []);
    return assignees;
  }

  // ---------------- Danh sach ----------------

  async function load() {
    const p = new URLSearchParams({ page: pager.page, size: 15 });
    if (state.status) p.set("status", state.status);
    if (state.q) p.set("q", state.q);
    if (state.scope === "mine") p.set("mine", "true");
    try {
      const data = await Auth.api("/api/staff/projects?" + p);
      document.getElementById("prj-total").textContent = data.totalItems;
      document.getElementById("prj-empty").classList.toggle("hidden", data.items.length > 0);
      const tbody = document.getElementById("prj-rows");
      tbody.innerHTML = data.items
        .map(
          (x, i) => `<tr class="row-link row-in" style="animation-delay:${i * 30}ms" data-id="${x.id}" tabindex="0">
            <td class="px-4 py-3"><p class="font-semibold">${esc(x.name)}</p>
              <p class="text-xs text-text-muted">${esc(x.code)} · ${esc(L.service[x.serviceType])}</p></td>
            <td class="px-4 py-3 hidden md:table-cell"><p>${esc(x.customerName)}</p><p class="text-xs text-text-muted">${esc(x.customerEmail)}</p></td>
            <td class="px-4 py-3">${L.statusBadge(x.status, L.projectStatus)}</td>
            <td class="px-4 py-3"><div class="flex items-center gap-2">${Portal.progressBar(x.progress)}<span class="text-xs font-semibold w-9 text-right">${x.progress}%</span></div></td>
            <td class="px-4 py-3 hidden lg:table-cell">${esc(x.manager?.fullName || "—")}</td>
            <td class="px-4 py-3 hidden lg:table-cell text-xs">${x.dueDate ? L.date(x.dueDate, false) : "—"}</td>
          </tr>`,
        )
        .join("");
      tbody.querySelectorAll(".progress-track").forEach((t) => t.classList.add("flex-1"));
      tbody.querySelectorAll("[data-id]").forEach((tr) => tr.addEventListener("click", () => openDetail(tr.dataset.id)));
      Portal.animateProgress(tbody);
      pager.update(data);
    } catch (err) {
      AdminApp.toast(err.message, true);
    }
  }

  // ---------------- Tao du an ----------------

  async function openCreate(leadId) {
    const isManager = Auth.isManager();
    const leads = await Auth.api("/api/staff/consultations?status=WON&size=100" + (isManager ? "" : "&mine=true"))
      .then((d) => d.items)
      .catch(() => []);
    const serviceOptions = Object.entries(L.service).map(([k, v]) => `<option value="${k}">${esc(v)}</option>`).join("");

    const body = AdminApp.openDrawer("Tạo dự án mới", `
      <form id="c-form" class="space-y-5" novalidate>
        ${isManager ? `<div class="flex gap-2 text-sm">
            <label class="flex-1 flex items-center gap-2 border border-border rounded-xl p-3 cursor-pointer has-[:checked]:border-primary has-[:checked]:bg-surface-alt">
              <input type="radio" name="src" value="lead" checked class="text-primary"> Từ lead đã chốt</label>
            <label class="flex-1 flex items-center gap-2 border border-border rounded-xl p-3 cursor-pointer has-[:checked]:border-primary has-[:checked]:bg-surface-alt">
              <input type="radio" name="src" value="email" class="text-primary"> Khách có tài khoản</label>
          </div>` : ""}
        <div id="c-lead-box" class="space-y-1.5">
          <label for="c-lead" class="${label}">Lead đã chốt (Thành công)</label>
          <select id="c-lead" class="${input}">
            <option value="">— Chọn lead —</option>
            ${leads.map((l) => `<option value="${l.id}" ${l.id === leadId ? "selected" : ""}>${esc(l.fullName)} · ${esc(l.companyName || l.email)} · ${esc(L.service[l.serviceType])}</option>`).join("")}
          </select>
          ${leads.length ? "" : '<p class="text-xs text-amber-600">Chưa có lead nào ở trạng thái "Thành công".</p>'}
        </div>
        <div id="c-email-box" class="hidden grid grid-cols-2 gap-3">
          <div class="space-y-1.5"><label for="c-email" class="${label}">Email khách hàng</label>
            <input id="c-email" type="email" class="${input}" placeholder="khach@congty.vn"></div>
          <div class="space-y-1.5"><label for="c-service" class="${label}">Dịch vụ</label>
            <select id="c-service" class="${input}"><option value="">— Chọn —</option>${serviceOptions}</select></div>
        </div>
        <div class="space-y-1.5"><label for="c-name" class="${label}">Tên dự án *</label>
          <input id="c-name" maxlength="200" class="${input}" placeholder="VD: Website giới thiệu ACME"></div>
        <div class="grid grid-cols-2 gap-3">
          <div class="space-y-1.5"><label for="c-start" class="${label}">Bắt đầu</label><input id="c-start" type="date" class="${input}"></div>
          <div class="space-y-1.5"><label for="c-due" class="${label}">Dự kiến bàn giao</label><input id="c-due" type="date" class="${input}"></div>
        </div>
        <div class="space-y-1.5"><label for="c-desc" class="${label}">Mô tả</label>
          <textarea id="c-desc" rows="3" maxlength="5000" class="${input}"></textarea></div>
        <p class="text-xs text-text-muted">Dự án được tạo sẵn 5 giai đoạn: Tư vấn → UI/UX → Development → Testing → Launch.</p>
        <p id="c-error" class="hidden text-sm text-red-600 bg-red-50 rounded-xl p-3"></p>
        <button class="w-full bg-primary text-white rounded-xl py-3 font-semibold hover:bg-[#34b6d0] disabled:opacity-50">Tạo dự án</button>
      </form>`);

    const $ = (s) => body.querySelector(s);
    const leadSel = $("#c-lead");
    // Chon lead -> goi y ten du an
    const suggestName = () => {
      const l = leads.find((x) => x.id === leadSel.value);
      if (l && !$("#c-name").value) $("#c-name").value = `${L.service[l.serviceType]} ${l.companyName || l.fullName}`;
    };
    leadSel.addEventListener("change", suggestName);
    suggestName();
    body.querySelectorAll('input[name="src"]').forEach((r) =>
      r.addEventListener("change", () => {
        const byLead = body.querySelector('input[name="src"]:checked').value === "lead";
        $("#c-lead-box").classList.toggle("hidden", !byLead);
        $("#c-email-box").classList.toggle("hidden", byLead);
      }),
    );

    $("#c-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const byLead = (body.querySelector('input[name="src"]:checked')?.value || "lead") === "lead";
      const payload = {
        name: $("#c-name").value.trim(),
        startDate: $("#c-start").value || null,
        dueDate: $("#c-due").value || null,
        description: $("#c-desc").value.trim(),
      };
      if (byLead) payload.consultationId = leadSel.value || null;
      else Object.assign(payload, { customerEmail: $("#c-email").value.trim(), serviceType: $("#c-service").value || null });

      const err = $("#c-error");
      err.classList.add("hidden");
      if (byLead && !payload.consultationId) {
        err.textContent = "Vui lòng chọn lead";
        return err.classList.remove("hidden");
      }
      const btn = e.target.querySelector("button");
      btn.disabled = true;
      try {
        const d = await Auth.api("/api/staff/projects", { method: "POST", body: payload });
        AdminApp.toast("Đã tạo dự án " + d.summary.code);
        load();
        openDetail(d.summary.id);
      } catch (ex) {
        err.textContent = Object.values(ex.errors || {})[0] || ex.message;
        err.classList.remove("hidden");
      } finally {
        btn.disabled = false;
      }
    });
  }

  // ---------------- Chi tiet ----------------

  async function openDetail(id) {
    let d;
    try {
      d = await Auth.api("/api/staff/projects/" + id);
    } catch (err) {
      return AdminApp.toast(err.message, true);
    }
    const s = d.summary;
    const isManager = Auth.isManager();
    const canEdit = isManager || s.manager?.email === Auth.user().email;
    const dis = canEdit ? "" : "disabled";
    const staff = isManager ? await loadAssignees() : [];

    const statusOpts = Object.entries(L.projectStatus)
      .map(([k, v]) => `<option value="${k}" ${k === s.status ? "selected" : ""}>${v}</option>`)
      .join("");
    const msOpts = (cur) =>
      Object.entries(L.milestoneStatus).map(([k, v]) => `<option value="${k}" ${k === cur ? "selected" : ""}>${v}</option>`).join("");

    const body = AdminApp.openDrawer(`${s.code} · ${s.name}`, `
      <div class="space-y-8">
        <div class="space-y-2">
          <p class="text-sm">Khách hàng: <b>${esc(s.customerName)}</b> <span class="text-text-muted">(${esc(s.customerEmail)})</span></p>
          <div class="flex items-center gap-3">${Portal.progressBar(s.progress)}<b class="text-sm">${s.progress}%</b></div>
          ${canEdit ? "" : '<p class="text-xs text-amber-600">Bạn chỉ xem được — dự án do người khác phụ trách.</p>'}
        </div>

        <form id="p-form" class="grid grid-cols-2 gap-3">
          <div class="col-span-2 space-y-1.5"><label class="${label}" for="p-name">Tên dự án</label>
            <input id="p-name" maxlength="200" value="${esc(s.name)}" class="${input}" ${dis}></div>
          <div class="space-y-1.5"><label class="${label}" for="p-status">Trạng thái</label>
            <select id="p-status" class="${input}" ${dis}>${statusOpts}</select></div>
          <div class="space-y-1.5"><label class="${label}" for="p-manager">Phụ trách</label>
            ${isManager
              ? `<select id="p-manager" class="${input}">${staff
                  .map((u) => `<option value="${u.id}" ${u.id === s.manager?.id ? "selected" : ""}>${esc(u.fullName)}</option>`)
                  .join("")}</select>`
              : `<input class="${input}" value="${esc(s.manager?.fullName || "—")}" disabled>`}</div>
          <div class="space-y-1.5"><label class="${label}" for="p-start">Bắt đầu</label>
            <input id="p-start" type="date" value="${s.startDate || ""}" class="${input}" ${dis}></div>
          <div class="space-y-1.5"><label class="${label}" for="p-due">Dự kiến bàn giao</label>
            <input id="p-due" type="date" value="${s.dueDate || ""}" class="${input}" ${dis}></div>
          <div class="col-span-2 space-y-1.5"><label class="${label}" for="p-desc">Mô tả</label>
            <textarea id="p-desc" rows="3" maxlength="5000" class="${input}" ${dis}>${esc(d.description || "")}</textarea></div>
          <p id="p-error" class="col-span-2 hidden text-sm text-red-600 bg-red-50 rounded-xl p-3"></p>
          ${canEdit ? '<button class="col-span-2 bg-primary text-white rounded-xl py-2.5 font-semibold hover:bg-[#34b6d0]">Lưu thông tin</button>' : ""}
        </form>

        <div class="space-y-3">
          <h3 class="font-bold">Giai đoạn</h3>
          <ul class="space-y-2">${d.milestones
            .map(
              (m) => `<li class="flex items-center gap-2 border border-border rounded-xl p-3">
                <span class="flex-1 text-sm font-medium">${esc(m.title)}</span>
                <select data-ms="${m.id}" class="border border-border rounded-lg py-1.5 px-2 text-xs" ${dis}>${msOpts(m.status)}</select>
                ${canEdit ? `<button data-del="${m.id}" class="p-1.5 rounded-lg text-text-muted hover:text-red-600 hover:bg-red-50" title="Xoá giai đoạn">
                  <span class="material-symbols-outlined text-lg">delete</span></button>` : ""}
              </li>`,
            )
            .join("")}</ul>
          ${canEdit ? `<form id="ms-form" class="flex gap-2">
              <input id="ms-title" maxlength="200" placeholder="Thêm giai đoạn…" class="${input}">
              <input id="ms-due" type="date" class="border border-border rounded-xl px-2 text-sm" aria-label="Hạn">
              <button class="px-4 rounded-xl bg-dark-navy text-white text-sm font-semibold">Thêm</button></form>` : ""}
        </div>

        <div class="space-y-3">
          <h3 class="font-bold">Cập nhật & trao đổi</h3>
          ${canEdit ? `<form id="up-form" class="space-y-2">
              <textarea id="up-content" rows="3" maxlength="5000" placeholder="Viết cập nhật tiến độ…" class="${input}"></textarea>
              <div class="flex items-center justify-between">
                <label class="flex items-center gap-2 text-sm"><input id="up-visible" type="checkbox" checked class="rounded text-primary"> Khách hàng xem được (gửi email)</label>
                <button class="px-4 py-2 rounded-xl bg-primary text-white text-sm font-semibold">Đăng</button>
              </div></form>` : ""}
          <ul class="space-y-3">${d.updates
            .map(
              (u) => `<li class="rounded-xl p-3 text-sm ${u.authorRole === "CUSTOMER" ? "bg-surface-alt" : "border border-border"}">
                <p class="text-xs text-text-muted mb-1"><b class="text-dark-navy">${esc(u.authorName)}</b>
                  ${u.authorRole === "CUSTOMER" ? '· <span class="text-primary font-semibold">Khách hàng</span>' : ""}
                  ${u.visibleToCustomer ? "" : '· <span class="text-amber-600 font-semibold">Nội bộ</span>'} · ${L.date(u.createdAt)}</p>
                <p class="whitespace-pre-wrap">${esc(u.content)}</p></li>`,
            )
            .join("") || '<li class="text-sm text-text-muted">Chưa có cập nhật.</li>'}</ul>
        </div>
      </div>`);

    Portal.animateProgress(body);
    const $ = (sel) => body.querySelector(sel);
    const refresh = () => {
      load();
      openDetail(id);
    };
    const fail = (err) => AdminApp.toast(err.message, true);

    $("#p-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const payload = {
        name: $("#p-name").value.trim(),
        status: $("#p-status").value,
        startDate: $("#p-start").value || null,
        dueDate: $("#p-due").value || null,
        description: $("#p-desc").value,
      };
      const mgr = $("#p-manager");
      if (mgr && mgr.value && mgr.value !== s.manager?.id) payload.managerId = mgr.value;
      try {
        await Auth.api("/api/staff/projects/" + id, { method: "PATCH", body: payload });
        AdminApp.toast("Đã lưu dự án");
        refresh();
      } catch (err) {
        $("#p-error").textContent = err.message;
        $("#p-error").classList.remove("hidden");
      }
    });

    body.querySelectorAll("[data-ms]").forEach((sel) =>
      sel.addEventListener("change", () =>
        Auth.api(`/api/staff/projects/${id}/milestones/${sel.dataset.ms}`, { method: "PATCH", body: { status: sel.value } })
          .then(() => {
            AdminApp.toast("Đã cập nhật giai đoạn");
            refresh();
          })
          .catch(fail),
      ),
    );
    body.querySelectorAll("[data-del]").forEach((btn) =>
      btn.addEventListener("click", () => {
        if (!confirm("Xoá giai đoạn này?")) return;
        Auth.api(`/api/staff/projects/${id}/milestones/${btn.dataset.del}`, { method: "DELETE" }).then(refresh).catch(fail);
      }),
    );
    $("#ms-form")?.addEventListener("submit", (e) => {
      e.preventDefault();
      const title = $("#ms-title").value.trim();
      if (!title) return;
      Auth.api(`/api/staff/projects/${id}/milestones`, {
        method: "POST",
        body: { title, dueDate: $("#ms-due").value || null },
      })
        .then(refresh)
        .catch(fail);
    });
    $("#up-form")?.addEventListener("submit", (e) => {
      e.preventDefault();
      const content = $("#up-content").value.trim();
      if (!content) return;
      Auth.api(`/api/staff/projects/${id}/updates`, {
        method: "POST",
        body: { content, visibleToCustomer: $("#up-visible").checked },
      })
        .then(() => {
          AdminApp.toast("Đã đăng cập nhật");
          refresh();
        })
        .catch(fail);
    });
  }

  AdminApp.register({
    id: "projects",
    label: "Dự án",
    icon: "folder_open",
    init() {
      const statusSel = document.getElementById("prj-status");
      Object.entries(L.projectStatus).forEach(([k, v]) => statusSel.add(new Option(v, k)));
      pager = AdminApp.pager("prj-prev", "prj-next", "prj-page", load);
      const reload = () => {
        pager.reset();
        load();
      };
      statusSel.addEventListener("change", (e) => {
        state.status = e.target.value;
        reload();
      });
      document.getElementById("prj-scope").addEventListener("change", (e) => {
        state.scope = e.target.value;
        reload();
      });
      document.getElementById("prj-q").addEventListener(
        "input",
        AdminApp.debounce((e) => {
          state.q = e.target.value.trim();
          reload();
        }),
      );
      document.getElementById("prj-new").addEventListener("click", () => openCreate());
    },
    show: load,
  });

  // Tab Leads goi de tao du an tu lead dang xem
  window.AdminProjects = { openCreate };
})();
