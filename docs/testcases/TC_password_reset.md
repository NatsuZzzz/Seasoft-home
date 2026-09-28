# TC — Quên / Đặt lại mật khẩu (`/api/auth/forgot-password`, `/reset-password`, 2 trang HTML)

- Ngày test: 2026-09-28 · Test tự động: `PasswordResetApiTest` (12/12 pass) · Test UI: trình duyệt thật, link lấy từ log `[DEV MAIL]`
- Link hiệu lực 30 phút, dùng 1 lần, DB chỉ lưu hash

| ID | Kịch bản | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-P01 | Email tồn tại | Gửi forgot | 200 message chung, mail có link `reset-password.html?token=...` | Đúng | ✅ |
| TC-P02 | Email không tồn tại | Gửi forgot | 200 **cùng** message, không gửi mail (không lộ tài khoản) | Đúng | ✅ |
| TC-P03 | Email sai định dạng | `abc` | 400 | Đúng | ✅ |
| TC-P04 | Đặt lại thành công | Dùng token, mật khẩu mới | 200, login bằng mật khẩu mới OK | Đúng | ✅ |
| TC-P05 | Mật khẩu cũ hết tác dụng | Login mật khẩu cũ | 401 | Đúng (bug entity detached đã fix) | ✅ |
| TC-P06 | Token dùng 2 lần | Reset lần 2 | 400 "Link ... không hợp lệ hoặc đã hết hạn" | Đúng | ✅ |
| TC-P07 | Token hết hạn | `expires_at` quá khứ | 400 | Đúng | ✅ |
| TC-P08 | Token bịa | | 400 | Đúng | ✅ |
| TC-P09 | Mật khẩu mới quá ngắn | `123` | 400 `errors.newPassword` | Đúng | ✅ |
| TC-P10 | Xin link mới → link cũ vô hiệu | Forgot 2 lần | Link 1: 400, link 2: 200 | Đúng | ✅ |
| TC-P11 | Reset → đăng xuất mọi thiết bị | Refresh token cũ sau reset | 401 | Đúng | ✅ |
| TC-P12 | Tài khoản bị khoá | Forgot | 200 message chung, không gửi mail | Đúng | ✅ |
| TC-P13 | UI: email sai định dạng | Nhập `sai-dinh-dang` | Hiện lỗi dưới ô, không gọi API | Đúng | ✅ |
| TC-P14 | UI: flow đầy đủ | Nhập email HOA → mở link → nhập 2 lần không khớp → nhập đúng | Báo "không khớp"; sau đó hiện "Xong rồi!", login mật khẩu mới 200 | Đúng | ✅ |
