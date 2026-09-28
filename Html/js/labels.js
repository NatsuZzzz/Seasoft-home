/* SeaSoft - nhan tieng Viet cho cac enum tu API (dung chung moi trang) */
(function () {
  "use strict";

  const Labels = {
    service: {
      CORPORATE_WEBSITE: "Website doanh nghiệp",
      LANDING_PAGE: "Landing Page",
      ECOMMERCE: "Website bán hàng",
      CUSTOM: "Website theo yêu cầu",
      MAINTENANCE: "Bảo trì & nâng cấp",
      OTHER: "Khác",
    },
    budget: {
      UNDER_20M: "Dưới 20 triệu",
      FROM_20M_TO_50M: "20 – 50 triệu",
      FROM_50M_TO_100M: "50 – 100 triệu",
      OVER_100M: "Trên 100 triệu",
      UNDECIDED: "Chưa xác định",
    },
    leadStatus: {
      NEW: "Mới",
      CONTACTED: "Đã liên hệ",
      QUOTED: "Đã báo giá",
      WON: "Thành công",
      LOST: "Thất bại",
    },
    // Luong chuyen trang thai hop le (khop ConsultationEnums.Status o backend)
    leadNext: {
      NEW: ["CONTACTED", "LOST"],
      CONTACTED: ["QUOTED", "LOST"],
      QUOTED: ["WON", "LOST", "CONTACTED"],
      WON: [],
      LOST: ["CONTACTED"],
    },
    projectStatus: {
      PLANNING: "Lên kế hoạch",
      IN_PROGRESS: "Đang thực hiện",
      ON_HOLD: "Tạm dừng",
      COMPLETED: "Hoàn thành",
      CANCELLED: "Đã huỷ",
    },
    milestoneStatus: {
      TODO: "Chưa bắt đầu",
      IN_PROGRESS: "Đang làm",
      DONE: "Hoàn thành",
    },
    role: {
      CUSTOMER: "Khách hàng",
      STAFF: "Nhân viên",
      MANAGER: "Quản lý",
      ADMIN: "Quản trị viên",
    },
    // Class Tailwind cho badge trang thai
    badge: {
      NEW: "bg-sky-50 text-sky-700 ring-sky-200",
      CONTACTED: "bg-amber-50 text-amber-700 ring-amber-200",
      QUOTED: "bg-violet-50 text-violet-700 ring-violet-200",
      WON: "bg-emerald-50 text-emerald-700 ring-emerald-200",
      LOST: "bg-rose-50 text-rose-700 ring-rose-200",
      PLANNING: "bg-sky-50 text-sky-700 ring-sky-200",
      IN_PROGRESS: "bg-amber-50 text-amber-700 ring-amber-200",
      ON_HOLD: "bg-gray-100 text-gray-600 ring-gray-200",
      COMPLETED: "bg-emerald-50 text-emerald-700 ring-emerald-200",
      CANCELLED: "bg-rose-50 text-rose-700 ring-rose-200",
      TODO: "bg-gray-50 text-gray-600 ring-gray-200",
      DONE: "bg-emerald-50 text-emerald-700 ring-emerald-200",
    },
  };

  Labels.statusBadge = (status, map = Labels.leadStatus) =>
    `<span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ring-1 ${
      Labels.badge[status] || "bg-gray-50 text-gray-600 ring-gray-200"
    }">${map[status] || status}</span>`;

  // Chen text vao HTML an toan (chong XSS tu du lieu nguoi dung nhap)
  Labels.esc = (s) =>
    String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);

  Labels.date = (iso, withTime = true) =>
    iso
      ? new Date(iso).toLocaleString("vi-VN", withTime ? {} : { day: "2-digit", month: "2-digit", year: "numeric" })
      : "—";

  window.Labels = Labels;
})();
