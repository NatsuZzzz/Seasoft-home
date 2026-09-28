# TC — Hardening bảo mật (Phase 8)

- Ngày test: 2026-09-28 · Test tự động: `SecurityHardeningTest` (13/13) + toàn bộ 174 test · Kiểm tra thêm bằng curl trên server thật
- Tự review toàn bộ code: auth, phân quyền, input, output (XSS), header, cấu hình prod

| ID | Kịch bản | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-SEC01 | Security headers | GET bất kỳ trang/API | `Content-Security-Policy` (chỉ `'self'`, `frame-ancestors 'none'`), `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy` | Đúng (test + curl) | ✅ |
| TC-SEC02 | CSP không chặn nhầm | Mở 8 trang trong browser | 0 vi phạm CSP trong console; GSAP, Lenis, font, ảnh portfolio vẫn chạy | Đúng | ✅ |
| TC-SEC03 | Chống dò mật khẩu theo email | 10 lần sai rồi lần 11 | 10 × 401 → 429 "Bạn thao tác quá nhiều lần…"; email khác không bị ảnh hưởng | Đúng (test + curl server thật) | ✅ |
| TC-SEC04 | Chống spam mail quên mật khẩu | 4 lần cùng email | 3 × 200 → 429 | Đúng | ✅ |
| TC-SEC05 | Giới hạn theo IP | Unit test `RateLimiter` với đồng hồ giả | Hết lượt → chặn; IP khác vẫn được; qua cửa sổ 10 phút → được lại | Đúng | ✅ |
| TC-SEC06 | Chính sách mật khẩu | Đăng ký 7 ký tự / 8 ký tự | 400 "Mật khẩu từ 8 đến 100 ký tự" / 200 (FE + BE thống nhất 8) | Đúng | ✅ |
| TC-SEC07 | Chặn DoS qua BCrypt | Login mật khẩu 101 ký tự | 400 trước khi băm | Đúng | ✅ |
| TC-SEC08 | Actuator | `/actuator/health`; `/actuator/env` | 200 `UP` không lộ chi tiết; env bị chặn (401) | Đúng | ✅ |
| TC-SEC09 | JWT secret yếu | Secret 5 byte / không phải base64 | App từ chối khởi động (`IllegalStateException`) | Đúng | ✅ |
| TC-SEC10 | SSR không bị XSS | Bài có `<script>` trong nội dung | HTML server trả `&lt;script&gt;`, không có thẻ script thật | Đúng | ✅ |
| TC-SEC11 | JSON-LD không thoát khỏi thẻ script | Tiêu đề chứa `</script><script>alert(1)` | Được mã hoá `</script>` | Đúng | ✅ |
| TC-SEC12 | Dọn token cũ | Chèn refresh/reset token hết hạn 3 ngày, chạy job | Xoá token cũ, giữ token còn hạn | Đúng | ✅ |
| TC-SEC13 | Cấu hình prod | Đọc `application-prod.properties` | Bắt buộc `CORS_ORIGINS`, `FRONTEND_URL`; ẩn stacktrace/message lỗi; `forward-headers-strategy=native` (IP thật cho rate limit); nén response | Đúng (review) | ✅ |
| TC-SEC14 | Không còn phụ thuộc CDN ngoài | grep `jsdelivr`, `googleapis`, `gstatic` trong Html/ | 0 kết quả (GSAP, Lenis, font tự host qua `npm run vendor`) | Đúng | ✅ |
