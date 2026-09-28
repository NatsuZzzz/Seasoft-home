# TC — Đăng ký tài khoản (`POST /api/auth/register`, `register.html`)

- Ngày test: 2026-09-28 · Test tự động: `RegisterApiTest` (12/12 pass) · Test UI: trình duyệt thật trên http://localhost:8080

| ID | Kịch bản | Dữ liệu / Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-R01 | Đăng ký hợp lệ | Họ tên, email mới, SĐT, mật khẩu ≥ 6 | 200, trả `token`, `refreshToken`, role `CUSTOMER` | Đúng | ✅ |
| TC-R02 | Mật khẩu được hash | Đọc `password_hash` trong DB | Không phải plain text, dạng BCrypt `$2...` | Đúng | ✅ |
| TC-R03 | Email trùng | Đăng ký lại cùng email | 400 "Email đã được sử dụng" | Đúng | ✅ |
| TC-R04 | Email trùng khác hoa/thường + khoảng trắng | `"  EMAIL@X.TEST "` | 400 "Email đã được sử dụng" | Đúng (trước khi fix: 400 vì sai định dạng) | ✅ |
| TC-R05 | Chuẩn hoá email | Gửi email viết HOA | Lưu và trả về chữ thường | Đúng | ✅ |
| TC-R06 | Họ tên trống | `fullName=""` | 400, `errors.fullName` | Đúng | ✅ |
| TC-R07 | Email sai định dạng | `khong-phai-email` | 400, `errors.email` | Đúng | ✅ |
| TC-R08 | Mật khẩu ngắn | `12345` | 400, `errors.password` | Đúng | ✅ |
| TC-R09 | SĐT sai định dạng | `abc` | 400, `errors.phone` | Đúng | ✅ |
| TC-R10 | Tự gán role ADMIN | Body có `"role":"ADMIN"` | Bị bỏ qua, role vẫn `CUSTOMER` | Đúng | ✅ |
| TC-R11 | Body rỗng | `{}` | 400, lỗi `email`, `password` | Đúng | ✅ |
| TC-R12 | Đăng ký xong đăng nhập được ngay | status = `ACTIVE`, login | 200 | Đúng | ✅ |
| TC-R13 | UI: đăng ký thành công | Điền form, tick điều khoản, bấm Tạo tài khoản | Hiện thông báo thành công, lưu 3 key token/user, về `Page.html`, header hiện tên | Đúng | ✅ |
| TC-R14 | UI: đã đăng nhập mà mở `register.html` | Có token | Tự chuyển về `Page.html` | Đúng | ✅ |
