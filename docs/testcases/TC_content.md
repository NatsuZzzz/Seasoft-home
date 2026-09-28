# TC — Nội dung marketing: dự án tiêu biểu, blog, đánh giá (`/api/public/**`, `/api/admin/content/**`, tab "Nội dung", `blog.html`, `post.html`)

- Ngày test: 2026-09-28 · Test tự động: `PublicContentApiTest` (10/10) + `AdminContentApiTest` (12/12) · Test UI: ADMIN dev
- Quy tắc: chỉ MANAGER/ADMIN quản lý · public chỉ thấy nội dung đã đăng · link chỉ nhận `http(s)` · nội dung luôn escape trước khi hiển thị

| ID | Kịch bản | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-CT01 | Seed dự án tiêu biểu | GET `/api/public/portfolio` | 3 dự án cũ của trang chủ, đúng thứ tự | Đúng | ✅ |
| TC-CT02 | Dự án ẩn | Tạo `published=false` | Không xuất hiện ở public | Đúng | ✅ |
| TC-CT03 | Blog chỉ bài đã đăng | 1 nháp + 1 đăng | Public chỉ có bài đăng, danh sách không kèm `content` | Đúng | ✅ |
| TC-CT04 | Chi tiết bài | Slug bài đăng / bài nháp / slug lạ | 200 có nội dung / 404 / 404 | Đúng | ✅ |
| TC-CT05 | Đánh giá chỉ bản đã duyệt | 1 hiển thị + 1 ẩn | Public chỉ có bản hiển thị | Đúng | ✅ |
| TC-CT06 | Public chỉ đọc | POST `/api/public/...`; GET admin không token | 401 / 401 | Đúng | ✅ |
| TC-CT07 | Phân trang blog | `size=1` | 1 bài, `totalPages ≥ 2` | Đúng | ✅ |
| TC-CT08 | Phân quyền quản lý | STAFF / khách / MANAGER | 403 / 403 / 200 | Đúng | ✅ |
| TC-CT09 | Slug tự tạo tiếng Việt | "Thiết kế Website Đẹp & Nhanh!" | `thiet-ke-website-dep-nhanh` | Đúng | ✅ |
| TC-CT10 | Trùng slug | Tiêu đề trùng; slug nhập tay trùng | Tự thêm `-2`; 409 | Đúng | ✅ |
| TC-CT11 | Link nguy hiểm / slug sai | `javascript:alert(1)`, `data:image…`, slug có dấu | 400 kèm lỗi từng field | Đúng | ✅ |
| TC-CT12 | Sửa + đăng dự án | PUT tiêu đề mới + `published` | Slug tự cập nhật, xuất hiện ở public | Đúng | ✅ |
| TC-CT13 | Xoá dự án | DELETE 2 lần | 204 rồi 404 | Đúng | ✅ |
| TC-CT14 | Vòng đời bài viết | Nháp → Đăng → Về nháp | `publishedAt` null → có giá trị → giữ nguyên ngày; public 404 khi về nháp | Đúng | ✅ |
| TC-CT15 | Tác giả | MANAGER viết bài | `authorName` = người tạo | Đúng | ✅ |
| TC-CT16 | Validate bài viết / đánh giá | Thiếu nội dung, tiêu đề trống; sao 0 hoặc 6 | 400; không nhập sao → mặc định 5 | Đúng | ✅ |
| TC-CT17 | UI: viết bài qua tab Nội dung | Điền form có `##`, `-`, và `<script>` / `<img onerror>` | Lưu thành công, bài ở trạng thái "Đã đăng" | Đúng | ✅ |
| TC-CT18 | UI: `blog.html` → `post.html` | Mở thẻ bài | 2 tiêu đề mục, 3 gạch đầu dòng, thẻ #SEO #Website; script/img hiện dạng chữ, 0 thẻ bị chèn | Đúng | ✅ |
| TC-CT19 | UI: trang chủ lấy dự án từ API | Mở `Page.html`, cuộn tới Dự án | 3 thẻ render từ API, animate vào mượt, có nghiêng 3D | Đúng | ✅ |
| TC-CT20 | UI: section đánh giá | Không có dữ liệu → đăng 1 bản test → xoá | Ẩn → hiện (sao có nhãn "4 trên 5 sao", HTML bị escape) → đã xoá bản test | Đúng | ✅ |
