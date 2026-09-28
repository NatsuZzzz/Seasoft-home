package com.store.seasoft;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetApiTest extends ApiTestBase {

    private static final String GENERIC_MESSAGE = "Nếu email tồn tại trong hệ thống, link đặt lại mật khẩu đã được gửi";

    private ResultActions forgot(String email) throws Exception {
        return postJson("/api/auth/forgot-password", "{\"email\":\"%s\"}".formatted(email));
    }

    private ResultActions reset(String token, String newPassword) throws Exception {
        return postJson("/api/auth/reset-password",
                "{\"token\":\"%s\",\"newPassword\":\"%s\"}".formatted(token, newPassword));
    }

    // Lay token tu noi dung email da "gui" (MailService bi mock)
    private String tokenFromMail(String email) {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailService, org.mockito.Mockito.atLeastOnce()).send(eq(email), anyString(), body.capture());
        String text = body.getValue();
        int i = text.indexOf("token=") + "token=".length();
        return text.substring(i).split("\\s")[0];
    }

    @Test
    void TC_P01_forgotExistingEmail_sendsMailWithLink() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        forgot(email).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(GENERIC_MESSAGE));
        assertThat(tokenFromMail(email)).isNotBlank();
    }

    @Test
    void TC_P02_forgotUnknownEmail_sameResponse_noMail() throws Exception {
        String email = uniqueEmail();
        forgot(email).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(GENERIC_MESSAGE));
        verify(mailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void TC_P03_forgotInvalidEmail_returns400() throws Exception {
        forgot("abc").andExpect(status().isBadRequest());
    }

    @Test
    void TC_P04_resetSuccess_canLoginWithNewPassword() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        forgot(email);
        reset(tokenFromMail(email), "NewPass456").andExpect(status().isOk());
        login(email, "NewPass456").andExpect(status().isOk());
    }

    @Test
    void TC_P05_afterReset_oldPasswordFails() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        forgot(email);
        reset(tokenFromMail(email), "NewPass456");
        login(email, PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void TC_P06_tokenCannotBeUsedTwice() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        forgot(email);
        String token = tokenFromMail(email);
        reset(token, "NewPass456").andExpect(status().isOk());
        reset(token, "Another789").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"));
    }

    @Test
    void TC_P07_expiredToken_returns400() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        forgot(email);
        String token = tokenFromMail(email);
        sql("update password_reset_tokens set expires_at = now() - interval '1 minute'");
        reset(token, "NewPass456").andExpect(status().isBadRequest());
    }

    @Test
    void TC_P08_randomToken_returns400() throws Exception {
        reset("token-bua", "NewPass456").andExpect(status().isBadRequest());
    }

    @Test
    void TC_P09_shortNewPassword_returns400() throws Exception {
        reset("abc", "123").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").exists());
    }

    @Test
    void TC_P10_requestingNewLink_invalidatesOldLink() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        forgot(email);
        String first = tokenFromMail(email);
        forgot(email);
        String second = tokenFromMail(email);
        assertThat(second).isNotEqualTo(first);
        reset(first, "NewPass456").andExpect(status().isBadRequest());
        reset(second, "NewPass456").andExpect(status().isOk());
    }

    @Test
    void TC_P11_resetRevokesAllRefreshTokens() throws Exception {
        String email = uniqueEmail();
        String rt = json(registerOk(email), "$.refreshToken");
        forgot(email);
        reset(tokenFromMail(email), "NewPass456");
        postJson("/api/auth/refresh", "{\"refreshToken\":\"%s\"}".formatted(rt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void TC_P12_suspendedUser_noMailSent() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        sql("update users set status = 'SUSPENDED' where email = ?", email);
        forgot(email).andExpect(status().isOk());
        verify(mailService, never()).send(anyString(), anyString(), anyString());
    }
}
