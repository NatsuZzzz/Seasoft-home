package com.store.seasoft;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RefreshTokenApiTest extends ApiTestBase {

    private org.springframework.test.web.servlet.ResultActions refresh(String refreshToken) throws Exception {
        return postJson("/api/auth/refresh", "{\"refreshToken\":\"%s\"}".formatted(refreshToken));
    }

    private org.springframework.test.web.servlet.ResultActions logout(String refreshToken) throws Exception {
        return postJson("/api/auth/logout", "{\"refreshToken\":\"%s\"}".formatted(refreshToken));
    }

    @Test
    void TC_RT01_refreshSuccess_returnsNewPair() throws Exception {
        String body = registerOk(uniqueEmail());
        String rt = json(body, "$.refreshToken");
        String newBody = refresh(rt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        assertThat(json(newBody, "$.refreshToken")).isNotEqualTo(rt);
    }

    @Test
    void TC_RT02_newAccessTokenWorks() throws Exception {
        String email = uniqueEmail();
        String body = registerOk(email);
        String newBody = refresh(json(body, "$.refreshToken")).andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(newBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void TC_RT03_oldRefreshTokenCannotBeReused() throws Exception {
        String rt = json(registerOk(uniqueEmail()), "$.refreshToken");
        refresh(rt).andExpect(status().isOk());
        refresh(rt)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại"));
    }

    @Test
    void TC_RT04_reuseOfRevokedToken_revokesWholeFamily() throws Exception {
        String rt1 = json(registerOk(uniqueEmail()), "$.refreshToken");
        String rt2 = json(refresh(rt1).andReturn().getResponse().getContentAsString(), "$.refreshToken");
        refresh(rt1).andExpect(status().isUnauthorized()); // ke gian dung lai token cu
        refresh(rt2).andExpect(status().isUnauthorized()); // token moi cung bi thu hoi
    }

    @Test
    void TC_RT05_randomToken_returns401() throws Exception {
        refresh("khong-ton-tai").andExpect(status().isUnauthorized());
    }

    @Test
    void TC_RT06_blankToken_returns400() throws Exception {
        refresh("").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.refreshToken").value("Thiếu refresh token"));
    }

    @Test
    void TC_RT07_expiredToken_returns401() throws Exception {
        String email = uniqueEmail();
        String rt = json(registerOk(email), "$.refreshToken");
        sql("update refresh_tokens set expires_at = now() - interval '1 minute' "
                + "where user_id = (select id from users where email = ?)", email);
        refresh(rt).andExpect(status().isUnauthorized());
    }

    @Test
    void TC_RT08_suspendedUser_cannotRefresh() throws Exception {
        String email = uniqueEmail();
        String rt = json(registerOk(email), "$.refreshToken");
        sql("update users set status = 'SUSPENDED' where email = ?", email);
        refresh(rt).andExpect(status().isForbidden());
    }

    @Test
    void TC_RT09_logout_revokesRefreshToken() throws Exception {
        String rt = json(registerOk(uniqueEmail()), "$.refreshToken");
        logout(rt).andExpect(status().isNoContent());
        refresh(rt).andExpect(status().isUnauthorized());
    }

    @Test
    void TC_RT10_logoutUnknownToken_isIdempotent() throws Exception {
        logout("khong-ton-tai").andExpect(status().isNoContent());
    }

    @Test
    void TC_RT11_refreshTokenStoredAsHashOnly() throws Exception {
        String email = uniqueEmail();
        String rt = json(registerOk(email), "$.refreshToken");
        String hash = queryOne("select token_hash from refresh_tokens "
                + "where user_id = (select id from users where email = ?)", String.class, email);
        assertThat(hash).isNotEqualTo(rt).hasSize(64);
    }

    @Test
    void TC_RT12_refreshTokenIsNotAcceptedAsAccessToken() throws Exception {
        String rt = json(registerOk(uniqueEmail()), "$.refreshToken");
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + rt))
                .andExpect(status().isUnauthorized());
    }
}
