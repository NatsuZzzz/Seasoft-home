# TC — Tạm ẩn blog (cờ `FEATURE_BLOG`)

- Ngày test: 2026-09-29 · Test tự động: `BlogFeatureFlagTest` (8/8 pass), toàn bộ 182/182 pass · Test UI: trình duyệt thật (profile dev, `FEATURE_BLOG` không set = tắt)
- Bật lại blog: set `FEATURE_BLOG=true` + bỏ comment 3 link có đánh dấu `FEATURE_BLOG` trong `Html/Page.html`

| ID | Kịch bản | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-BF01 | Mở trang blog | `GET /blog.html` | 302 → `/Page.html` | Đúng | ✅ |
| TC-BF02 | Mở bài viết | `GET /post.html?slug=...` | 302 → `/Page.html` (không SSR bài) | Đúng | ✅ |
| TC-BF03 | API danh sách blog | `GET /api/public/blog` | 404 JSON `{status:404}` | Đúng | ✅ |
| TC-BF04 | API bài đã đăng | Tạo bài PUBLISHED, `GET /api/public/blog/{slug}` | 404 | Đúng | ✅ |
| TC-BF05 | Sitemap | Có bài PUBLISHED, `GET /sitemap.xml` | Có `/Page.html`, không có `blog.html`/slug bài | Đúng | ✅ |
| TC-BF06 | Admin vẫn soạn bài | ADMIN tạo bài nháp, `GET /api/admin/content/blog` | 200, thấy bài (soạn sẵn chờ mở blog) | Đúng | ✅ |
| TC-BF07 | Nội dung công khai khác | `GET /api/public/portfolio`, `/testimonials` | 200, không bị ảnh hưởng | Đúng | ✅ |
| TC-BF08 | Trang chủ + robots | `GET /Page.html`, `/robots.txt` | 200 | Đúng | ✅ |
| TC-BF09 | Blog bật (test cũ) | Profile test `app.features.blog=true`, chạy `PublicContentApiTest` | Blog, bài, sitemap hoạt động như cũ | Đúng | ✅ |
| TC-BF10 | UI: menu desktop | Mở trang chủ | Không còn mục "Blog" trên thanh menu | Đúng | ✅ |
| TC-BF11 | UI: menu mobile + footer | Đếm thẻ `<a>` trỏ tới blog | 0 link (menu mobile + "Blog kiến thức" ở footer đã ẩn) | Đúng | ✅ |
| TC-BF12 | UI: gõ thẳng URL | Trình duyệt mở `/blog.html`, `/post.html?slug=x` | Về trang chủ, không lỗi console | Đúng | ✅ |
