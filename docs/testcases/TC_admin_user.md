# TC — Quản lý người dùng (`/api/admin/users`, tab "Người dùng" trong `admin.html`)

- Ngày test: 2026-09-28 · Test tự động: `AdminUserApiTest` (13/13 pass) · Test UI: ADMIN dev + nhân viên demo `nv.demo@seasoft.test`
- Quy tắc: chỉ MANAGER/ADMIN vào được · MANAGER chỉ tạo/sửa/khoá STAFF và khách · chỉ ADMIN đổi vai trò · không tự khoá / tự đổi vai trò · nhân viên mới nhận **email mời đặt mật khẩu** (48 giờ), admin không biết mật khẩu

| ID | Kịch bản | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-AU01 | Phân quyền | STAFF / khách / MANAGER gọi danh sách | 403 / 403 / 200 | Đúng | ✅ |
| TC-AU02 | Lọc danh sách | `role=STAFF&q=email`; `status=ACTIVE&role=CUSTOMER` | Đúng 1 kết quả, không lộ `passwordHash`; toàn CUSTOMER | Đúng | ✅ |
| TC-AU03 | Tạo nhân viên → nhận lời mời → đăng nhập | ADMIN tạo STAFF, lấy token từ mail, đặt mật khẩu | 201 ACTIVE + hồ sơ nhân sự; mail "Mời bạn tham gia… 48 giờ"; login role STAFF | Đúng | ✅ |
| TC-AU04 | MANAGER tạo tài khoản | MANAGER / ADMIN / STAFF | 403 / 403 / 201 | Đúng | ✅ |
| TC-AU05 | Xung đột khi tạo | Email trùng; mã NV trùng; role CUSTOMER | 409; 409 "Mã nhân viên đã tồn tại"; 400 | Đúng | ✅ |
| TC-AU06 | Khoá tài khoản | Khoá → login, token cũ, refresh; mở khoá → login | 403; 401; 401; mở khoá login được | Đúng | ✅ |
| TC-AU07 | Tự thao tác lên mình | Tự khoá / tự đổi vai trò | 400 | Đúng | ✅ |
| TC-AU08 | MANAGER đụng ADMIN/MANAGER | Khoá ADMIN, khoá MANAGER khác, khoá STAFF | 403, 403, 200 | Đúng | ✅ |
| TC-AU09 | Đổi vai trò | MANAGER đổi; role "HACKER"; ADMIN đổi khách → `staff` (chữ thường) | 403; 400; 200 STAFF, access token cũ dùng được quyền mới ngay, refresh token cũ bị thu hồi | Đúng | ✅ |
| TC-AU10 | Sửa hồ sơ nhân sự | Tên, phòng ban, chức vụ, ngày vào làm; thêm phòng ban cho khách | 200 cập nhật đúng; khách → 400 | Đúng | ✅ |
| TC-AU11 | Trùng mã NV khi sửa | Gán mã của người khác | 409 | Đúng | ✅ |
| TC-AU12 | Gửi link đặt lại mật khẩu | ADMIN bấm gửi | Mail có token | Đúng | ✅ |
| TC-AU13 | Lỗi tham số | ID không tồn tại; `status=KHONG_CO`; body trạng thái rỗng | 404; 400; 400 `errors.status` | Đúng | ✅ |
| TC-AU14 | UI: thêm nhân viên | Điền form (mã NV-001, Sales) | Toast "Đã tạo tài khoản và gửi lời mời…", drawer chi tiết mở ra, badge "Nhân viên · Hoạt động" | Đúng | ✅ |
| TC-AU15 | UI: khoá / mở khoá / đổi vai trò | Bấm lần lượt | "Đã khoá" + nút đổi thành "Mở khoá tài khoản" → "Hoạt động" → "Quản lý" | Đúng | ✅ |
| TC-AU16 | UI: nhận lời mời thật | Lấy link từ log `[DEV MAIL]`, đặt mật khẩu, đăng nhập | Đăng nhập thành công với vai trò được giao | Đúng | ✅ |
| TC-AU17 | UI: MANAGER xem tài khoản ADMIN | Mở drawer ADMIN; mở form thêm | Không có nút khoá/đổi vai trò, có cảnh báo; chỉ chọn được "Nhân viên"; gọi thẳng API khoá ADMIN → 403 | Đúng | ✅ |
| TC-AU18 | UI: vai trò bị đổi khi đang đăng nhập | ADMIN hạ MANAGER → STAFF, người đó mở lại `admin.html` | Giao diện tự đồng bộ vai trò từ server, chỉ còn tab Leads + Dự án (bug đã fix) | Đúng | ✅ |
