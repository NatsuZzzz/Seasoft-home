# TC — Giao diện động & hiệu ứng cuộn (Phase 3)

- Ngày test: 2026-09-28
- Môi trường: browser pane (Chromium) qua http://localhost:8080, viewport 1440×900 (desktop) và 375×812 (mobile)
- Thư viện: GSAP 3.15.0 + ScrollTrigger, Lenis 1.3.26 (jsDelivr)
- Cập nhật 2026-09-28: người dùng báo **chuyển trang bị chớp** → thay màn che JS bằng View Transitions API + build Tailwind ra CSS tĩnh (TC-A10, A11, A18, A19)
- Ghi chú: pane test bị ẩn nên trình duyệt không chạy `requestAnimationFrame`; các case animation được kiểm bằng cách "bơm" frame thủ công cho GSAP (`gsap.ticker.tick()`) rồi đọc style thật trên DOM + chụp màn hình

| ID | Chức năng | Bước | Kết quả mong đợi | Thực tế | Trạng thái |
|---|---|---|---|---|---|
| TC-A01 | Khởi tạo | Mở `Page.html` desktop | `<html>` có `js-anim lenis anim-ready`, ~28 ScrollTrigger, không lỗi console | Đúng | ✅ |
| TC-A02 | Hero tách chữ | Chờ load | Headline tách 8 từ, hiện lần lượt từ dưới lên; `aria-label` giữ câu đầy đủ | Đúng | ✅ |
| TC-A03 | Số chạy | Chờ load | 3 số đếm từ 0 → `50+`, `5+`, `100%` | Đúng | ✅ |
| TC-A04 | Reveal khi cuộn | Cuộn hết trang | Mọi phần tử `data-reveal` / `data-stagger` đều hiện (0 phần tử kẹt opacity 0) | 0 phần tử kẹt | ✅ |
| TC-A05 | Quy trình pin + scrub (desktop) | Cuộn qua `#process` | Section đứng yên (top 157px), thanh nối chạy 0 → 29% → 59% → 99%, số bước sáng 1 → 2 → 4 → 5 | Đúng | ✅ |
| TC-A06 | Chuyển cảnh "Tại sao chọn SeaSoft" | Cuộn tới `#why-us` | Section navy từ thẻ bo góc `inset(5.7% 4.8% round 34px)` nở dần ra full màn hình | Đúng | ✅ |
| TC-A07 | Header co lại + progress bar | Cuộn > 40px | Header cao 80 → 64px, nền mờ + bóng; thanh progress trên cùng chạy theo % trang | Đúng (94% ở gần cuối trang) | ✅ |
| TC-A08 | Scrollspy menu | Cuộn qua từng section rồi về đầu | Menu sáng đúng mục: Dịch vụ → Quy trình → Dự án → Về chúng tôi → Liên hệ; về hero thì tắt hết | Đúng (bug về hero không tắt đã fix) | ✅ |
| TC-A09 | Click menu cuộn mượt | Click "Dự án", "Liên hệ", "Dịch vụ", "Quy trình" | Cuộn tới section, cách header 64px; hash URL cập nhật | Đúng (bug Lenis dùng chiều cao cũ, không tới được "Liên hệ" → đã fix) | ✅ |
| TC-A10 | Chuyển trang (View Transitions API) | Click "Đăng nhập" trên trang chủ | Trình duyệt tạo view transition (`pageswap.viewTransition` có giá trị); trang mới kéo rèm từ dưới lên trên nền navy, trang cũ lùi lại; **không nháy trắng** | `hasVT: true`, Chrome 152 | ✅ |
| TC-A11 | Không nháy style (FOUC) | Mở từng trang | Không còn Tailwind CDN (`window.tailwind` undefined), CSS tĩnh 37KB load trong `<head>` nên style có ngay từ khung đầu | Đúng, 6/6 trang | ✅ |
| TC-A12 | Rung form login khi lỗi | Nhập email sai định dạng rồi submit | Form có class `shake`, `animation-name: shake` | Đúng | ✅ |
| TC-A13 | Mobile 375px | Mở trang, cuộn hết | Không ghim Quy trình, không parallax, không tràn ngang, mọi phần tử hiện, 5 bước sáng khi cuộn tới | Đúng | ✅ |
| TC-A14 | Người dùng bật "giảm chuyển động" | Giả lập `prefers-reduced-motion: reduce` | Không Lenis, 0 ScrollTrigger, không màn chuyển trang, nội dung hiện tĩnh đầy đủ, thanh quy trình full | Đúng | ✅ |
| TC-A15 | CDN GSAP chết | Trỏ `gsap.min.js` sang URL 404 | Gỡ `js-anim` ngay, trang hiện đầy đủ, đăng nhập/header vẫn chạy | Đúng (hiện ngay, không cần chờ failsafe 4s) | ✅ |
| TC-A16 | Lỗi JS giữa lúc init | (Gặp thật: lỗi TDZ `FROM`) | Có `try/catch` → dọn trigger, hiện nội dung tĩnh | Đã thêm, lỗi gốc đã fix | ✅ |
| TC-A17 | Marquee công nghệ | Xem dưới hero | Chạy ngang vô tận, dừng khi hover, chip lặp có `aria-hidden` | Đúng | ✅ |
| TC-A18 | Giao diện sau khi bỏ CDN | So sánh login / trang chủ trước-sau | Màu, bo góc, font, layout giữ nguyên | Đúng (nút `rgb(72,202,228)`, card bo 16px, font Inter) | ✅ |
| TC-A19 | Trình duyệt không hỗ trợ View Transition | Firefox / Chrome cũ | Chuyển trang bình thường, không lỗi, không màn che | Theo thiết kế (CSS `@view-transition` bị bỏ qua) | ✅ |
