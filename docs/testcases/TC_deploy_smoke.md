# TC — Smoke test bản production (Render + Neon)

- Ngày test: 2026-09-29 · URL: https://seasoft.onrender.com · Render Web Service `seasoft` (Docker, Free, Singapore) · Neon Postgres 18 (Singapore, kết nối trực tiếp, không qua pooler) · Commit `29ed3fd`
- Công cụ: `curl` từ máy dev + trình duyệt thật + log Render

| ID | Kịch bản | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-DS01 | Health | `GET /actuator/health` | 200 `UP` | 200 `UP` (0.4s khi đang thức) | ✅ |
| TC-DS02 | Flyway trên Neon trống | Log khởi động | Tạo schema history, chạy V1→V5 thành công | "Successfully applied 5 migrations … v5" | ✅ |
| TC-DS03 | Seed ADMIN | Log `AdminBootstrap` | "Da tao tai khoan ADMIN dau tien" | Có | ✅ |
| TC-DS04 | Trang tĩnh | `/Page.html`, `/login.html`, `/register.html`, `/admin.html` | 200 | 200 | ✅ |
| TC-DS05 | Asset tự host | `/css/tailwind.css`, `/vendor/gsap.min.js`, `/fonts/material-symbols.woff2` | 200 | 200 | ✅ |
| TC-DS06 | Domain gốc | `GET /` | Redirect `/Page.html` | Đúng | ✅ |
| TC-DS07 | Ép HTTPS | `GET http://…/Page.html` | 301 sang https | 301 | ✅ |
| TC-DS08 | Security headers | `HEAD /Page.html` | CSP, HSTS, X-Frame-Options DENY, Referrer-Policy | Có đủ | ✅ |
| TC-DS09 | API công khai | `/api/public/portfolio`, `/testimonials` | 200, portfolio 3 mục seed | 200, 3 mục | ✅ |
| TC-DS10 | API cần đăng nhập | `GET /api/users/me` | 401 | 401 | ✅ |
| TC-DS11 | Actuator ẩn | `GET /actuator/env` | Không lộ (≠ 200) | 401 | ✅ |
| TC-DS12 | Blog tắt | `/blog.html`, `/api/public/blog`, trang chủ | 302, 404, 0 link blog | Đúng | ✅ |
| TC-DS13 | SEO theo domain thật | `/sitemap.xml`, `/robots.txt` | Chỉ `https://seasoft.onrender.com/Page.html`; robots trỏ sitemap đúng domain | Đúng | ✅ |
| TC-DS14 | CORS | Preflight từ `https://evil.example` và từ domain thật | 403 / 200 | 403 / 200 | ✅ |
| TC-DS15 | Login sai | `POST /api/auth/login` tài khoản không tồn tại | 401 | 401 | ✅ |
| TC-DS16 | UI trang chủ | Mở trên trình duyệt, xem console | API cùng origin, 3 dự án hiển thị, có form tư vấn, không lỗi console/CSP | Đúng | ✅ |
| TC-DS17 | Nén + tốc độ | `GET /Page.html` có `--compressed` | Có nén, phản hồi nhanh khi đang thức | 15 KB, 0.16s | ✅ |

## Ghi chú vận hành
- Khởi động lạnh: Spring mất ~98s trên gói Free (0.1 CPU). Sau 15 phút không có truy cập, instance ngủ → lần mở tiếp chờ ~1–2 phút. Khắc phục ở 9.8 (UptimeRobot ping `/actuator/health`) hoặc lên gói trả phí / VPS.
- Quá trình lên sóng: service đầu (`Seasoft-home`) tạo ở Oregon và thiếu env → fail; service thứ 2 (`Seasoft-home-1`) thiếu `DB_URL` → app kết nối `localhost:5432` và fail. Cả hai cần xoá. Service dùng thật: `seasoft`.
- `JWT_SECRET` sinh bằng nút Generate của Render chỉ có 24 byte → app từ chối (cần ≥ 32 byte). Dùng lệnh sinh 48 byte trong `docs/DEPLOY.md`.
- `ADMIN_PASSWORD` < 8 ký tự → app **bỏ qua** tạo admin (chỉ WARN), cần để ý log.
