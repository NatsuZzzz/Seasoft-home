# TC — Refresh token (`POST /api/auth/refresh`, `/logout`, auto-refresh trong `js/auth.js`)

- Ngày test: 2026-09-28 · Test tự động: `RefreshTokenApiTest` (12/12 pass) · Test UI: trình duyệt thật
- Cấu hình: access token 15 phút, refresh token 7 ngày, rotation mỗi lần refresh

| ID | Kịch bản | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-RT01 | Refresh hợp lệ | Gửi refresh token | 200, cặp token mới, refresh token khác cái cũ | Đúng | ✅ |
| TC-RT02 | Access token mới dùng được | Gọi `/api/users/me` | 200 | Đúng | ✅ |
| TC-RT03 | Không dùng lại token cũ | Refresh 2 lần cùng token | Lần 2: 401 "Phiên đăng nhập đã hết hạn..." | Đúng | ✅ |
| TC-RT04 | Phát hiện token bị đánh cắp | Dùng lại token đã rotate | Thu hồi **toàn bộ** phiên, token mới nhất cũng 401 | Đúng | ✅ |
| TC-RT05 | Token bịa | `khong-ton-tai` | 401 | Đúng | ✅ |
| TC-RT06 | Token trống | `""` | 400 `errors.refreshToken` | Đúng | ✅ |
| TC-RT07 | Token hết hạn | `expires_at` trong quá khứ | 401 | Đúng | ✅ |
| TC-RT08 | User bị khoá | SUSPENDED rồi refresh | 403 | Đúng | ✅ |
| TC-RT09 | Logout thu hồi token | Logout rồi refresh | 204 → 401 | Đúng | ✅ |
| TC-RT10 | Logout token không tồn tại | | 204 (idempotent) | Đúng | ✅ |
| TC-RT11 | Chỉ lưu hash | Đọc DB | `token_hash` là SHA-256 (64 hex), khác token gốc | Đúng | ✅ |
| TC-RT12 | Refresh token không dùng làm access token | `Bearer <refreshToken>` | 401 | Đúng | ✅ |
| TC-RT13 | UI: auto refresh | Làm hỏng access token trong storage rồi lưu hồ sơ | Tự gọi refresh, retry thành công, token trong storage được thay mới | Đúng | ✅ |
