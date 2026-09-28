package com.store.seasoft;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegisterApiTest extends ApiTestBase {

    @Test
    void TC_R01_registerSuccess_returnsTokensAndCustomerRole() throws Exception {
        String email = uniqueEmail();
        register("Nguyễn Văn A", email, "0901234567", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.fullName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void TC_R02_passwordIsStoredHashed() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        String hash = queryOne("select password_hash from users where email = ?", String.class, email);
        assertThat(hash).isNotEqualTo(PASSWORD).startsWith("$2");
    }

    @Test
    void TC_R03_duplicateEmail_returns400() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        register("Khác", email, null, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email đã được sử dụng"));
    }

    @Test
    void TC_R04_duplicateEmailDifferentCase_returns400() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        register("Khác", "  " + email.toUpperCase() + " ", null, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email đã được sử dụng"));
    }

    @Test
    void TC_R05_emailIsNormalizedToLowercase() throws Exception {
        String email = uniqueEmail();
        register("Test", email.toUpperCase(), null, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void TC_R06_blankFullName_returnsFieldError() throws Exception {
        register("", uniqueEmail(), null, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").value("Họ tên không được để trống"));
    }

    @Test
    void TC_R07_invalidEmail_returnsFieldError() throws Exception {
        register("Test", "khong-phai-email", null, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("Email không hợp lệ"));
    }

    @Test
    void TC_R08_shortPassword_returnsFieldError() throws Exception {
        register("Test", uniqueEmail(), null, "12345")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value(startsWith("Mật khẩu từ 6")));
    }

    @Test
    void TC_R09_invalidPhone_returnsFieldError() throws Exception {
        register("Test", uniqueEmail(), "abc", PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").value("Số điện thoại không hợp lệ"));
    }

    @Test
    void TC_R10_cannotSelfAssignAdminRole() throws Exception {
        String email = uniqueEmail();
        postJson("/api/auth/register", """
                {"fullName":"Hacker","email":"%s","password":"%s","role":"ADMIN"}""".formatted(email, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void TC_R11_emptyBody_returns400() throws Exception {
        postJson("/api/auth/register", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void TC_R12_newUserIsActiveAndCanLoginImmediately() throws Exception {
        String email = uniqueEmail();
        registerOk(email);
        String status = queryOne("select status from users where email = ?", String.class, email);
        assertThat(status).isEqualTo("ACTIVE");
        login(email, PASSWORD).andExpect(status().isOk());
    }
}
