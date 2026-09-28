# TC — Form đăng ký tư vấn (`POST /api/consultations`, `/mine`, form trên `Page.html`)

- Ngày test: 2026-09-28 · Test tự động: `ConsultationApiTest` (14/14 pass) · Test UI: browser thật trên http://localhost:8080
- Chống spam: honeypot `website` + giới hạn 5 lần / 10 phút / IP

| ID | Kịch bản | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-C01 | Khách vãng lai gửi form | Điền đủ, gửi | 201, có `id`; DB: status `NEW`, lưu IP, `customer_id` null | Đúng | ✅ |
| TC-C02 | Khách đã đăng nhập gửi | Gửi kèm token | Gắn `customer_id`; `/mine` trả về đúng 1 yêu cầu | Đúng | ✅ |
| TC-C03 | Bỏ trống | `{}` | 400, lỗi `fullName`, `email`, `phone`, `serviceType` | Đúng | ✅ |
| TC-C04 | Email sai | `khong-phai-email` | 400 `errors.email` | Đúng | ✅ |
| TC-C05 | SĐT sai | `abc` | 400 `errors.phone` | Đúng | ✅ |
| TC-C06 | Dịch vụ không tồn tại | `serviceType=HACK` | 400 | Đúng | ✅ |
| TC-C07 | Không chọn ngân sách | Bỏ `budgetRange` | Lưu `UNDECIDED` | Đúng | ✅ |
| TC-C08 | Bot điền honeypot | `website=http://spam.bot` | 201 giả (không có `id`), **không** lưu DB | Đúng | ✅ |
| TC-C09 | Spam từ 1 IP | 6 lần liên tiếp cùng IP | Lần 6: 429; IP khác vẫn gửi được | Đúng | ✅ |
| TC-C10 | Email viết HOA + khoảng trắng | `"  EMAIL@X "` | Lưu chữ thường, đã trim | Đúng (bug trim với record → fix bằng compact constructor) | ✅ |
| TC-C11 | Mail xác nhận | Gửi form | Gửi mail "SeaSoft đã nhận yêu cầu tư vấn của bạn" tới khách | Đúng (unit + log `[DEV MAIL]` khi test UI) | ✅ |
| TC-C12 | `/mine` bảo mật | Không token; 2 khách khác nhau | 401; mỗi khách chỉ thấy yêu cầu của mình | Đúng | ✅ |
| TC-C13 | Nội dung quá dài | 2001 ký tự | 400 `errors.message` | Đúng | ✅ |
| TC-C14 | Khách không thấy dữ liệu nội bộ | Có ghi chú nội bộ | `/mine` không trả `internalNote`, `ipAddress`, `phone` | Đúng | ✅ |
| TC-C15 | UI: validate phía client | Bấm gửi khi trống | Hiện 4 lỗi dưới ô, form rung, focus ô đầu tiên lỗi | Đúng | ✅ |
| TC-C16 | UI: gửi thành công | Điền đủ, gửi | Form ẩn, hiện dấu tick vẽ + "Gửi thành công!"; nút "Gửi yêu cầu khác" giữ tên/SĐT/email | Đúng | ✅ |
| TC-C17 | UI: tự điền khi đã đăng nhập | Khách login rồi vào `#contact` | Tự điền họ tên, email, SĐT từ `/api/users/me` | Đúng | ✅ |
| TC-C18 | UI: hồ sơ khách | Vào `profile.html` | Mục "Yêu cầu tư vấn của tôi" hiện dịch vụ, ngân sách, thời gian, badge trạng thái | Đúng | ✅ |
