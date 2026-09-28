package com.store.seasoft;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginApiTest extends ApiTestBase {

    @Test
    void TC_L01_loginSuccess_returnsTokens() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        login(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void TC_L02_wrongPassword_returns401() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        login(email, "WrongPass1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng"));
    }

    @Test
    void TC_L03_unknownEmail_returnsSame401Message() throws Exception {
        login(uniqueEmail(), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng"));
    }

    @Test
    void TC_L04_emailCaseInsensitive() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        login(" " + email.toUpperCase() + " ", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void TC_L05_passwordIsCaseSensitive() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        login(email, PASSWORD.toLowerCase()).andExpect(status().isUnauthorized());
    }

    @Test
    void TC_L06_suspendedAccount_returns403() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        sql("update users set status = 'SUSPENDED' where email = ?", email);
        login(email, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khoá"));
    }

    @Test
    void TC_L07_inactiveAccount_returns403() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        sql("update users set status = 'INACTIVE' where email = ?", email);
        login(email, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tài khoản chưa được kích hoạt"));
    }

    @Test
    void TC_L08_blankFields_returnsFieldErrors() throws Exception {
        login("", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void TC_L09_malformedJson_returns400() throws Exception {
        postJson("/api/auth/login", "{oops")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body không hợp lệ"));
    }

    @Test
    void TC_L10_accessTokenWorksOnProtectedEndpoint() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        String body = login(email, PASSWORD).andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void TC_L11_lastLoginAtIsUpdated() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        sql("update users set last_login_at = null where email = ?", email);
        login(email, PASSWORD).andExpect(status().isOk());
        Timestamp last = queryOne("select last_login_at from users where email = ?", Timestamp.class, email);
        assertThat(last).isNotNull();
    }

    @Test
    void TC_L12_suspendedUserTokenIsRejected() throws Exception {
        String email = uniqueEmail();
        String body = registerOk(email);
        sql("update users set status = 'SUSPENDED' where email = ?", email);
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(body)))
                .andExpect(status().isUnauthorized());
    }
}
