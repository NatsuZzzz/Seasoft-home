# TC — Thống kê tổng quan (`/api/admin/stats`, tab "Tổng quan" trong `admin.html`)

- Ngày test: 2026-09-28 · Test tự động: `AdminStatsApiTest` (11/11 pass, tính theo chênh lệch trước/sau nên không phụ thuộc dữ liệu có sẵn) · Test UI: ADMIN dev
- Biểu đồ: 1 chuỗi, màu `#0096C7` (đã chạy validator của skill dataviz: pass lightness/chroma/contrast), cột ≤ 24px bo 4px ở đầu, nhãn chọn lọc, tooltip hover + focus bàn phím, bảng dữ liệu thay thế

| ID | Kịch bản | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-ST01 | Phân quyền | STAFF / khách / không token / MANAGER | 403 / 403 / 401 / 200 | Đúng | ✅ |
| TC-ST02 | Đủ khoá kể cả = 0 | Gọi stats | `leadsByStatus` 5 khoá, `projectsByStatus` 5 khoá, `usersByRole` 4 khoá | Đúng | ✅ |
| TC-ST03 | Tổng lead & 30 ngày | Tạo 2 lead | `leadsTotal`, `leadsLast30Days`, `NEW` đều +2 | Đúng | ✅ |
| TC-ST04 | Lead theo trạng thái | 1 WON, 1 LOST | WON +1, LOST +1, NEW không đổi | Đúng | ✅ |
| TC-ST05 | Tỉ lệ chốt | 3 WON, 1 LOST | `conversionRate` = round(WON·1000/(WON+LOST))/10 | Đúng | ✅ |
| TC-ST06 | Lead theo tuần | Gọi stats | Đúng 8 tuần, tuần cuối = thứ Hai tuần này, có lead vừa tạo | Đúng | ✅ |
| TC-ST07 | Dự án theo trạng thái | Tạo 1 dự án | `projectsTotal` +1, PLANNING +1 | Đúng | ✅ |
| TC-ST08 | Tiến độ trung bình | 1/5 giai đoạn DONE | `avgProgress` trong [0, 100] | Đúng | ✅ |
| TC-ST09 | Dự án trễ hạn | Hạn 2020-01-01 → COMPLETED | +1 rồi về như cũ | Đúng | ✅ |
| TC-ST10 | Người dùng theo vai trò | +1 STAFF, +1 khách | STAFF +1, CUSTOMER +1 | Đúng | ✅ |
| TC-ST11 | Lead theo dịch vụ | +1 lead LANDING_PAGE | LANDING_PAGE +1 | Đúng | ✅ |
| TC-ST12 | UI: thẻ số liệu | Mở tab Tổng quan | 5 thẻ: Lead 30 ngày 1, Tỉ lệ chốt 100%, Dự án đang chạy 1, Tiến độ TB 40%, Trễ hạn 0 | Đúng (khớp dữ liệu demo) | ✅ |
| TC-ST13 | UI: biểu đồ cột theo tuần | Nhìn + hover cột cuối | 8 cột, chỉ ghi số ở cột cao nhất/tuần hiện tại; tooltip "Tuần 28-09: 1 lead" | Đúng | ✅ |
| TC-ST14 | UI: truy cập bằng bàn phím | Focus 1 thanh ngang | Tooltip "Thành công: 1 lead" hiện | Đúng | ✅ |
| TC-ST15 | UI: bảng dữ liệu | Mở "Xem dạng bảng" | Bảng Nhóm/Lead đủ 5 trạng thái | Đúng | ✅ |
| TC-ST16 | UI: giá trị 0 | Trạng thái có 0 lead | Không vẽ vạch (bug vạch 2px đã fix) | Đúng | ✅ |
| TC-ST17 | UI: STAFF | Nhân viên mở `admin.html` | Không có tab Tổng quan / Người dùng | Đúng | ✅ |
