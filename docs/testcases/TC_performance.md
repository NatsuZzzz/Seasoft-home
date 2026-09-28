# TC — Hiệu năng & khả năng tiếp cận (Lighthouse 13, Phase 8)

- Ngày đo: 2026-09-28 · Chrome headless, cấu hình mặc định **mobile** (giả lập 4G chậm, CPU chậm 4×) trừ khi ghi "desktop"
- Server: jar local (http://localhost:8080) — số thật trên hosting sẽ phụ thuộc mạng/server

## Trước / sau tối ưu

| Trang | Performance trước | Performance sau | LCP trước → sau | Accessibility | Best Practices | SEO |
|---|---|---|---|---|---|---|
| Trang chủ (mobile) | 56 | **82–83** | 10.8 s → 3.8–4.0 s | 93 → **96** | 100 | 100 |
| Trang chủ (desktop) | — | **98** | → 1.0 s | 93 | 100 | 100 |
| Đăng nhập | 57 | **95** | 8.7 s → 2.7 s | 90 → **95** | 100 | 100 |
| Đăng ký | — | **95** | → 2.7 s | **95** | 100 | 100 |
| Blog | 55 | **95** | 8.7 s → 2.6 s | 94 → **100** | 100 | 100 |
| Bài viết | 55 | **95** | 8.8 s → 2.6 s | **94** | 100 | 100 |

## Testcase

| ID | Kịch bản | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|
| TC-PF01 | Font icon chỉ gồm icon đang dùng | < 20 KB (trước 1.1 MB) | 63 icon = 7.7 KB | ✅ |
| TC-PF02 | Không tải tài nguyên bên thứ ba khi render | 0 request tới CDN/Google Fonts | 0 (tự host) | ✅ |
| TC-PF03 | Tổng dung lượng trang chủ | Giảm rõ rệt | 1,682 KB → 666 KB | ✅ |
| TC-PF04 | Headline hero không chờ JS | Chữ hiện bằng CSS animation | `.hero-title` tách chữ sẵn, opacity 1 sau animation, không cần GSAP | ✅ |
| TC-PF05 | Phần tử LCP hiện ngay | Không bắt đầu từ opacity 0 | Đoạn mô tả hero chỉ trượt (`hero-in-soft`), render delay ~0.3 s | ✅ |
| TC-PF06 | Performance ≥ 90 các trang phụ | login, register, blog, post | 95 cả 4 trang | ✅ |
| TC-PF07 | Trang chủ desktop ≥ 90 | | 98 | ✅ |
| TC-PF08 | Trang chủ mobile ≥ 90 | | **82–83 — chưa đạt** (xem nợ kỹ thuật) | ⚠️ |
| TC-PF09 | CLS | < 0.1 | 0 – 0.09 mọi trang | ✅ |
| TC-PF10 | Tương phản nút chính | ≥ 4.5:1 | Chữ navy trên cyan 9.17:1 (trước: trắng trên cyan 1.94:1) | ✅ |
| TC-PF11 | Cấu trúc heading & landmark | Không nhảy cấp, có `<main>` | h4 → h3 (13 chỗ), thêm `<main id="main">` + link "Bỏ qua tới nội dung chính" | ✅ |
| TC-PF12 | Vùng bấm nút ẩn/hiện mật khẩu | ≥ 24 px | Thêm padding → ~30 px | ✅ |
| TC-PF13 | Hiệu ứng vẫn chạy sau tối ưu | Hero, nút hút chuột, reveal khi cuộn | Đúng (CTA `transform` trả lại cho GSAP nhờ `animation-fill-mode: backwards`) | ✅ |

## Nợ kỹ thuật
- Trang chủ mobile 82–83: còn 4 file CSS chặn render + HTML 73 KB. Hướng xử lý: gom CSS thành 1 file, inline critical CSS, tối ưu ảnh hero (WebP/`srcset`).
- Chữ cyan `text-primary` trên nền trắng (1.94:1) vẫn còn ở logo "Soft" (logo được miễn WCAG) và một số nhãn nhỏ — cần chủ dự án quyết định có đổi sang tông đậm hơn (#0077B6, 4.87:1) không.
