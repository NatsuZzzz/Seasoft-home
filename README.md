# SeaSoft — Website thiết kế web cho doanh nghiệp B2B

| Thư mục | Nội dung |
|---|---|
| `seasoft/` | Backend Spring Boot 4 (Java 21) + PostgreSQL + JWT |
| `Html/` | Frontend HTML tĩnh (Tailwind) |
| `Logo/` | Logo, favicon |
| `Database/db.sql` | Schema gốc (tham khảo) — schema thật do Flyway quản lý ở `seasoft/src/main/resources/db/migration` |
| `docs/testcases/` | Testcase theo từng chức năng |
| `PLAN.md` | Kế hoạch tới lúc deploy |

## Chạy local

Yêu cầu: JDK 21, PostgreSQL (có DB `SeaSoft`, và DB `seasoft_test` để chạy test).

```bash
cd seasoft
cp .env.example .env      # điền DB_PASSWORD, JWT_SECRET
./mvnw spring-boot:run    # web + API ở http://localhost:8080
```

Spring Boot serve luôn thư mục `Html/` (chạy lệnh từ trong `seasoft/`), nên mở http://localhost:8080 là vào trang chủ.
Chưa cấu hình SMTP thì email (quên mật khẩu) được in ra console với tiền tố `[DEV MAIL]`.

Bật log SQL khi dev: `SPRING_PROFILES_ACTIVE=dev`.

## Build CSS frontend (Tailwind)

`Html/css/tailwind.css` là file **build** (đã commit sẵn). Sửa class Tailwind trong `Html/` thì build lại:

```bash
npm install
npm run build:css      # hoặc: npm run watch:css khi đang sửa giao diện
```

## Biến môi trường

| Biến | Bắt buộc | Mặc định |
|---|---|---|
| `DB_URL` | | `jdbc:postgresql://localhost:5432/SeaSoft` |
| `DB_USERNAME` | | `postgres` |
| `DB_PASSWORD` | ✅ | |
| `JWT_SECRET` | ✅ | base64, ≥ 32 byte (`openssl rand -base64 32`) |
| `JWT_EXPIRATION` | | `900000` (15 phút, ms) |
| `JWT_REFRESH_EXPIRATION` | | `604800000` (7 ngày, ms) |
| `FRONTEND_URL` | ✅ ở prod | `http://localhost:8080` (dùng cho link trong email) |
| `SPRING_MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | | trống → chỉ log mail |
| `CORS_ORIGINS` | ✅ ở prod | `*` |
| `PORT` | | `8080` |
| `TEST_DB_URL` | | `jdbc:postgresql://localhost:5432/seasoft_test` |

## Test

```bash
cd seasoft
./mvnw test
```

## Format lỗi API

```json
{ "status": 400, "message": "Dữ liệu không hợp lệ", "errors": { "email": "Email không hợp lệ" } }
```
