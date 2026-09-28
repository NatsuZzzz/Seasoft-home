# TC — Hồ sơ cá nhân (`/api/users/me`, `/api/users/me/password`, `profile.html`)

- Ngày test: 2026-09-28 · Test tự động: `UserMeApiTest` (12/12 pass) · Test UI: trình duyệt thật

| ID | Kịch bản | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-U01 | Xem hồ sơ | `GET /me` có token | 200, có email/role/status, **không** có passwordHash | Đúng | ✅ |
| TC-U02 | Không token | `GET /me` | 401 | Đúng | ✅ |
| TC-U03 | Sửa họ tên + SĐT | `PUT /me` | 200, lưu thật (GET lại thấy mới) | Đúng | ✅ |
| TC-U04 | Họ tên trống | | 400 `errors.fullName` | Đúng | ✅ |
| TC-U05 | Cố đổi email/role/status | Gửi kèm field lạ | Bị bỏ qua, email & role giữ nguyên | Đúng | ✅ |
| TC-U06 | Xoá SĐT | `phone=""` | Lưu null | Đúng | ✅ |
| TC-U07 | Đổi mật khẩu đúng | | 200, login mật khẩu mới OK, cũ 401 | Đúng | ✅ |
| TC-U08 | Sai mật khẩu hiện tại | | 400 "Mật khẩu hiện tại không đúng" | Đúng | ✅ |
| TC-U09 | Mật khẩu mới trùng cũ | | 400 | Đúng | ✅ |
| TC-U10 | Đổi mật khẩu → thu hồi refresh token | | Refresh token cũ 401 | Đúng | ✅ |
| TC-U11 | Chỉ thấy hồ sơ của mình | 2 user, token A | Trả về A | Đúng | ✅ |
| TC-U12 | Token bị sửa | Thêm ký tự vào token | 401 | Đúng | ✅ |
| TC-U13 | UI: guard | Chưa đăng nhập mở `profile.html` | Chuyển `login.html?next=profile.html` | Đúng | ✅ |
| TC-U14 | UI: lưu hồ sơ | Sửa tên + SĐT, bấm Lưu | "Đã lưu ✓", sidebar + header cập nhật tên | Đúng | ✅ |
| TC-U15 | UI: validate đổi mật khẩu | Không khớp / quá ngắn / sai hiện tại / trùng cũ | Hiện đúng 4 thông báo | Đúng | ✅ |
| TC-U16 | UI: đổi mật khẩu thành công | | Thông báo, xoá token, về login với `next=profile.html` | Đúng | ✅ |
