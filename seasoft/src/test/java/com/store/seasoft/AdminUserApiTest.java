package com.store.seasoft;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminUserApiTest extends ApiTestBase {

    ResultActions createStaff(String auth, String email, String role, String code) throws Exception {
        String codeJson = code == null ? "" : ",\"employeeCode\":\"" + code + "\"";
        return mockMvc.perform(post("/api/admin/users").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Nhân Viên Mới\",\"email\":\"" + email + "\",\"role\":\"" + role + "\","
                        + "\"department\":\"Dev\",\"position\":\"Frontend\"" + codeJson + "}"));
    }

    ResultActions patchJson(String url, String auth, String json) throws Exception {
        return mockMvc.perform(patch(url).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    String tokenFromLastMail(String email) {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).send(eq(email), anyString(), body.capture());
        String text = body.getValue();
        return text.substring(text.indexOf("token=") + 6).split("\\s")[0];
    }

    @Test
    void TC_AU01_onlyManagerAndAdminCanAccess() throws Exception {
        String staff = userWithRole("STAFF", uniqueEmail());
        String customer = bearer(registerOk(uniqueEmail()));
        String manager = userWithRole("MANAGER", uniqueEmail());
        mockMvc.perform(get("/api/admin/users").header("Authorization", staff)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users").header("Authorization", customer)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users").header("Authorization", manager)).andExpect(status().isOk());
    }

    @Test
    void TC_AU02_listWithFilters() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String staffEmail = uniqueEmail();
        userWithRole("STAFF", staffEmail);
        mockMvc.perform(get("/api/admin/users?role=STAFF&q=" + staffEmail).header("Authorization", admin))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].email").value(staffEmail))
                .andExpect(jsonPath("$.items[0].passwordHash").doesNotExist());
        mockMvc.perform(get("/api/admin/users?status=ACTIVE&role=CUSTOMER").header("Authorization", admin))
                .andExpect(jsonPath("$.items[*].role", everyItem(is("CUSTOMER"))));
    }

    @Test
    void TC_AU03_createStaff_inviteMail_thenSetPasswordAndLogin() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String email = uniqueEmail();
        createStaff(admin, email, "STAFF", "NV-" + System.nanoTime())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.staffProfile.department").value("Dev"));
        verify(mailService).send(eq(email), contains("Mời bạn tham gia"), contains("48 giờ"));
        postJson("/api/auth/reset-password",
                "{\"token\":\"" + tokenFromLastMail(email) + "\",\"newPassword\":\"StaffPass1\"}")
                .andExpect(status().isOk());
        login(email, "StaffPass1").andExpect(status().isOk()).andExpect(jsonPath("$.role").value("STAFF"));
    }

    @Test
    void TC_AU04_managerCanOnlyCreateStaff() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        createStaff(manager, uniqueEmail(), "MANAGER", null).andExpect(status().isForbidden());
        createStaff(manager, uniqueEmail(), "ADMIN", null).andExpect(status().isForbidden());
        createStaff(manager, uniqueEmail(), "STAFF", null).andExpect(status().isCreated());
    }

    @Test
    void TC_AU05_createConflictsAndInvalidRole() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String existing = uniqueEmail();
        registerOk(existing);
        createStaff(admin, existing, "STAFF", null).andExpect(status().isConflict());
        String code = "NV-" + System.nanoTime();
        createStaff(admin, uniqueEmail(), "STAFF", code).andExpect(status().isCreated());
        createStaff(admin, uniqueEmail(), "STAFF", code).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Mã nhân viên đã tồn tại"));
        createStaff(admin, uniqueEmail(), "CUSTOMER", null).andExpect(status().isBadRequest());
    }

    @Test
    void TC_AU06_suspend_blocksLoginRefreshAndExistingToken() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String email = uniqueEmail();
        String body = registerOk(email);
        patchJson("/api/admin/users/" + userId(email) + "/status", admin, "{\"status\":\"SUSPENDED\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));
        login(email, PASSWORD).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(body))).andExpect(status().isUnauthorized());
        postJson("/api/auth/refresh", "{\"refreshToken\":\"" + json(body, "$.refreshToken") + "\"}")
                .andExpect(status().isUnauthorized());
        patchJson("/api/admin/users/" + userId(email) + "/status", admin, "{\"status\":\"ACTIVE\"}");
        login(email, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void TC_AU07_cannotChangeOwnStatusOrRole() throws Exception {
        String email = uniqueEmail();
        String admin = userWithRole("ADMIN", email);
        patchJson("/api/admin/users/" + userId(email) + "/status", admin, "{\"status\":\"SUSPENDED\"}")
                .andExpect(status().isBadRequest());
        patchJson("/api/admin/users/" + userId(email) + "/role", admin, "{\"role\":\"STAFF\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void TC_AU08_managerCannotTouchManagersOrAdmins() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String adminEmail = uniqueEmail();
        userWithRole("ADMIN", adminEmail);
        String otherManager = uniqueEmail();
        userWithRole("MANAGER", otherManager);
        String staff = uniqueEmail();
        userWithRole("STAFF", staff);
        patchJson("/api/admin/users/" + userId(adminEmail) + "/status", manager, "{\"status\":\"SUSPENDED\"}")
                .andExpect(status().isForbidden());
        patchJson("/api/admin/users/" + userId(otherManager) + "/status", manager, "{\"status\":\"SUSPENDED\"}")
                .andExpect(status().isForbidden());
        patchJson("/api/admin/users/" + userId(staff) + "/status", manager, "{\"status\":\"SUSPENDED\"}")
                .andExpect(status().isOk());
    }

    @Test
    void TC_AU09_roleChange_adminOnly_takesEffectImmediately() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String manager = userWithRole("MANAGER", uniqueEmail());
        String email = uniqueEmail();
        String customerBody = registerOk(email);
        patchJson("/api/admin/users/" + userId(email) + "/role", manager, "{\"role\":\"STAFF\"}")
                .andExpect(status().isForbidden());
        patchJson("/api/admin/users/" + userId(email) + "/role", admin, "{\"role\":\"HACKER\"}")
                .andExpect(status().isBadRequest());
        patchJson("/api/admin/users/" + userId(email) + "/role", admin, "{\"role\":\"staff\"}")
                .andExpect(jsonPath("$.role").value("STAFF"));
        // Access token cu dung duoc voi quyen moi (filter nap role moi moi request)
        mockMvc.perform(get("/api/staff/consultations").header("Authorization", bearer(customerBody)))
                .andExpect(status().isOk());
        // Refresh token cu bi thu hoi
        postJson("/api/auth/refresh", "{\"refreshToken\":\"" + json(customerBody, "$.refreshToken") + "\"}")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void TC_AU10_updateStaffProfile() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String staff = uniqueEmail();
        userWithRole("STAFF", staff);
        patchJson("/api/admin/users/" + userId(staff), admin,
                "{\"fullName\":\"Tên Mới\",\"department\":\"Sales\",\"position\":\"Leader\",\"hireDate\":\"2025-01-15\"}")
                .andExpect(jsonPath("$.fullName").value("Tên Mới"))
                .andExpect(jsonPath("$.staffProfile.department").value("Sales"))
                .andExpect(jsonPath("$.staffProfile.hireDate").value("2025-01-15"));
        String customer = uniqueEmail();
        registerOk(customer);
        patchJson("/api/admin/users/" + userId(customer), admin, "{\"department\":\"Sales\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void TC_AU11_employeeCodeConflictOnUpdate() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String code = "NV-" + System.nanoTime();
        createStaff(admin, uniqueEmail(), "STAFF", code).andExpect(status().isCreated());
        String other = uniqueEmail();
        createStaff(admin, other, "STAFF", null).andExpect(status().isCreated());
        patchJson("/api/admin/users/" + userId(other), admin, "{\"employeeCode\":\"" + code + "\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void TC_AU12_adminSendsResetLink() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String email = uniqueEmail();
        registerOk(email);
        mockMvc.perform(post("/api/admin/users/" + userId(email) + "/reset-password").header("Authorization", admin))
                .andExpect(status().isOk());
        assertThat(tokenFromLastMail(email)).isNotBlank();
    }

    @Test
    void TC_AU13_notFoundAndBadParams() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        mockMvc.perform(get("/api/admin/users/00000000-0000-0000-0000-000000000000").header("Authorization", admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/admin/users?status=KHONG_CO").header("Authorization", admin))
                .andExpect(status().isBadRequest());
        String email = uniqueEmail();
        registerOk(email);
        patchJson("/api/admin/users/" + userId(email) + "/status", admin, "{}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.status").exists());
    }
}
