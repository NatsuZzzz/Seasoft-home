# Deploy SeaSoft

App đóng gói thành **1 image Docker** (backend + frontend). Mọi cấu hình đi qua biến môi trường (xem [`.env.example`](../.env.example)), nên chạy ở Render, Railway hay VPS thì image vẫn y hệt.

- **Giai đoạn 1 (lên sóng nhanh)**: Render (app, free) + Neon (Postgres, free)
- **Giai đoạn 2 (lâu dài)**: VPS + Docker Compose + Nginx + Certbot

---

## Giai đoạn 1 — Render + Neon

### 1. Tạo database trên Neon
1. Đăng ký https://neon.tech (đăng nhập bằng GitHub) → **Create project**, region **Singapore** (gần VN nhất), Postgres 17.
2. Vào **Connection Details**, lấy chuỗi dạng `postgresql://USER:PASSWORD@HOST/DBNAME?sslmode=require`.
3. Tách ra thành 3 biến (lưu ý JDBC **không** chứa user/pass trong URL):
   - `DB_URL` = `jdbc:postgresql://HOST/DBNAME?sslmode=require`
   - `DB_USERNAME` = `USER`
   - `DB_PASSWORD` = `PASSWORD`

Flyway tự tạo bảng ở lần khởi động đầu tiên, không cần chạy SQL tay.

### 2. Tạo web service trên Render
1. Đăng ký https://render.com (bằng GitHub) → **New → Web Service** → chọn repo `Seasoft-home`.
2. Language: **Docker** (Render tự đọc `Dockerfile` ở gốc repo). Region **Singapore**. Plan: **Free**.
3. **Health Check Path**: `/actuator/health`
4. **Environment** → thêm các biến (điền trực tiếp trên dashboard, không commit):

| Biến | Giá trị |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | từ Neon (bước 1) |
| `JWT_SECRET` | chạy `openssl rand -base64 48` rồi dán kết quả |
| `CORS_ORIGINS` | `https://<ten-service>.onrender.com` (có domain thật thì thêm vào, cách nhau dấu phẩy) |
| `FRONTEND_URL` | `https://<ten-service>.onrender.com` |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME` | tài khoản admin đầu tiên (mật khẩu ≥ 8 ký tự, tên không dấu) |
| `FEATURE_BLOG` | `false` |
| `SPRING_MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `LEAD_NOTIFY_EMAIL` | khi có SMTP (chưa set thì mail chỉ ghi ra log) |

Không cần set `PORT`: Render tự cấp và app đọc biến này.

5. **Deploy**. Lần đầu mất khoảng 5–8 phút (build Maven). Mỗi lần push `main` Render sẽ tự deploy lại.
6. Đăng nhập admin xong thì **xoá `ADMIN_PASSWORD`** khỏi Environment (admin chỉ được tạo 1 lần; mật khẩu đổi trong trang Hồ sơ).

### 3. Hạn chế của gói free
- Render free **ngủ sau 15 phút** không có truy cập, lần mở lại chờ khoảng 1 phút. Có thể đặt UptimeRobot ping `/actuator/health` mỗi 5 phút để app không ngủ (750 giờ free/tháng, đủ cho 1 service chạy 24/7).
- Neon free cũng tự tạm dừng DB khi rảnh, lần gọi đầu chậm khoảng 1 giây.
- RAM 512 MB: Dockerfile đã giới hạn heap (`MaxRAMPercentage=75`, SerialGC).

### 4. Domain riêng (khi có)
Render → Settings → **Custom Domains** → thêm `seasoft.vn` và `www.seasoft.vn` → tạo bản ghi DNS theo hướng dẫn (CNAME/ALIAS). HTTPS được cấp tự động. Sau đó cập nhật `CORS_ORIGINS` và `FRONTEND_URL` về domain thật.

---

## Giai đoạn 2 — Chuyển sang VPS (khi cần)

1. Trên VPS (Ubuntu), cài Docker, rồi `git clone` repo.
2. `cp .env.example .env` → điền giá trị (DB giờ nằm trong container `db` nên chỉ cần `DB_PASSWORD`).
3. Chuyển dữ liệu từ Neon:
   ```bash
   pg_dump "postgresql://USER:PASS@NEON_HOST/DB?sslmode=require" -Fc -f seasoft.dump
   docker compose up -d db
   docker compose exec -T db pg_restore -U seasoft -d seasoft --no-owner < seasoft.dump
   ```
4. `docker compose up -d --build` → app chạy ở cổng 8080.
5. Nginx reverse proxy từ 443 vào `localhost:8080` + Certbot để lấy HTTPS. App đã bật `forward-headers-strategy` nên vẫn lấy đúng IP thật của khách cho rate limit.
6. Trỏ DNS domain sang IP VPS → tắt service Render.

---

## Chạy thử local bằng Docker

```bash
cp .env.example .env   # điền DB_PASSWORD, JWT_SECRET, ADMIN_*
docker compose up -d --build
```
Mở http://localhost:8080. Để kiểm tra health: `curl localhost:8080/actuator/health`.

## CI
`.github/workflows/ci.yml`: mỗi lần push/PR, CI chạy toàn bộ test (có Postgres), sau đó build image, chạy `docker compose` và smoke test (trang, API, bảo mật, blog tắt).
