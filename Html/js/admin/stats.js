/* SeaSoft admin - tab "Tong quan": the so lieu + bieu do 1 chuoi (HTML/CSS, khong thu vien) */
(function () {
  "use strict";

  const L = window.Labels;
  const esc = L.esc;
  const fmt = (n) => Number(n).toLocaleString("vi-VN");

  // Tooltip dung chung: gan vao phan tu co data-tip
  function bindTips(root) {
    const tip = document.getElementById("chart-tip");
    const show = (el) => {
      const r = el.getBoundingClientRect();
      tip.textContent = el.dataset.tip;
      tip.style.left = r.left + r.width / 2 + "px";
      tip.style.top = r.top + "px";
      tip.classList.add("is-visible");
    };
    const hide = () => tip.classList.remove("is-visible");
    root.querySelectorAll("[data-tip]").forEach((el) => {
      el.addEventListener("pointerenter", () => show(el));
      el.addEventListener("pointerleave", hide);
      el.addEventListener("focus", () => show(el));
      el.addEventListener("blur", hide);
    });
  }

  // Bang du lieu thay the (doc man hinh / xem so chinh xac)
  function table(rows, head) {
    return `<details class="mt-4 text-xs"><summary class="cursor-pointer text-text-muted hover:text-primary">Xem dạng bảng</summary>
      <table class="mt-2 w-full"><thead><tr class="text-left text-text-muted"><th class="py-1">${head[0]}</th><th class="py-1 text-right">${head[1]}</th></tr></thead>
      <tbody>${rows.map(([k, v]) => `<tr class="border-t border-border"><td class="py-1">${esc(k)}</td><td class="py-1 text-right tabular-nums">${fmt(v)}</td></tr>`).join("")}</tbody></table></details>`;
  }

  // Cot doc theo tuan: chi ghi so o tuan cao nhat va tuan hien tai (ghi nhan chon loc)
  function columnChart(el, weeks) {
    const max = Math.max(1, ...weeks.map((w) => w.count));
    const peak = weeks.findIndex((w) => w.count === max);
    const last = weeks.length - 1;
    const label = (w) => new Date(w.weekStart).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" });
    el.innerHTML = `
      <div class="col-chart" role="img" aria-label="Biểu đồ cột số lead mới theo tuần">
        ${weeks
          .map(
            (w, i) => `<div class="col-slot" tabindex="0" data-tip="Tuần ${label(w)}: ${fmt(w.count)} lead">
              ${i === peak || i === last ? `<span class="col-value">${fmt(w.count)}</span>` : ""}
              <div class="col-bar" style="height:${w.count ? Math.max(2, (w.count / max) * 170) : 0}px;animation-delay:${i * 50}ms"></div>
            </div>`,
          )
          .join("")}
      </div>
      <div class="col-labels">${weeks.map((w) => `<span>${label(w)}</span>`).join("")}</div>
      ${table(weeks.map((w) => ["Tuần " + label(w), w.count]), ["Tuần", "Lead"])}`;
    bindTips(el);
  }

  // Thanh ngang: moi danh muc 1 thanh, gia tri o dau thanh
  function barChart(el, map, labels, unit) {
    const entries = Object.entries(map);
    const max = Math.max(1, ...entries.map(([, v]) => v));
    el.innerHTML = `<div role="img" aria-label="Biểu đồ thanh">
      ${entries
        .map(
          ([k, v], i) => `<div class="hbar-row" tabindex="0" data-tip="${esc(labels[k] || k)}: ${fmt(v)} ${unit}">
            <span class="hbar-label">${esc(labels[k] || k)}</span>
            <span class="hbar-track"><span class="hbar" style="width:${v ? Math.max(1, (v / max) * 88) : 0}%;animation-delay:${i * 60}ms"></span>
              <span class="hbar-value">${fmt(v)}</span></span>
          </div>`,
        )
        .join("")}</div>
      ${table(entries.map(([k, v]) => [labels[k] || k, v]), ["Nhóm", unit[0].toUpperCase() + unit.slice(1)])}`;
    bindTips(el);
  }

  function tile(label, value, note) {
    return `<div class="stat-tile row-in">
      <p class="text-xs font-semibold text-text-muted">${label}</p>
      <p class="text-3xl font-semibold mt-2">${value}</p>
      ${note ? `<p class="text-xs text-text-muted mt-1">${note}</p>` : ""}</div>`;
  }

  async function load() {
    let s;
    try {
      s = await Auth.api("/api/admin/stats");
    } catch (err) {
      return AdminApp.toast(err.message, true);
    }
    document.getElementById("st-time").textContent = new Date().toLocaleString("vi-VN");
    const running = s.projectsByStatus.PLANNING + s.projectsByStatus.IN_PROGRESS + s.projectsByStatus.ON_HOLD;
    document.getElementById("st-tiles").innerHTML = [
      tile("Lead 30 ngày qua", fmt(s.leadsLast30Days), `Tổng ${fmt(s.leadsTotal)} lead`),
      tile("Tỉ lệ chốt", s.conversionRate == null ? "—" : s.conversionRate.toLocaleString("vi-VN") + "%", "Thành công / (Thành công + Thất bại)"),
      tile("Dự án đang chạy", fmt(running), `Tổng ${fmt(s.projectsTotal)} dự án`),
      tile("Tiến độ trung bình", s.avgProgress == null ? "—" : s.avgProgress.toLocaleString("vi-VN") + "%", "Của dự án đang chạy"),
      tile("Dự án trễ hạn", fmt(s.projectsOverdue), s.projectsOverdue ? "Cần xử lý" : "Không có"),
    ].join("");

    columnChart(document.getElementById("ch-weeks"), s.leadsPerWeek);
    barChart(document.getElementById("ch-lead-status"), s.leadsByStatus, L.leadStatus, "lead");
    barChart(document.getElementById("ch-lead-service"), s.leadsByService, L.service, "lead");
    barChart(document.getElementById("ch-project-status"), s.projectsByStatus, L.projectStatus, "dự án");
    barChart(document.getElementById("ch-users"), s.usersByRole, L.role, "tài khoản");
  }

  AdminApp.register({
    id: "stats",
    label: "Tổng quan",
    icon: "insights",
    roles: ["MANAGER", "ADMIN"],
    init() {
      document.getElementById("st-refresh").addEventListener("click", load);
    },
    show: load,
  });
})();
