package com.store.seasoft;

import com.store.seasoft.Service.BlogRenderer;
import com.store.seasoft.Service.JwtService;
import com.store.seasoft.Service.RateLimiter;
import com.store.seasoft.Service.TokenCleanupJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Phase 8: security headers, rate limit, chinh sach mat khau, actuator, SSR blog, don token
class SecurityHardeningTest extends ContentTestSupport {

    @Autowired
    TokenCleanupJob cleanupJob;

    @Test
    void TC_H01_securityHeaders() throws Exception {
        mockMvc.perform(get("/api/public/portfolio"))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self' 'unsafe-inline';")))
                .andExpect(header().string("Content-Security-Policy", not(containsString("jsdelivr"))))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", containsString("camera=()")));
    }

    @Test
    void TC_H02_loginRateLimitPerEmail() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        for (int i = 0; i < 10; i++) {
            login(email, "SaiMatKhau1").andExpect(status().isUnauthorized());
        }
        login(email, PASSWORD).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Bạn thao tác quá nhiều lần, vui lòng thử lại sau ít phút"));
        login(uniqueEmail(), PASSWORD).andExpect(status().isUnauthorized()); // email khac khong bi anh huong
    }

    @Test
    void TC_H03_forgotRateLimitPerEmail() throws Exception {
        String email = uniqueEmail();
        for (int i = 0; i < 3; i++) {
            postJson("/api/auth/forgot-password", "{\"email\":\"" + email + "\"}").andExpect(status().isOk());
        }
        postJson("/api/auth/forgot-password", "{\"email\":\"" + email + "\"}").andExpect(status().isTooManyRequests());
    }

    @Test
    void TC_H04_passwordPolicyMin8() throws Exception {
        register("Test", uniqueEmail(), null, "Abc1234").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Mật khẩu từ 8 đến 100 ký tự"));
        register("Test", uniqueEmail(), null, "Abc12345").andExpect(status().isOk());
    }

    @Test
    void TC_H05_loginPasswordTooLong_rejectedBeforeBcrypt() throws Exception {
        login(uniqueEmail(), "x".repeat(101)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Mật khẩu tối đa 100 ký tự"));
    }

    @Test
    void TC_H06_actuatorOnlyHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mockMvc.perform(get("/actuator/env")).andExpect(result ->
                assertThat(result.getResponse().getStatus()).isIn(401, 403, 404));
    }

    @Test
    void TC_H07_ssrPublishedPost() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String slug = json(send("POST", "/api/admin/content/blog", admin, """
                {"title":"SSR có dấu","excerpt":"Mô tả SEO","content":"## Mục A\\nĐoạn <script>alert(1)</script>","status":"PUBLISHED"}""")
                .andReturn().getResponse().getContentAsString(), "$.summary.slug");
        mockMvc.perform(get("/post.html?slug=" + slug))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>SSR có dấu | Blog SeaSoft</title>")))
                .andExpect(content().string(containsString("content=\"Mô tả SEO\"")))
                .andExpect(content().string(containsString("rel=\"canonical\"")))
                .andExpect(content().string(containsString("\"@type\":\"BlogPosting\"")))
                .andExpect(content().string(containsString("<h2>Mục A</h2>")))
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)"))))
                .andExpect(content().string(containsString("<div id=\"content\" class=\"space-y-6")));
    }

    @Test
    void TC_H08_ssrUnknownOrDraft_404Noindex() throws Exception {
        mockMvc.perform(get("/post.html?slug=khong-ton-tai"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("name=\"robots\" content=\"noindex\"")));
        mockMvc.perform(get("/post.html")).andExpect(status().isOk());
    }

    @Test
    void TC_H09_ssrJsonLdCannotBreakOutOfScript() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String slug = json(send("POST", "/api/admin/content/blog", admin, """
                {"title":"Tiêu đề </script><script>alert(1)</script>","content":"x","status":"PUBLISHED"}""")
                .andReturn().getResponse().getContentAsString(), "$.summary.slug");
        String html = mockMvc.perform(get("/post.html?slug=" + slug)).andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("</script><script>alert(1)");
        assertThat(html).contains("\\u003c/script>");
    }

    @Test
    void TC_H10_rateLimiterSlidingWindow() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        Clock clock = new Clock() {
            public ZoneOffset getZone() { return ZoneOffset.UTC; }
            public Clock withZone(java.time.ZoneId z) { return this; }
            public Instant instant() { return now.get(); }
        };
        RateLimiter rl = new RateLimiter(2, Duration.ofMinutes(10), clock);
        assertThat(rl.tryAcquire("ip")).isTrue();
        assertThat(rl.tryAcquire("ip")).isTrue();
        assertThat(rl.tryAcquire("ip")).isFalse();
        assertThat(rl.tryAcquire("ip-khac")).isTrue();
        now.set(now.get().plus(Duration.ofMinutes(10)).plusSeconds(1));
        assertThat(rl.tryAcquire("ip")).isTrue(); // het cua so -> duoc goi lai
    }

    @Test
    void TC_H11_blogRendererMatchesFrontendRules() {
        assertThat(BlogRenderer.render("Dòng 1\nDòng 2\n\n## Tiêu đề\n- a\n- <b>\n### Nhỏ"))
                .isEqualTo("<p>Dòng 1<br>Dòng 2</p>\n<h2>Tiêu đề</h2>\n<ul><li>a</li><li>&lt;b&gt;</li></ul>\n<h3>Nhỏ</h3>");
    }

    @Test
    void TC_H12_cleanupJobRemovesOldTokens() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        sql("insert into refresh_tokens(user_id, token_hash, expires_at) "
                + "select id, 'old', now() - interval '3 days' from users where email = ?", email);
        sql("insert into password_reset_tokens(user_id, token_hash, expires_at) "
                + "select id, 'old', now() - interval '3 days' from users where email = ?", email);
        assertThat(cleanupJob.cleanup()).isGreaterThanOrEqualTo(2);
        assertThat(queryOne("select count(*) from refresh_tokens where token_hash = 'old'", Long.class)).isZero();
        assertThat(queryOne("select count(*) from refresh_tokens r join users u on u.id = r.user_id where u.email = ?",
                Long.class, email)).isEqualTo(1); // token con han cua lan dang ky van giu
    }

    @Test
    void TC_H13_weakJwtSecretFailsFast() {
        JwtService jwt = new JwtService();
        ReflectionTestUtils.setField(jwt, "secretKey", "c2hvcnQ="); // "short"
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(jwt, "validateSecret"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("quá ngắn");
        ReflectionTestUtils.setField(jwt, "secretKey", "khong-phai-base64!!");
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(jwt, "validateSecret"))
                .isInstanceOf(IllegalStateException.class);
    }
}
