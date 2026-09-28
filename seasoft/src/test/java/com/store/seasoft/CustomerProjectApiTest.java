package com.store.seasoft;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Khach hang xem du an cua minh (customer portal)
class CustomerProjectApiTest extends ProjectTestSupport {

    @Test
    void TC_CP01_listOwnProjects() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String[] pc = projectForNewCustomer(manager);
        mockMvc.perform(get("/api/projects").header("Authorization", pc[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(pc[0]))
                .andExpect(jsonPath("$[0].progress").value(0));
    }

    @Test
    void TC_CP02_detailWithMilestones() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String[] pc = projectForNewCustomer(manager);
        mockMvc.perform(get("/api/projects/" + pc[0]).header("Authorization", pc[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.milestones", hasSize(5)))
                .andExpect(jsonPath("$.summary.managerName").exists());
    }

    @Test
    void TC_CP03_otherCustomersProject_returns404() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String[] a = projectForNewCustomer(manager);
        String[] b = projectForNewCustomer(manager);
        mockMvc.perform(get("/api/projects/" + a[0]).header("Authorization", b[1]))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy dự án"));
        mockMvc.perform(get("/api/projects").header("Authorization", b[1]))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(b[0]));
    }

    @Test
    void TC_CP04_unknownOrInvalidId() throws Exception {
        String customer = bearer(registerOk(uniqueEmail()));
        mockMvc.perform(get("/api/projects/00000000-0000-0000-0000-000000000000").header("Authorization", customer))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/projects/abc").header("Authorization", customer))
                .andExpect(status().isBadRequest());
    }

    @Test
    void TC_CP05_commentVisibleToStaff_andManagerMailed() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String[] pc = projectForNewCustomer(manager);
        postAuth("/api/projects/" + pc[0] + "/comments", pc[1], "{\"content\":\"Mình muốn đổi màu chủ đạo\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorRole").value("CUSTOMER"));
        mockMvc.perform(get("/api/staff/projects/" + pc[0]).header("Authorization", manager))
                .andExpect(jsonPath("$.updates[0].content").value("Mình muốn đổi màu chủ đạo"));
        verify(mailService).send(anyString(), contains("Khách hàng vừa bình luận"), contains("đổi màu"));
    }

    @Test
    void TC_CP06_commentValidation() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String[] pc = projectForNewCustomer(manager);
        postAuth("/api/projects/" + pc[0] + "/comments", pc[1], "{\"content\":\"  \"}")
                .andExpect(status().isBadRequest());
        postAuth("/api/projects/" + pc[0] + "/comments", pc[1], "{\"content\":\"" + "x".repeat(2001) + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void TC_CP07_commentOnOthersProject_returns404() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String[] a = projectForNewCustomer(manager);
        String[] b = projectForNewCustomer(manager);
        postAuth("/api/projects/" + a[0] + "/comments", b[1], "{\"content\":\"hack\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    void TC_CP08_requiresLogin() throws Exception {
        mockMvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
    }

    @Test
    void TC_CP09_customerViewHidesInternalData() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String[] pc = projectForNewCustomer(manager);
        postAuth("/api/staff/projects/" + pc[0] + "/updates", manager,
                "{\"content\":\"Khách khó tính\",\"visibleToCustomer\":false}");
        mockMvc.perform(get("/api/projects/" + pc[0]).header("Authorization", pc[1]))
                .andExpect(jsonPath("$.updates", hasSize(0)))
                .andExpect(jsonPath("$.consultationId").doesNotExist())
                .andExpect(jsonPath("$.summary.customerEmail").doesNotExist());
    }

    @Test
    void TC_CP10_progressReflectsMilestones() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String[] pc = projectForNewCustomer(manager);
        String body = mockMvc.perform(get("/api/staff/projects/" + pc[0]).header("Authorization", manager))
                .andReturn().getResponse().getContentAsString();
        for (int i = 0; i < 2; i++) {
            patchAuth("/api/staff/projects/" + pc[0] + "/milestones/" + json(body, "$.milestones[" + i + "].id"),
                    manager, "{\"status\":\"DONE\"}").andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/projects").header("Authorization", pc[1]))
                .andExpect(jsonPath("$[0].progress").value(40))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"));
    }
}
