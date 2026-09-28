/* SeaSoft admin - tab "Noi dung": du an tieu bieu, blog, danh gia khach hang */
(function () {
  "use strict";

  const esc = window.Labels.esc;
  const input = "w-full border border-border rounded-xl py-2.5 px-3 text-sm focus:border-primary focus:ring-primary/20 focus:ring-4";
  const label = "text-xs font-semibold text-text-muted";
  const pill = (on, yes, no) =>
    `<span class="inline-flex px-2.5 py-1 rounded-full text-xs font-semibold ring-1 ${
      on ? "bg-emerald-50 text-emerald-700 ring-emerald-200" : "bg-gray-50 text-gray-600 ring-gray-200"
    }">${on ? yes : no}</span>`;

  const field = (id, text, value = "", attrs = "") =>
    `<div class="space-y-1.5"><label class="${label}" for="${id}">${text}</label>
      <input id="${id}" value="${esc(value ?? "")}" class="${input}" ${attrs}></div>`;
  const area = (id, text, value = "", rows = 4, attrs = "") =>
    `<div class="space-y-1.5"><label class="${label}" for="${id}">${text}</label>
      <textarea id="${id}" rows="${rows}" class="${input}" ${attrs}>${esc(value ?? "")}</textarea></div>`;
  const check = (id, text, on) =>
    `<label class="flex items-center gap-2 text-sm"><input id="${id}" type="checkbox" ${on ? "checked" : ""} class="rounded text-primary"> ${text}</label>`;

  // Moi loai noi dung: cach tai danh sach, ve 1 dong, form va doc form
  const KINDS = {
    portfolio: {
      label: "Dự án tiêu biểu",
      newLabel: "Thêm dự án",
      api: "/api/admin/content/portfolio",
      list: (d) => d,
      row: (x) => `<p class="font-semibold">${esc(x.title)}</p>
        <p class="text-xs text-text-muted">${esc([x.category, x.clientName].filter(Boolean).join(" · "))} · thứ tự ${x.sortOrder}</p>`,
      badge: (x) => pill(x.published, "Hiển thị", "Ẩn"),
      form: (x = {}) => `
        ${field("c-title", "Tiêu đề *", x.title, 'maxlength="200"')}
        <div class="grid grid-cols-2 gap-3">${field("c-category", "Danh mục", x.category, 'maxlength="100"')}
          ${field("c-client", "Khách hàng", x.clientName, 'maxlength="150"')}</div>
        ${area("c-summary", "Tóm tắt (hiện trên thẻ)", x.summary, 3, 'maxlength="500"')}
        ${field("c-cover", "Link ảnh bìa (https://…)", x.coverImageUrl, 'type="url" maxlength="1000"')}
        ${field("c-url", "Link website dự án (https://…)", x.projectUrl, 'type="url" maxlength="500"')}
        <div class="grid grid-cols-2 gap-3">${field("c-slug", "Slug (bỏ trống = tự tạo)", x.slug, 'maxlength="200"')}
          ${field("c-order", "Thứ tự", x.sortOrder ?? 0, 'type="number"')}</div>
        ${check("c-published", "Hiển thị trên trang chủ", x.published)}`,
      read: ($) => ({
        title: $("#c-title").value.trim(),
        category: $("#c-category").value.trim(),
        clientName: $("#c-client").value.trim(),
        summary: $("#c-summary").value.trim(),
        coverImageUrl: $("#c-cover").value.trim(),
        projectUrl: $("#c-url").value.trim(),
        slug: $("#c-slug").value.trim(),
        sortOrder: Number($("#c-order").value || 0),
        published: $("#c-published").checked,
      }),
      title: (x) => x.title,
    },
    blog: {
      label: "Blog",
      newLabel: "Viết bài",
      api: "/api/admin/content/blog",
      list: (d) => d.items,
      query: "?size=100",
      row: (x) => `<p class="font-semibold">${esc(x.title)}</p>
        <p class="text-xs text-text-muted">${esc(x.authorName)} · ${x.publishedAt ? "đăng " + window.Labels.date(x.publishedAt, false) : "chưa đăng"}</p>`,
      badge: (x) => pill(x.status === "PUBLISHED", "Đã đăng", "Bản nháp"),
      detail: (id) => Auth.api("/api/admin/content/blog/" + id).then((d) => ({ ...d.summary, content: d.content })),
      form: (x = {}) => `
        ${field("c-title", "Tiêu đề *", x.title, 'maxlength="200"')}
        ${area("c-excerpt", "Mô tả ngắn (SEO & thẻ bài viết)", x.excerpt, 2, 'maxlength="500"')}
        ${area("c-content", "Nội dung *", x.content, 14, 'maxlength="50000"')}
        <p class="text-xs text-text-muted -mt-1">Dòng trống = đoạn mới · "## " = tiêu đề mục · "### " = tiêu đề nhỏ · "- " = gạch đầu dòng</p>
        ${field("c-cover", "Link ảnh bìa (https://…)", x.coverImageUrl, 'type="url" maxlength="1000"')}
        <div class="grid grid-cols-2 gap-3">${field("c-tags", "Thẻ (cách nhau dấu phẩy)", x.tags, 'maxlength="200"')}
          ${field("c-slug", "Slug (bỏ trống = tự tạo)", x.slug, 'maxlength="200"')}</div>
        ${check("c-published", "Đăng công khai", x.status === "PUBLISHED")}
        ${x.slug && x.status === "PUBLISHED" ? `<a href="post.html?slug=${encodeURIComponent(x.slug)}" target="_blank" class="text-sm font-semibold text-primary">Xem bài trên website ↗</a>` : ""}`,
      read: ($) => ({
        title: $("#c-title").value.trim(),
        excerpt: $("#c-excerpt").value.trim(),
        content: $("#c-content").value,
        coverImageUrl: $("#c-cover").value.trim(),
        tags: $("#c-tags").value.trim(),
        slug: $("#c-slug").value.trim(),
        status: $("#c-published").checked ? "PUBLISHED" : "DRAFT",
      }),
      title: (x) => x.title,
    },
    testimonials: {
      label: "Đánh giá",
      newLabel: "Thêm đánh giá",
      api: "/api/admin/content/testimonials",
      list: (d) => d,
      row: (x) => `<p class="font-semibold">${esc(x.customerName)} <span class="text-amber-500">${"★".repeat(x.rating)}</span></p>
        <p class="text-xs text-text-muted line-clamp-1">“${esc(x.quote)}”</p>`,
      badge: (x) => pill(x.published, "Hiển thị", "Ẩn"),
      form: (x = {}) => `
        <p class="text-xs bg-amber-50 text-amber-700 rounded-xl p-3">Chỉ đăng đánh giá thật, đã được khách hàng đồng ý công khai.</p>
        <div class="grid grid-cols-2 gap-3">${field("c-name", "Tên khách hàng *", x.customerName, 'maxlength="150"')}
          ${field("c-position", "Chức vụ", x.position, 'maxlength="100"')}</div>
        ${field("c-company", "Công ty", x.company, 'maxlength="200"')}
        ${area("c-quote", "Nội dung đánh giá *", x.quote, 4, 'maxlength="1000"')}
        <div class="grid grid-cols-2 gap-3">
          <div class="space-y-1.5"><label class="${label}" for="c-rating">Số sao</label>
            <select id="c-rating" class="${input}">${[5, 4, 3, 2, 1]
              .map((n) => `<option value="${n}" ${n === (x.rating ?? 5) ? "selected" : ""}>${"★".repeat(n)} (${n})</option>`)
              .join("")}</select></div>
          ${field("c-order", "Thứ tự", x.sortOrder ?? 0, 'type="number"')}</div>
        ${field("c-avatar", "Link ảnh đại diện (https://…)", x.avatarUrl, 'type="url" maxlength="1000"')}
        ${check("c-published", "Hiển thị trên trang chủ", x.published)}`,
      read: ($) => ({
        customerName: $("#c-name").value.trim(),
        position: $("#c-position").value.trim(),
        company: $("#c-company").value.trim(),
        quote: $("#c-quote").value.trim(),
        rating: Number($("#c-rating").value),
        sortOrder: Number($("#c-order").value || 0),
        avatarUrl: $("#c-avatar").value.trim(),
        published: $("#c-published").checked,
      }),
      title: (x) => x.customerName,
    },
  };

  let kind = "portfolio";
  let items = [];

  async function load() {
    const k = KINDS[kind];
    document.getElementById("ct-new-label").textContent = k.newLabel;
    document.querySelectorAll("[data-kind]").forEach((b) => {
      const on = b.dataset.kind === kind;
      b.classList.toggle("bg-dark-navy", on);
      b.classList.toggle("text-white", on);
      b.setAttribute("aria-selected", on);
    });
    try {
      items = k.list(await Auth.api(k.api + (k.query || "")));
    } catch (err) {
      return AdminApp.toast(err.message, true);
    }
    document.getElementById("ct-empty").classList.toggle("hidden", items.length > 0);
    const list = document.getElementById("ct-list");
    list.innerHTML = items
      .map(
        (x, i) => `<li class="row-link row-in flex items-center gap-4 px-5 py-4" style="animation-delay:${i * 30}ms" data-id="${x.id}" tabindex="0">
          <div class="flex-1 min-w-0">${k.row(x)}</div>${k.badge(x)}</li>`,
      )
      .join("");
    list.querySelectorAll("[data-id]").forEach((li) => li.addEventListener("click", () => openForm(li.dataset.id)));
  }

  async function openForm(id) {
    const k = KINDS[kind];
    let x = id ? items.find((i) => i.id === id) : undefined;
    if (id && k.detail) x = await k.detail(id).catch(() => x);
    const b = AdminApp.openDrawer(id ? k.title(x) : k.newLabel, `
      <form id="c-form" class="space-y-4" novalidate>
        ${k.form(x)}
        <p id="c-error" class="hidden text-sm text-red-600 bg-red-50 rounded-xl p-3"></p>
        <div class="flex gap-2">
          <button class="flex-1 bg-primary text-dark-navy rounded-xl py-3 font-semibold hover:bg-[#34b6d0] disabled:opacity-50">Lưu</button>
          ${id ? '<button type="button" id="c-delete" class="px-4 rounded-xl border border-rose-200 text-rose-600 font-semibold hover:bg-rose-50">Xoá</button>' : ""}
        </div>
      </form>`);
    const $ = (s) => b.querySelector(s);
    $("#c-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const btn = e.target.querySelector("button");
      btn.disabled = true;
      try {
        await Auth.api(id ? `${k.api}/${id}` : k.api, { method: id ? "PUT" : "POST", body: k.read($) });
        AdminApp.toast("Đã lưu");
        AdminApp.closeDrawer();
        load();
      } catch (err) {
        $("#c-error").textContent = Object.values(err.errors || {}).join(" · ") || err.message;
        $("#c-error").classList.remove("hidden");
      } finally {
        btn.disabled = false;
      }
    });
    $("#c-delete")?.addEventListener("click", () => {
      if (!confirm("Xoá vĩnh viễn nội dung này?")) return;
      Auth.api(`${k.api}/${id}`, { method: "DELETE" })
        .then(() => {
          AdminApp.toast("Đã xoá");
          AdminApp.closeDrawer();
          load();
        })
        .catch((err) => AdminApp.toast(err.message, true));
    });
  }

  AdminApp.register({
    id: "content",
    label: "Nội dung",
    icon: "edit_document",
    roles: ["MANAGER", "ADMIN"],
    init() {
      const box = document.getElementById("ct-kinds");
      box.innerHTML = Object.entries(KINDS)
        .map(([key, k]) => `<button data-kind="${key}" role="tab" class="px-4 py-2 rounded-lg text-sm font-semibold">${k.label}</button>`)
        .join("");
      box.querySelectorAll("[data-kind]").forEach((b) =>
        b.addEventListener("click", () => {
          kind = b.dataset.kind;
          load();
        }),
      );
      document.getElementById("ct-new").addEventListener("click", () => openForm());
    },
    show: load,
  });
})();
