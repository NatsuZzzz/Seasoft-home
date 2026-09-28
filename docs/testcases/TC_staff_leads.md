# TC — Quản lý lead cho nhân viên (`/api/staff/consultations`, tab "Yêu cầu tư vấn" trong `admin.html`)

- Ngày test: 2026-09-28 · Test tự động: `StaffConsultationApiTest` (14/14 pass) · Test UI: đăng nhập ADMIN dev (`ADMIN_EMAIL` trong `seasoft/.env`)
- Quyền: STAFF xem tất cả, chỉ sửa lead mình phụ trách, chỉ tự nhận lead chưa ai nhận · MANAGER/ADMIN làm được hết
- Luồng trạng thái: NEW → CONTACTED/LOST · CONTACTED → QUOTED/LOST · QUOTED → WON/LOST/CONTACTED · LOST → CONTACTED · WON là trạng thái cuối

| ID | Kịch bản | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-S01 | Phân quyền API | Khách gọi / không token | 403 / 401 | Đúng | ✅ |
| TC-S02 | Danh sách + phân trang | STAFF, `size=1` | `items` 1 phần tử, có `totalItems`, `totalPages` | Đúng | ✅ |
| TC-S03 | Lọc + tìm kiếm | `status=CONTACTED&q=trần thị`; status sai | Chỉ lead CONTACTED khớp tên; status sai → 400 | Đúng | ✅ |
| TC-S04 | STAFF sửa lead chưa nhận | PATCH status | 403 "Bạn chỉ được cập nhật lead do mình phụ trách" | Đúng | ✅ |
| TC-S05 | STAFF tự nhận rồi cập nhật | assign chính mình → CONTACTED; `mine=true` | 200; lọc "của tôi" thấy lead | Đúng | ✅ |
| TC-S06 | Chuyển trạng thái sai luồng | NEW → WON | 409 "Không thể chuyển trạng thái từ NEW sang WON" | Đúng | ✅ |
| TC-S07 | Luồng đầy đủ | NEW→CONTACTED→QUOTED→WON rồi →LOST; lead khác LOST→CONTACTED | WON khoá (409); LOST mở lại được | Đúng | ✅ |
| TC-S08 | Giành lead của người khác | STAFF B nhận/sửa lead của A | 409 / 403 | Đúng | ✅ |
| TC-S09 | STAFF gán cho người khác | assign staffId khác | 403 | Đúng | ✅ |
| TC-S10 | MANAGER phân công / bỏ phân công | assign staff → null | 200, `assignedStaff` đổi đúng | Đúng | ✅ |
| TC-S11 | Gán cho khách hoặc nhân viên bị khoá | | 400 | Đúng | ✅ |
| TC-S12 | ID không tồn tại / sai định dạng | | 404 / 400 | Đúng | ✅ |
| TC-S13 | Ghi chú nội bộ | PATCH chỉ `internalNote` | Lưu ghi chú, trạng thái giữ nguyên | Đúng | ✅ |
| TC-S14 | Danh sách người phụ trách | `/api/staff/assignees` | Chỉ nhân viên ACTIVE, không có khách | Đúng | ✅ |
| TC-S15 | UI: ADMIN mở trang quản trị | Login admin → `admin.html` | Sidebar + tab "Yêu cầu tư vấn", bảng có lead vừa gửi | Đúng | ✅ |
| TC-S16 | UI: drawer chi tiết | Click hàng | Drawer hiện đủ thông tin; select trạng thái chỉ có Mới / Đã liên hệ / Thất bại; HTML khách nhập bị escape (chống XSS) | Đúng | ✅ |
| TC-S17 | UI: lưu thay đổi | Phân công + CONTACTED + ghi chú → Lưu | Toast "Đã lưu thay đổi", drawer đóng, hàng cập nhật | Đúng | ✅ |
| TC-S18 | UI: bộ lọc | Trạng thái / Của tôi / Chưa phân công / tìm "sóng xanh" | Số hàng đúng từng trường hợp (0/1), tìm có dấu chạy đúng | Đúng | ✅ |
| TC-S19 | UI: khách vào `admin.html` | Login khách | Màn "Khu vực dành cho nhân viên", link Quản trị ẩn trên header | Đúng | ✅ |
