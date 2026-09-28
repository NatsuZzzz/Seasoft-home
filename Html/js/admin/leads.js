/* SeaSoft admin - tab "Yeu cau tu van" (lead) */
(function () {
  "use strict";

  const L = window.Labels;
  const esc = L.esc;
  let assignees = null; // cache danh sach nhan vien de phan cong
  let pager;
  const state = { status: "", q: "", scope: "all" };

  function query() {
    const p = new URLSearchParams({ page: pager.page, size: 15 });
    if (state.status) p.set("status", state.status);
    if (state.q) p.set("q", state.q);
    if (state.scope === "mine") p.set("mine", "true");
    if (state.scope === "unassigned") p.set("unassigned", "true");
    return p.toString();
  }

  async function load() {
    const tbody = document.getElementById("lead-rows");
    try {
      const data = await Auth.api("/api/staff/consultations?" + query());
      document.getElementById("lead-total").textContent = data.totalItems;
      document.getElementById("lead-empty").classList.toggle("hidden", data.items.length > 0);
      tbody.innerHTML = data.items.map(row).join("");
      tbody.querySelectorAll("[data-id]").forEach((tr) =>
        tr.addEventListener("click", () => openDetail(tr.dataset.id)),
      );
      pager.update(data);
    } catch (err) {
      AdminApp.toast(err.message, true);
    }
  }

  function row(c, i) {
    return `<tr class="row-link row-in" style="animation-delay:${i * 30}ms" data-id="${c.id}" tabindex="0">
      <td class="px-4 py-3">
        <p class="font-semibold">${esc(c.fullName)}</p>
        <p class="text-xs text-text-muted">${esc(c.companyName || "")}</p>
      </td>
      <td class="px-4 py-3 hidden md:table-cell text-xs">
        <p>${esc(c.email)}</p><p class="text-text-muted">${esc(c.phone)}</p>
      </td>
      <td class="px-4 py-3 hidden sm:table-cell">${esc(L.service[c.serviceType])}</td>
      <td class="px-4 py-3">${L.statusBadge(c.status)}</td>
      <td class="px-4 py-3 hidden md:table-cell">${
        c.assignedStaff ? esc(c.assignedStaff.fullName) : '<span class="text-text-muted italic">Chưa có</span>'
      }</td>
      <td class="px-4 py-3 hidden lg:table-cell text-xs text-text-muted">${L.date(c.createdAt)}</td>
    </tr>`;
  }

  async function openDetail(id) {
    let c;
    try {
      c = await Auth.api("/api/staff/consultations/" + id);
    } catch (err) {
      return AdminApp.toast(err.message, true);
    }
    const me = Auth.user();
    const isManager = Auth.isManager();
    if (isManager && !assignees) {
      assignees = await Auth.api("/api/staff/assignees").catch(() => []);
    }
    const mineOrManager = isManager || c.assignedStaff?.email === me.email;
    const statusOptions = [c.status, ...L.leadNext[c.status]]
      .map((s) => `<option value="${s}" ${s === c.status ? "selected" : ""}>${L.leadStatus[s]}</option>`)
      .join("");

    let assignHtml;
    if (isManager) {
      assignHtml = `<select id="d-assign" class="w-full border border-border rounded-xl py-2.5 px-3 text-sm">
          <option value="">— Chưa phân công —</option>
          ${assignees
            .map(
              (s) =>
                `<option value="${s.id}" ${c.assignedStaff?.id === s.id ? "selected" : ""}>${esc(s.fullName)} · ${esc(
                  L.role[s.role],
                )}</option>`,
            )
            .join("")}
        </select>`;
    } else if (!c.assignedStaff) {
      assignHtml = `<button id="d-claim" class="w-full bg-dark-navy text-white rounded-xl py-2.5 text-sm font-semibold hover:bg-primary">Nhận lead này</button>`;
    } else {
      assignHtml = `<p class="text-sm font-semibold">${esc(c.assignedStaff.fullName)}</p>`;
    }

    const body = AdminApp.openDrawer(c.fullName, `
      <div class="space-y-6">
        <div class="flex items-center gap-3">${L.statusBadge(c.status)}
          <span class="text-xs text-text-muted">Gửi lúc ${L.date(c.createdAt)}</span></div>
        <dl class="grid grid-cols-2 gap-4 text-sm">
          <div><dt class="text-xs text-text-muted">Email</dt><dd><a class="text-primary break-all" href="mailto:${esc(c.email)}">${esc(c.email)}</a></dd></div>
          <div><dt class="text-xs text-text-muted">Điện thoại</dt><dd><a class="text-primary" href="tel:${esc(c.phone)}">${esc(c.phone)}</a></dd></div>
          <div><dt class="text-xs text-text-muted">Công ty</dt><dd>${esc(c.companyName || "—")}</dd></div>
          <div><dt class="text-xs text-text-muted">Tài khoản</dt><dd>${c.hasAccount ? "Đã đăng ký" : "Khách vãng lai"}</dd></div>
          <div><dt class="text-xs text-text-muted">Dịch vụ</dt><dd>${esc(L.service[c.serviceType])}</dd></div>
          <div><dt class="text-xs text-text-muted">Ngân sách</dt><dd>${esc(L.budget[c.budgetRange])}</dd></div>
        </dl>
        <div>
          <p class="text-xs text-text-muted mb-1">Nhu cầu của khách</p>
          <p class="text-sm bg-surface rounded-xl p-4 whitespace-pre-wrap">${esc(c.message || "(không có mô tả)")}</p>
        </div>
        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-text-muted">Phụ trách</label>${assignHtml}
        </div>
        <div class="space-y-1.5">
          <label for="d-status" class="text-xs font-semibold text-text-muted">Trạng thái</label>
          <select id="d-status" class="w-full border border-border rounded-xl py-2.5 px-3 text-sm" ${mineOrManager ? "" : "disabled"}>${statusOptions}</select>
        </div>
        <div class="space-y-1.5">
          <label for="d-note" class="text-xs font-semibold text-text-muted">Ghi chú nội bộ</label>
          <textarea id="d-note" rows="4" maxlength="5000" class="w-full border border-border rounded-xl py-2.5 px-3 text-sm" ${mineOrManager ? "" : "disabled"}>${esc(c.internalNote || "")}</textarea>
        </div>
        ${mineOrManager ? "" : '<p class="text-xs text-amber-600">Bạn cần nhận lead này trước khi cập nhật.</p>'}
        <p id="d-error" class="hidden text-sm text-red-600 bg-red-50 rounded-xl p-3"></p>
        <button id="d-save" class="w-full bg-primary text-white rounded-xl py-3 font-semibold hover:bg-[#34b6d0] disabled:opacity-50" ${mineOrManager ? "" : "disabled"}>Lưu thay đổi</button>
      </div>`);

    const showError = (msg) => {
      const p = body.querySelector("#d-error");
      p.textContent = msg;
      p.classList.remove("hidden");
    };

    body.querySelector("#d-claim")?.addEventListener("click", async () => {
      try {
        const myId = (await Auth.api("/api/users/me")).id;
        await Auth.api(`/api/staff/consultations/${c.id}/assign`, { method: "PATCH", body: { staffId: myId } });
        AdminApp.toast("Đã nhận lead");
        load();
        openDetail(c.id);
      } catch (err) {
        showError(err.message);
      }
    });

    body.querySelector("#d-save").addEventListener("click", async (ev) => {
      ev.target.disabled = true;
      try {
        const assignSel = body.querySelector("#d-assign");
        if (assignSel && assignSel.value !== (c.assignedStaff?.id || "")) {
          await Auth.api(`/api/staff/consultations/${c.id}/assign`, {
            method: "PATCH",
            body: { staffId: assignSel.value || null },
          });
        }
        const status = body.querySelector("#d-status").value;
        const note = body.querySelector("#d-note").value;
        const patch = {};
        if (status !== c.status) patch.status = status;
        if (note !== (c.internalNote || "")) patch.internalNote = note;
        if (Object.keys(patch).length) {
          await Auth.api("/api/staff/consultations/" + c.id, { method: "PATCH", body: patch });
        }
        AdminApp.toast("Đã lưu thay đổi");
        AdminApp.closeDrawer();
        load();
      } catch (err) {
        showError(err.message);
      } finally {
        ev.target.disabled = false;
      }
    });
  }

  AdminApp.register({
    id: "leads",
    label: "Yêu cầu tư vấn",
    icon: "support_agent",
    init() {
      const statusSel = document.getElementById("lead-status");
      Object.entries(L.leadStatus).forEach(([k, v]) => statusSel.add(new Option(v, k)));
      pager = AdminApp.pager("lead-prev", "lead-next", "lead-page", load);

      const reload = () => {
        pager.reset();
        load();
      };
      statusSel.addEventListener("change", (e) => {
        state.status = e.target.value;
        reload();
      });
      document.getElementById("lead-scope").addEventListener("change", (e) => {
        state.scope = e.target.value;
        reload();
      });
      document.getElementById("lead-q").addEventListener(
        "input",
        AdminApp.debounce((e) => {
          state.q = e.target.value.trim();
          reload();
        }),
      );
      document.getElementById("lead-refresh").addEventListener("click", load);
      document.getElementById("lead-rows").addEventListener("keydown", (e) => {
        if (e.key === "Enter" && e.target.dataset.id) openDetail(e.target.dataset.id);
      });
    },
    show: load,
  });
})();
