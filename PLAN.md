# SeaSoft — Plan từ hiện tại đến Deploy

> Stack: Spring Boot 4.1.1 (Java 21) + PostgreSQL + JWT · Frontend: HTML tĩnh + Tailwind (CDN) + fetch API
> Quy ước (CLAUDE.md): sau mỗi mốc nhỏ → **đánh giá** · sau mỗi chức năng → **test** + **file testcase .md (≥10 case)** trong `docs/testcases/`

---

## 0. Hiện trạng

| Hạng mục                                                             | Trạng thái |
| -------------------------------------------------------------------- | ---------- |
| DB schema `Database/db.sql`                                          | ✅         |
| Entity/Repo: User, Role, CustomerProfile, StaffProfile               | ✅         |
| Auth: `POST /api/auth/register`, `POST /api/auth/login`, JWT, BCrypt | ✅         |
| Trang HTML: `Page.html`, `login.html`, `register.html`               | ✅         |
| Test tự động                                                         | ❌         |
| Refresh token / quên mật khẩu                                        | ❌         |
| Nghiệp vụ chính (tư vấn, dự án, admin)                               | ❌         |
| Hiệu ứng giao diện động                                              | ❌         |
| Docker / CI / deploy                                                 | ❌         |

### ⚠️ Cần xử lý sớm

1. Secret (DB password, `jwt.secret`) nằm trong `application.properties` → chuyển sang env var, đổi secret mới.
2. `ddl-auto=update` song song `db.sql` → chuyển sang Flyway, prod dùng `validate`.
3. `users.status` trong `db.sql` là PG enum `user_status`, entity map `EnumType.STRING` (varchar) → insert sẽ lỗi nếu DB tạo từ `db.sql`. Fix bằng `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` hoặc đổi cột sang VARCHAR + CHECK.
4. `README.md` encode UTF-16 → UTF-8.
5. CORS `*` → giới hạn domain thật khi lên prod.

---

## Phase 1 — Dọn nền móng

- [x] 1.1 Tách config theo profile (`dev`/`prod`), secret đọc từ env (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ORIGINS`)
- [x] 1.2 Flyway: `db.sql` → `V1__init_users.sql` (fix cột status)
- [x] 1.3 Chuẩn hoá response lỗi `{status, message, errors}` (+ lỗi validation)
- [x] 1.4 Setup test infra (MockMvc + Postgres `seasoft_test` — Docker daemon tắt nên chưa dùng Testcontainers), thư mục `docs/testcases/`
- [x] 1.5 README UTF-8, `.gitignore`
- **Mốc đánh giá #1**: chạy được bằng env var, `mvn test` xanh, không còn secret trong repo

## Phase 2 — Hoàn thiện Authentication

- [x] 2.1 Test + `TC_register.md`
- [x] 2.2 Test + `TC_login.md`
- [x] 2.3 Refresh token + logout (revoke, rotation, phát hiện dùng lại token) — `TC_refresh_token.md`
- [x] 2.4 Quên / đặt lại mật khẩu (Spring Mail; dev log ra console) — `TC_password_reset.md`
- [ ] 2.5 (Tuỳ chọn) Xác thực email — **hoãn**, làm khi có SMTP thật (Phase 9)
- [x] 2.6 `GET/PUT /api/users/me`, đổi mật khẩu — `TC_user_profile.md`
- [x] 2.7 Frontend: `js/auth.js`, guard trang, auto refresh token, nút logout, `profile.html`, `forgot-password.html`, `reset-password.html`
- **Mốc đánh giá #2**

## Phase 3 — Giao diện động & hiệu ứng cuộn ✨

Mục tiêu: kéo xuống tới đâu, nội dung "diễn" tới đó — mượt, không giật, không làm chậm trang.

- [ ] 3.1 Thư viện: **GSAP + ScrollTrigger** (hiệu ứng theo cuộn) + **Lenis** (smooth scroll), load từ CDN jsDelivr
- [ ] 3.2 File dùng chung `js/animations.js` + `css/animations.css`, gắn bằng data-attribute (`data-reveal="fade-up"`, `data-stagger`, `data-parallax`) để trang nào cũng tái sử dụng
- [ ] 3.3 Hero: text xuất hiện từng dòng/từng chữ, mockup parallax nhẹ, nền wave chuyển động (theo logo sóng)
- [ ] 3.4 Reveal khi cuộn: fade-up, slide-in trái/phải, scale-in cho card dịch vụ; stagger cho danh sách
- [ ] 3.5 Section "Quy trình" (`#process`): **pin + scrub** — cuộn tới đâu từng bước sáng lên tới đó, thanh tiến trình chạy theo
- [ ] 3.6 Chuyển cảnh giữa section: đổi màu nền mượt khi cuộn (trắng → navy), wave divider
- [ ] 3.7 Counter số liệu chạy số, marquee logo khách hàng
- [ ] 3.8 Micro-interaction: hover card nổi 3D nhẹ, nút magnetic, header co lại + blur khi cuộn, progress bar đọc trang
- [ ] 3.9 Chuyển trang (Page ↔ login ↔ register): overlay fade/slide
- [ ] 3.10 Login/Register: form xuất hiện mượt, shake khi lỗi, loading state trên nút
- [ ] 3.11 Hiệu năng & a11y: chỉ animate `transform`/`opacity`, tôn trọng `prefers-reduced-motion`, giảm parallax trên mobile
- [ ] 3.12 Test (desktop + mobile, reduced-motion) + `TC_ui_animation.md`
- **Mốc đánh giá #3**

## Phase 4 — Đăng ký tư vấn (Lead)

- [ ] 4.1 Migration `V2__consultations.sql` (status NEW/CONTACTED/QUOTED/WON/LOST, assigned_staff_id)
- [ ] 4.2 `POST /api/consultations` public + chống spam
- [ ] 4.3 API staff: list/filter/phân công/đổi trạng thái
- [ ] 4.4 Gắn form tư vấn trên landing (animation gửi thành công)
- [ ] 4.5 Test + `TC_consultation.md`
- **Mốc đánh giá #4**

## Phase 5 — Theo dõi dự án (Customer Portal)

- [ ] 5.1 Migration `V3__projects.sql`: projects, project_milestones, project_updates
- [ ] 5.2 API staff tạo/cập nhật, customer chỉ xem dự án của mình (test IDOR)
- [ ] 5.3 `dashboard.html`: danh sách dự án, progress bar animate, timeline
- [ ] 5.4 Test + `TC_project.md`
- **Mốc đánh giá #5**

## Phase 6 — Admin / Manager Panel

- [ ] 6.1 Quản lý user: tạo STAFF, khoá/mở, đổi role
- [ ] 6.2 Quản lý lead + dự án (filter, phân trang)
- [ ] 6.3 Thống kê
- [ ] 6.4 `admin.html`
- [ ] 6.5 Test + `TC_admin_user.md`, `TC_admin_stats.md`
- **Mốc đánh giá #6**

## Phase 7 — (Tuỳ chọn) Marketing & SEO

- [ ] Portfolio / case study, blog, testimonial
- [ ] SEO: meta, sitemap.xml, robots.txt, OG image

## Phase 8 — Hardening

- [ ] 8.1 Security review: CORS, rate-limit login, security headers, validate input
- [ ] 8.2 Tailwind CDN → build CSS tĩnh
- [ ] 8.3 Gom JS chung `api.js`
- [ ] 8.4 Responsive, Lighthouse ≥ 90
- [ ] 8.5 Actuator health, logging prod
- **Mốc đánh giá #8**: regression toàn bộ

## Phase 9 — Đóng gói & Deploy 🚀

- [ ] 9.1 Dockerfile multi-stage
- [ ] 9.2 Frontend vào `src/main/resources/static/` (1 service)
- [ ] 9.3 `docker-compose.yml`: app + postgres
- [ ] 9.4 CI GitHub Actions: test + build image
- [ ] 9.5 Hosting, env var, Flyway, seed ADMIN
- [ ] 9.6 Domain + HTTPS, CORS về domain thật
- [ ] 9.7 Smoke test prod + `TC_deploy_smoke.md`
- [ ] 9.8 Backup DB, uptime monitoring
- **Mốc đánh giá cuối**

| Hosting                                | Ưu                                | Nhược                |
| -------------------------------------- | --------------------------------- | -------------------- |
| Render/Railway + Neon                  | Free, deploy từ GitHub, HTTPS sẵn | Ngủ đông, cold start |
| VPS + Docker Compose + Nginx + Certbot | Toàn quyền, rẻ lâu dài            | Tự quản server       |

---

## Quy trình mỗi chức năng

code → test tự động → test tay trên UI → `docs/testcases/TC_<tên>.md` (≥10 case) → đánh giá mốc → tick checklist → commit

## Nhật ký đánh giá

### Mốc #1 — Phase 1 (2026-09-28) ✅

- **Kết quả**: 3 test tự động pass; smoke test 8 request trên DB thật đều đúng; `TC_foundation.md` 13 case pass.
- **Làm thêm ngoài plan**:
  - Fix bug token rác/hết hạn → server trả 500 (filter ném exception). Giờ là 401.
  - Chưa login → trước là 403 body rỗng, giờ 401 JSON; thiếu quyền → 403 JSON.
  - Login tài khoản SUSPENDED/chưa kích hoạt → 403 có message (trước rơi vào lỗi không bắt).
  - Filter chặn cả token của user đã bị khoá.
- **Phát hiện**: DB dev thực tế khác `db.sql` (Hibernate `update` đã tự sửa) → V2 chuẩn hoá `status`. Backup DB trước khi migrate nằm ở scratchpad (`SeaSoft_before_flyway.dump`).
- **Nợ kỹ thuật**: test chạy trên Postgres local, CI cần service Postgres (Phase 9.4). `Database/db.sql` giờ chỉ để tham khảo.
- **Tiếp theo**: Phase 2 — Auth.

### Mốc #2 — Phase 2 (2026-09-28) ✅
- **Kết quả**: 63/63 test tự động pass (5 class × 12 + 3). Test UI trên browser thật: đăng ký → header → hồ sơ → auto refresh → đổi mật khẩu → login lại với `next` → logout → guard → quên/đặt lại mật khẩu: tất cả pass, không lỗi JS. 5 file testcase (14–16 case/file).
- **Bug bắt được**:
  - Email có khoảng trắng đầu/cuối bị `@Email` từ chối (400) → trim trong setter DTO.
  - Bulk update (`@Modifying`) không đồng bộ cache Hibernate → thêm flush/clear tự động.
  - Reset mật khẩu trên entity detached không được lưu → load lại user theo id.
  - Menu mobile `Page.html` trỏ `#login`/`#register` (link chết) → sửa.
  - `register.html` bật lại nút submit sau khi đăng ký thành công → sửa.
- **Thay đổi đáng chú ý**:
  - Access token 15 phút (trước 24h), refresh token 7 ngày.
  - Spring Boot serve luôn `Html/` → mở http://localhost:8080 là có web, không cần Live Server.
  - Checkbox "Ghi nhớ 30 ngày" đổi thành "Ghi nhớ trên thiết bị này" cho đúng thực tế.
- **Nợ kỹ thuật**: `login.html`/`register.html`/`Page.html` vẫn nhúng Tailwind config + CSS inline (trang mới dùng file chung `js/tailwind-config.js`, `css/auth-pages.css`) → gom ở Phase 8.3. Frontend yêu cầu mật khẩu ≥ 8 khi đăng ký, backend ≥ 6 → thống nhất khi hardening.
- **Tiếp theo**: Phase 3 — Giao diện động ✨.
