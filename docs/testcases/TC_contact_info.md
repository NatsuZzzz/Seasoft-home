# TC — Cập nhật thông tin liên hệ

- Ngày test: 2026-09-29 · Trang: `Html/Page.html` · Test trên dev (localhost:8080) và production (https://seasoft.onrender.com) sau khi Render tự deploy
- Thông tin mới: SĐT `0372 236 632`, email `seasoft2026@gmail.com`, địa chỉ "Hoạt động tại TP. Hồ Chí Minh"

| ID | Kịch bản | Bước | Kết quả mong đợi | Trạng thái |
|---|---|---|---|---|
| TC-CI01 | Hotline cạnh form tư vấn | Xem mục "Liên hệ tư vấn" | Hiện `0372 236 632` | ✅ |
| TC-CI02 | Hotline bấm được | Kiểm tra `href` | `tel:+84372236632` (gọi được trên điện thoại) | ✅ |
| TC-CI03 | SĐT ở footer | Mục "Thông tin liên hệ" | `0372 236 632`, link `tel:` | ✅ |
| TC-CI04 | Email ở footer | Mục "Thông tin liên hệ" | `seasoft2026@gmail.com`, link `mailto:` | ✅ |
| TC-CI05 | Địa chỉ | Footer | "Hoạt động tại TP. Hồ Chí Minh, Việt Nam", không còn "Tòa nhà SeaSoft, Quận 1" | ✅ |
| TC-CI06 | JSON-LD (SEO) | Đọc `script[type=application/ld+json]` | `email` mới, `telephone` = `+84-372-236-632`, `addressLocality` = TP. Hồ Chí Minh | ✅ |
| TC-CI07 | Không sót thông tin cũ | Tìm `8888`, `contact@seasoft`, `Quận 1` trong toàn bộ `Html/` | 0 kết quả | ✅ |
| TC-CI08 | CSS không vỡ | Class mới (`hover:text-primary`, `font-bold`) có trong `tailwind.css` build sẵn | Có, không cần build lại | ✅ |
| TC-CI09 | Hover link | Rê chuột lên SĐT/email | Đổi sang màu primary | ✅ |
| TC-CI10 | Production | `curl https://seasoft.onrender.com/Page.html` sau deploy | Chứa SĐT, email mới, không chứa thông tin cũ | ✅ |
