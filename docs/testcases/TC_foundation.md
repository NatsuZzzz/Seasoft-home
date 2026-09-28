# TC — Phase 1: Nền móng (config, Flyway, xử lý lỗi, bảo mật cơ bản)

- Ngày test: 2026-09-28
- Môi trường: Windows 11, JDK 21.0.8, PostgreSQL 18.3, Spring Boot 4.1.1
- Cách test: `./mvnw test` (MockMvc, DB `seasoft_test`) + chạy jar thật với DB `SeaSoft` và gọi bằng curl

| ID | Chức năng | Bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-F01 | Config env | Chạy app, secret lấy từ `seasoft/.env` (không có trong `application.properties`) | App khởi động, kết nối DB OK | Started in 4.7s | ✅ Pass |
| TC-F02 | Flyway DB trống | Chạy test trên `seasoft_test` rỗng | Tạo `flyway_schema_history`, apply V1 + V2 | "Successfully applied 2 migrations ... v2" | ✅ Pass |
| TC-F03 | Flyway DB cũ | Chạy app trên DB `SeaSoft` đã có bảng | Baseline V1, chỉ chạy V2, không mất data | Baseline v1, apply V2, 2 user cũ còn nguyên | ✅ Pass |
| TC-F04 | Hibernate validate | `ddl-auto=validate` sau migrate | Entity khớp schema, app không lỗi | Không lỗi validate | ✅ Pass |
| TC-F05 | Lỗi validation | `POST /api/auth/register` với `fullName=""`, `email="bad"`, `password="1"` | 400, `message` + `errors` theo từng field | 400, đủ 3 lỗi field | ✅ Pass |
| TC-F06 | Body JSON hỏng | `POST /api/auth/login` body `{oops` | 400 "Request body không hợp lệ" | Đúng | ✅ Pass |
| TC-F07 | Lỗi nghiệp vụ | Đăng ký trùng email | 400 "Email đã được sử dụng" | Đúng | ✅ Pass |
| TC-F08 | Sai mật khẩu | Login với password sai | 401 "Email hoặc mật khẩu không đúng" | Đúng | ✅ Pass |
| TC-F09 | Không có token | `GET /api/users/me` không header Authorization | 401 JSON "Bạn cần đăng nhập" (trước đây 403 body rỗng) | 401 JSON | ✅ Pass (auto test) |
| TC-F10 | Token rác | Header `Bearer not-a-jwt` | 401, **không** 500 (trước đây filter ném exception) | 401 | ✅ Pass (auto test) |
| TC-F11 | Token chữ ký sai | Header `Bearer abc.def.ghi` | 401 | 401 | ✅ Pass |
| TC-F12 | Đăng ký + login OK | Đăng ký email mới rồi login | 200, trả token JWT | 200, có token | ✅ Pass |
| TC-F13 | Secret không vào git | `git status` | `seasoft/.env` bị ignore, chỉ có `.env.example` | Đúng | ✅ Pass |
