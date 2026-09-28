# TC — Quản lý dự án cho nhân viên (`/api/staff/projects`, tab "Dự án" trong `admin.html`)

- Ngày test: 2026-09-28 · Test tự động: `StaffProjectApiTest` (14/14 pass) · Test UI: ADMIN dev + dữ liệu demo
- Quyền: STAFF xem tất cả, chỉ sửa dự án mình phụ trách, chỉ tạo dự án từ lead mình phụ trách · MANAGER/ADMIN làm được hết

| ID | Kịch bản | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-P01 | Tạo từ lead đã chốt | MANAGER tạo từ lead WON | 201, mã `SS-YYYY-NNNN`, PLANNING, 0%, 5 giai đoạn mặc định, dịch vụ lấy từ lead, mail "đã được khởi tạo" cho khách | Đúng | ✅ |
| TC-P02 | Lead chưa chốt | Lead NEW | 409 "Chỉ tạo dự án từ lead đã chốt (Thành công)" | Đúng | ✅ |
| TC-P03 | Tạo 2 lần từ 1 lead | | Lần 2: 409 "Lead này đã có dự án" | Đúng | ✅ |
| TC-P04 | Lead của khách vãng lai | Email đã đăng ký / chưa đăng ký | Tự khớp tài khoản theo email / 400 "…chưa có tài khoản…" | Đúng | ✅ |
| TC-P05 | STAFF tạo dự án | Lead chưa giao cho mình → được giao | 403 → 201, người phụ trách = staff | Đúng | ✅ |
| TC-P06 | Tạo theo email khách (không lead) | Thiếu dịch vụ; STAFF; MANAGER + 2 giai đoạn tuỳ chỉnh | 400; 403; 201 với 2 giai đoạn | Đúng | ✅ |
| TC-P07 | Validate | Hạn trước ngày bắt đầu; tên trống | 400 "Hạn hoàn thành phải sau ngày bắt đầu"; 400 `errors.name` | Đúng | ✅ |
| TC-P08 | Hoàn thành giai đoạn | Giai đoạn 1 → DONE → TODO | Có `completedAt`, tiến độ 20%, dự án tự chuyển IN_PROGRESS; về TODO thì xoá `completedAt` | Đúng | ✅ |
| TC-P09 | Thêm / xoá giai đoạn | Thêm "Đào tạo sử dụng" rồi xoá; thêm tên trống | `sortOrder` = 6, xoá xong còn 5; tên trống 400 | Đúng | ✅ |
| TC-P10 | Quyền sửa | STAFF khác sửa / xem; MANAGER sửa | 403 / 200 xem được / 200 | Đúng | ✅ |
| TC-P11 | Cập nhật nội bộ vs công khai | Đăng 1 nội bộ, 1 công khai | Chỉ bản công khai gửi mail; staff thấy 2, khách thấy 1 | Đúng | ✅ |
| TC-P12 | Tìm kiếm / lọc | Theo mã; mã + COMPLETED; `mine=true` | 1 / 0 / đúng người phụ trách | Đúng | ✅ |
| TC-P13 | Khách gọi API staff | GET, PATCH | 403 | Đúng | ✅ |
| TC-P14 | Đổi người phụ trách | ADMIN đổi; STAFF (đang phụ trách) đổi cho người khác; STAFF đổi trạng thái | 200; 403; 200 | Đúng | ✅ |
| TC-P15 | UI: từ lead sang dự án | Mở lead WON → "Tạo dự án từ lead này" | Chuyển tab Dự án, chọn sẵn lead, gợi ý tên "Website bán hàng Demo Corp", tạo ra SS-2026-0001 + 5 giai đoạn, toast | Đúng | ✅ |
| TC-P16 | UI: cập nhật giai đoạn | Đổi select 2 giai đoạn DONE, 1 đang làm | Tiến độ 40%, trạng thái "Đang thực hiện", bảng cập nhật | Đúng | ✅ |
| TC-P17 | UI: đăng cập nhật | 1 công khai + 1 bỏ tick "Khách xem được" | Bản bỏ tick có nhãn "Nội bộ"; log chỉ có 1 mail "Cập nhật dự án" | Đúng | ✅ |
