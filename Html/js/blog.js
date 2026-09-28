/* SeaSoft - dung chung cho blog.html va post.html */
(function () {
  "use strict";

  const esc = window.Labels.esc;

  const Blog = {
    header() {
      return `<header class="w-full px-6 pt-6 pb-4">
        <div class="max-w-screen-lg mx-auto flex items-center justify-between gap-4">
          <a href="Page.html" class="flex items-center gap-2 font-headline text-2xl font-bold tracking-tight text-dark-navy" aria-label="SeaSoft - Trang chủ">
            <span class="w-9 h-9 rounded-lg bg-surface-alt text-primary flex items-center justify-center">
              <span class="material-symbols-outlined text-xl">waves</span></span>
            <span>Sea<span class="text-primary">Soft</span></span>
          </a>
          <nav class="flex items-center gap-1 sm:gap-3 text-sm font-semibold">
            <a href="Page.html" class="px-3 py-2 rounded-lg hover:bg-surface-alt hidden sm:inline-flex">Trang chủ</a>
            <a href="blog.html" class="px-3 py-2 rounded-lg hover:bg-surface-alt">Blog</a>
            <a href="Page.html#contact" class="px-4 py-2 rounded-lg bg-primary text-white hover:bg-[#34b6d0]">Nhận tư vấn</a>
          </nav>
        </div></header>`;
    },

    // Van ban -> HTML an toan: escape truoc, sau do moi tao the (khong bao gio chen HTML tho)
    // "## " -> h2, "### " -> h3, "- " -> danh sach, dong trong -> doan moi
    render(text) {
      const out = [];
      let para = [];
      let list = [];
      const flushPara = () => {
        if (para.length) out.push(`<p>${para.map(esc).join("<br>")}</p>`);
        para = [];
      };
      const flushList = () => {
        if (list.length) out.push(`<ul>${list.map((li) => `<li>${esc(li)}</li>`).join("")}</ul>`);
        list = [];
      };
      String(text || "")
        .split(/\r?\n/)
        .forEach((line) => {
          const t = line.trim();
          if (!t) {
            flushPara();
            flushList();
          } else if (t.startsWith("### ")) {
            flushPara();
            flushList();
            out.push(`<h3>${esc(t.slice(4))}</h3>`);
          } else if (t.startsWith("## ")) {
            flushPara();
            flushList();
            out.push(`<h2>${esc(t.slice(3))}</h2>`);
          } else if (t.startsWith("- ")) {
            flushPara();
            list.push(t.slice(2));
          } else {
            flushList();
            para.push(t);
          }
        });
      flushPara();
      flushList();
      return out.join("\n");
    },

    date(iso) {
      return iso ? new Date(iso).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }) : "";
    },

    // Cap nhat the meta cho SEO / chia se khi noi dung tai xong
    setMeta(name, value, attr = "name") {
      let m = document.head.querySelector(`meta[${attr}="${name}"]`);
      if (!m) {
        m = document.createElement("meta");
        m.setAttribute(attr, name);
        document.head.appendChild(m);
      }
      m.setAttribute("content", value);
    },
  };

  window.Blog = Blog;
})();
