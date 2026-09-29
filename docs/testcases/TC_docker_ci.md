# TC — Docker image, docker-compose, CI (Phase 9.1–9.4)

- Ngày test: 2026-09-29 · Nơi chạy: GitHub Actions (`ubuntu-latest`), run `CI` của commit `7f3353f` · Docker Desktop local bị lỗi "unable to start" nên toàn bộ phần image được kiểm chứng trên CI
- Kịch bản smoke test ở bước "Smoke test" trong `.github/workflows/ci.yml`

| ID | Kịch bản | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-DC01 | Test tự động trên CI | Job `test`: Postgres 17 service + `mvn -B test` | 182/182 pass | Pass (55s) | ✅ |
| TC-DC02 | Build image multi-stage | `docker compose up -d --build` | Maven build ở stage 1, image chạy là JRE Alpine + jar, user không phải root | Build OK | ✅ |
| TC-DC03 | Postgres chờ healthy | `depends_on: condition: service_healthy` | App chỉ khởi động sau khi `pg_isready` OK | Đúng | ✅ |
| TC-DC04 | Flyway trên DB trống | Lần chạy đầu với volume mới | Tạo đủ bảng V1–V5, app khởi động được | Đúng | ✅ |
| TC-DC05 | Health check | `GET /actuator/health` | `UP` trong vòng 180s | UP | ✅ |
| TC-DC06 | Frontend nằm trong jar | `GET /Page.html`, `/login.html`, `/css/tailwind.css` | 200 (profile prod chỉ đọc `classpath:/static`) | 200 | ✅ |
| TC-DC07 | API công khai | `GET /api/public/portfolio` | 200 | 200 | ✅ |
| TC-DC08 | API cần đăng nhập | `GET /api/users/me` không token | 401 | 401 | ✅ |
| TC-DC09 | Actuator khác bị chặn | `GET /actuator/env` | Khác 200 | Không phải 200 | ✅ |
| TC-DC10 | Blog tắt trong bản deploy | `GET /blog.html`, `GET /api/public/blog` | 302, 404 | 302, 404 | ✅ |
| TC-DC11 | Security headers | `HEAD /Page.html` | Có `Content-Security-Policy` | Có | ✅ |
| TC-DC12 | Secret không nằm trong repo | CI tạo `.env` ngẫu nhiên; kiểm tra file staged | Không có JWT secret/mật khẩu thật trong commit | Đúng | ✅ |
| TC-DC13 | Thiếu biến bắt buộc | `.env` không có `DB_PASSWORD`, `docker compose config` | Báo lỗi "Chua dat DB_PASSWORD trong .env", không chạy | Đúng (local) | ✅ |
| TC-DC14 | Lỗi build hiện ra ngoài | Build fail | Log lỗi hiện thành annotation trên trang run (xem không cần đăng nhập) | Đã thêm sau run #1 | ✅ |

## Chạy local bằng Docker Desktop (2026-09-29, cổng 8081)

| ID | Kịch bản | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-DC15 | Build + khởi động | `docker compose up -d --build` | Build OK, app healthy | Build 117s, UP sau 12s, image 408 MB | ✅ |
| TC-DC16 | Container không chạy bằng root | `docker compose exec app whoami` | `app` | `app` | ✅ |
| TC-DC17 | Seed ADMIN từ env | Login `ADMIN_EMAIL`/`ADMIN_PASSWORD` qua API và UI | 200, vào được `admin.html` (tab Tổng quan) | Đúng | ✅ |
| TC-DC18 | Sai mật khẩu | Login admin với mật khẩu sai | 401 | 401 | ✅ |
| TC-DC19 | Flyway | Bảng `flyway_schema_history` | V1–V5 thành công | 1,2,3,4,5 | ✅ |
| TC-DC20 | Dữ liệu bền qua restart | `docker compose down` (giữ volume) → `up`, login lại | Admin còn, không bị tạo trùng (1 user) | Đúng | ✅ |
| TC-DC21 | Form tư vấn | `POST /api/consultations` thiếu dịch vụ, rồi đủ trường | 400 `errors.serviceType`, sau đó 201 + lưu DB trạng thái NEW | Đúng | ✅ |
| TC-DC22 | Trang chủ chạy ở cổng khác 8080 | Mở `http://localhost:8081`, xem network/console | API gọi cùng origin (8081), không lỗi CSP | **Lần đầu fail**: `auth.js` gọi sang `localhost:8080`, CSP chặn → sửa, lần 2 pass | ✅ |
| TC-DC23 | `.env` gốc không bị commit | `git check-ignore .env` | Bị ignore, `.env.example` vẫn track | **Lần đầu fail**: `.env` gốc chưa có trong `.gitignore` → thêm, pass | ✅ |

## Ghi chú
- Run #1 (`b3e03c2`) fail ở bước `docker compose up` sau 20s mà không có log công khai. Run #2 dùng cùng Dockerfile và compose đã pass toàn bộ, nên nhiều khả năng là lỗi tạm thời lúc pull image. Đã bổ sung annotation để lần sau đọc được nguyên nhân.
- Docker Desktop local trước đó hỏng (ổ C đầy + database engine bị hỏng từ 01/09). Chủ dự án đã chuyển ổ ảo sang `D:\Tool\Docker` và purge data, sau đó chạy được.
