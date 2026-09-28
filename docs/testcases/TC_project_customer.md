# TC — Cổng khách hàng: theo dõi dự án (`/api/projects`, `dashboard.html`, `project.html`)

- Ngày test: 2026-09-28 · Test tự động: `CustomerProjectApiTest` (10/10 pass) · Test UI: tài khoản khách demo (ghi trong `seasoft/.env`)
- Nguyên tắc: khách chỉ thấy dự án của mình; dự án người khác trả **404** (không tiết lộ là có tồn tại) → chống IDOR

| ID | Kịch bản | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-CP01 | Danh sách dự án của tôi | GET `/api/projects` | Đúng 1 dự án của mình, có `progress` | Đúng | ✅ |
| TC-CP02 | Chi tiết | GET `/api/projects/{id}` | 5 giai đoạn, tên người phụ trách | Đúng | ✅ |
| TC-CP03 | Xem dự án người khác (IDOR) | Khách B mở id của A | 404 "Không tìm thấy dự án"; danh sách của B chỉ có dự án B | Đúng (API + thử lại trên browser) | ✅ |
| TC-CP04 | ID không tồn tại / sai định dạng | | 404 / 400 | Đúng | ✅ |
| TC-CP05 | Bình luận | POST comment | 201, `authorRole=CUSTOMER`; staff thấy; người phụ trách nhận mail | Đúng | ✅ |
| TC-CP06 | Bình luận không hợp lệ | Toàn khoảng trắng; > 2000 ký tự | 400 | Đúng | ✅ |
| TC-CP07 | Bình luận vào dự án người khác | | 404 | Đúng | ✅ |
| TC-CP08 | Chưa đăng nhập | GET `/api/projects` | 401 | Đúng | ✅ |
| TC-CP09 | Ẩn dữ liệu nội bộ | Có cập nhật nội bộ | Khách không thấy cập nhật nội bộ, không có `consultationId`, `customerEmail` | Đúng | ✅ |
| TC-CP10 | Tiến độ | 2/5 giai đoạn DONE | `progress` = 40, trạng thái IN_PROGRESS | Đúng | ✅ |
| TC-CP11 | UI: dashboard | Khách demo vào `dashboard.html` | Thẻ dự án: mã, tên, badge "Đang thực hiện", thanh tiến độ chạy tới 40%, hạn 15/12/2026 | Đúng | ✅ |
| TC-CP12 | UI: trang chi tiết | Bấm "Xem chi tiết" | Vòng tròn 40% (dashoffset 196.2/327), timeline: 2 hoàn thành, 1 đang làm (nháy), 2 chờ | Đúng | ✅ |
| TC-CP13 | UI: gửi bình luận có HTML | Gõ `<script>alert(1)</script>` | Hiện dạng chữ, không chạy script; "Đã gửi ✓", danh sách cập nhật ngay | Đúng | ✅ |
| TC-CP14 | UI: chưa đăng nhập vào `dashboard.html` | | Chuyển sang `login.html?next=dashboard.html` | Theo `Auth.requireAuth()` (đã test ở Phase 2) | ✅ |
