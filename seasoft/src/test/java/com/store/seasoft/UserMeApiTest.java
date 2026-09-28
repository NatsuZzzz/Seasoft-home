package com.store.seasoft;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserMeApiTest extends ApiTestBase {

    private ResultActions putJson(String url, String auth, String json) throws Exception {
        return mockMvc.perform(put(url).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void TC_U01_getMe_returnsProfileWithoutPassword() throws Exception {
        String email = uniqueEmail();
        String auth = bearer(registerOk(email));
        mockMvc.perform(get("/api/users/me").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void TC_U02_getMeWithoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void TC_U03_updateProfile_success() throws Exception {
        String auth = bearer(registerOk(uniqueEmail()));
        putJson("/api/users/me", auth, "{\"fullName\":\"Tên Mới\",\"phone\":\"0912345678\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Tên Mới"))
                .andExpect(jsonPath("$.phone").value("0912345678"));
        mockMvc.perform(get("/api/users/me").header("Authorization", auth))
                .andExpect(jsonPath("$.fullName").value("Tên Mới"));
    }

    @Test
    void TC_U04_updateProfile_blankName_returns400() throws Exception {
        String auth = bearer(registerOk(uniqueEmail()));
        putJson("/api/users/me", auth, "{\"fullName\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists());
    }

    @Test
    void TC_U05_updateProfile_cannotChangeEmailOrRole() throws Exception {
        String email = uniqueEmail();
        String auth = bearer(registerOk(email));
        putJson("/api/users/me", auth,
                "{\"fullName\":\"X\",\"email\":\"hack@x.com\",\"role\":\"ADMIN\",\"status\":\"ACTIVE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void TC_U06_updateProfile_clearPhone() throws Exception {
        String auth = bearer(registerOk(uniqueEmail()));
        putJson("/api/users/me", auth, "{\"fullName\":\"X\",\"phone\":\"\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").doesNotExist());
    }

    @Test
    void TC_U07_changePassword_success() throws Exception {
        String email = uniqueEmail();
        String auth = bearer(registerOk(email));
        putJson("/api/users/me/password", auth,
                "{\"currentPassword\":\"%s\",\"newPassword\":\"NewPass456\"}".formatted(PASSWORD))
                .andExpect(status().isOk());
        login(email, "NewPass456").andExpect(status().isOk());
        login(email, PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void TC_U08_changePassword_wrongCurrent_returns400() throws Exception {
        String auth = bearer(registerOk(uniqueEmail()));
        putJson("/api/users/me/password", auth, "{\"currentPassword\":\"sai-roi\",\"newPassword\":\"NewPass456\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mật khẩu hiện tại không đúng"));
    }

    @Test
    void TC_U09_changePassword_sameAsOld_returns400() throws Exception {
        String auth = bearer(registerOk(uniqueEmail()));
        putJson("/api/users/me/password", auth,
                "{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(PASSWORD, PASSWORD))
                .andExpect(status().isBadRequest());
    }

    @Test
    void TC_U10_changePassword_revokesRefreshTokens() throws Exception {
        String body = registerOk(uniqueEmail());
        putJson("/api/users/me/password", bearer(body),
                "{\"currentPassword\":\"%s\",\"newPassword\":\"NewPass456\"}".formatted(PASSWORD));
        postJson("/api/auth/refresh", "{\"refreshToken\":\"%s\"}".formatted(json(body, "$.refreshToken")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void TC_U11_eachUserSeesOnlyOwnProfile() throws Exception {
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();
        String authA = bearer(registerOk(emailA));
        registerOk(emailB);
        mockMvc.perform(get("/api/users/me").header("Authorization", authA))
                .andExpect(jsonPath("$.email").value(emailA));
    }

    @Test
    void TC_U12_expiredOrTamperedToken_returns401() throws Exception {
        String auth = bearer(registerOk(uniqueEmail()));
        mockMvc.perform(get("/api/users/me").header("Authorization", auth + "x"))
                .andExpect(status().isUnauthorized());
    }
}
