# TC — SEO (meta, Open Graph, JSON-LD, favicon, `robots.txt`, `sitemap.xml`)

- Ngày test: 2026-09-28 · Test tự động: `PublicContentApiTest` (TC_PC08, TC_PC09) · Kiểm tra thủ công: curl + browser
- `robots.txt` và `sitemap.xml` sinh động theo `FRONTEND_URL` (dev: http://localhost:8080)

| ID | Kịch bản | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-SEO01 | Meta description trang chủ | Đọc `<head>` Page.html | Có `meta description` mô tả dịch vụ | Đúng | ✅ |
| TC-SEO02 | Open Graph | Đọc `<head>` | `og:type`, `og:title`, `og:description`, `og:image`, `og:locale=vi_VN`, `twitter:card` | Đúng | ✅ |
| TC-SEO03 | Dữ liệu có cấu trúc | Đọc `<head>` | Có `application/ld+json` kiểu Organization (tên, email, SĐT, địa chỉ) | Đúng | ✅ |
| TC-SEO04 | Favicon | Mọi trang HTML | Có `link rel=icon` → `img/favicon.png` (tất cả trang) | Đúng | ✅ |
| TC-SEO05 | noindex trang riêng tư | admin, dashboard, project, profile, forgot/reset | Có `meta robots noindex` | Đúng (6 trang) | ✅ |
| TC-SEO06 | robots.txt | GET `/robots.txt` | Chặn `/admin.html`, `/dashboard.html`, `/project.html`, `/profile.html`, `/reset-password.html`, `/api/`; có dòng `Sitemap:` | Đúng | ✅ |
| TC-SEO07 | sitemap.xml cơ bản | GET `/sitemap.xml` | XML hợp lệ, có Page.html (priority 1.0) và blog.html | Đúng | ✅ |
| TC-SEO08 | sitemap có bài đã đăng | Đăng 1 bài | Có `post.html?slug=…` kèm `lastmod` | Đúng | ✅ |
| TC-SEO09 | sitemap không có bài nháp | Tạo 1 nháp | Không có slug nháp | Đúng | ✅ |
| TC-SEO10 | Meta động cho bài viết | Mở `post.html?slug=…` | `document.title`, `meta description`, `og:title`, `og:description` theo bài | Đúng | ✅ |
| TC-SEO11 | Bài không tồn tại | Mở slug lạ | Hiện "Bài viết không tồn tại…" + thêm `meta robots noindex` | Theo code (`showError`) | ✅ |
| TC-SEO12 | Menu + footer có Blog | Trang chủ | Link `blog.html` ở menu desktop, menu mobile, footer | Đúng | ✅ |
