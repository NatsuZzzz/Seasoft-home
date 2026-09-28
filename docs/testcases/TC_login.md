# TC — Đăng nhập / Đăng xuất (`POST /api/auth/login`, `/logout`, `login.html`)

- Ngày test: 2026-09-28 · Test tự động: `LoginApiTest` (12/12 pass) · Test UI: trình duyệt thật

| ID | Kịch bản | Dữ liệu / Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-L01 | Đăng nhập đúng | Email + mật khẩu đúng | 200, có `token` + `refreshToken` | Đúng | ✅ |
| TC-L02 | Sai mật khẩu | | 401 "Email hoặc mật khẩu không đúng" | Đúng | ✅ |
| TC-L03 | Email không tồn tại | | 401, **cùng** message với TC-L02 (không lộ email nào tồn tại) | Đúng | ✅ |
| TC-L04 | Email khác hoa/thường, có khoảng trắng | `" EMAIL@X.TEST "` | 200 | Đúng (bug đã fix: trước trả 400) | ✅ |
| TC-L05 | Mật khẩu phân biệt hoa/thường | Mật khẩu viết thường | 401 | Đúng | ✅ |
| TC-L06 | Tài khoản bị khoá | status = SUSPENDED | 403 "Tài khoản đã bị khoá" | Đúng | ✅ |
| TC-L07 | Tài khoản chưa kích hoạt | status = INACTIVE | 403 "Tài khoản chưa được kích hoạt" | Đúng | ✅ |
| TC-L08 | Để trống | `""`, `""` | 400, lỗi từng field | Đúng | ✅ |
| TC-L09 | JSON hỏng | `{oops` | 400 "Request body không hợp lệ" | Đúng | ✅ |
| TC-L10 | Access token dùng được | Gọi `/api/users/me` | 200 đúng user | Đúng | ✅ |
| TC-L11 | Cập nhật `last_login_at` | Login | Cột được set | Đúng | ✅ |
| TC-L12 | User bị khoá sau khi đã có token | Khoá rồi gọi API | 401 | Đúng | ✅ |
| TC-L13 | UI: "Ghi nhớ" không tick | Login | Token lưu ở `sessionStorage`, không ở `localStorage` | Đúng | ✅ |
| TC-L14 | UI: `?next=profile.html` | Login từ trang bị guard | Quay về `profile.html` | Đúng | ✅ |
| TC-L15 | UI: chống open redirect | `next=https://evil.com`, `//evil.com`, `javascript:...` | Về `Page.html` | Đúng | ✅ |
| TC-L16 | UI: Đăng xuất | Bấm nút logout | Xoá token, về `Page.html`, header hiện lại Đăng nhập/Tạo tài khoản; refresh token cũ → 401 | Đúng | ✅ |
