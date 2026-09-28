package com.store.seasoft;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StaffProjectApiTest extends ProjectTestSupport {

    @Test
    void TC_P01_createFromWonLead_defaultsAndMail() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String email = uniqueEmail();
        String customer = bearer(registerOk(email));
        String lead = wonLead(manager, email, customer);
        createFromLead(manager, lead)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.summary.code", matchesPattern("SS-\\d{4}-\\d{4}")))
                .andExpect(jsonPath("$.summary.status").value("PLANNING"))
                .andExpect(jsonPath("$.summary.progress").value(0))
                .andExpect(jsonPath("$.summary.customerEmail").value(email))
                .andExpect(jsonPath("$.summary.serviceType").value("LANDING_PAGE"))
                .andExpect(jsonPath("$.milestones", hasSize(5)))
                .andExpect(jsonPath("$.milestones[0].title").value("Tư vấn & khảo sát"))
                .andExpect(jsonPath("$.consultationId").value(lead));
        verify(mailService).send(eq(email), contains("đã được khởi tạo"), anyString());
    }

    @Test
    void TC_P02_leadNotWon_returns409() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String id = json(postAuth("/api/consultations", manager, ConsultationApiTest.leadJson(uniqueEmail()))
                .andReturn().getResponse().getContentAsString(), "$.id");
        createFromLead(manager, id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Chỉ tạo dự án từ lead đã chốt (Thành công)"));
    }

    @Test
    void TC_P03_sameLeadTwice_returns409() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String email = uniqueEmail();
        String lead = wonLead(manager, email, bearer(registerOk(email)));
        createFromLead(manager, lead).andExpect(status().isCreated());
        createFromLead(manager, lead).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Lead này đã có dự án"));
    }

    @Test
    void TC_P04_guestLead_matchedByEmail_orRejectedWithoutAccount() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String registered = uniqueEmail();
        registerOk(registered);
        createFromLead(manager, wonLead(manager, registered, null)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.summary.customerEmail").value(registered));

        String noAccount = uniqueEmail();
        createFromLead(manager, wonLead(manager, noAccount, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", matchesPattern(".*chưa có tài khoản.*")));
    }

    @Test
    void TC_P05_staffOnlyFromOwnLead() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String staffEmail = uniqueEmail();
        String staff = userWithRole("STAFF", staffEmail);
        String email = uniqueEmail();
        String lead = wonLead(manager, email, bearer(registerOk(email)));
        createFromLead(staff, lead).andExpect(status().isForbidden());

        patchAuth("/api/staff/consultations/" + lead + "/assign", manager,
                "{\"staffId\":\"" + userId(staffEmail) + "\"}").andExpect(status().isOk());
        createFromLead(staff, lead).andExpect(status().isCreated())
                .andExpect(jsonPath("$.summary.manager.email").value(staffEmail));
    }

    @Test
    void TC_P06_createByCustomerEmail_managerOnly_needsServiceType() throws Exception {
        String manager = userWithRole("ADMIN", uniqueEmail());
        String staff = userWithRole("STAFF", uniqueEmail());
        String email = uniqueEmail();
        registerOk(email);
        String noService = "{\"customerEmail\":\"" + email + "\",\"name\":\"Bảo trì\"}";
        postAuth("/api/staff/projects", manager, noService).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vui lòng chọn loại dịch vụ"));
        String ok = "{\"customerEmail\":\"" + email + "\",\"name\":\"Bảo trì\",\"serviceType\":\"MAINTENANCE\","
                + "\"milestones\":[{\"title\":\"Rà soát\"},{\"title\":\"Nâng cấp\"}]}";
        postAuth("/api/staff/projects", staff, ok).andExpect(status().isForbidden());
        postAuth("/api/staff/projects", manager, ok).andExpect(status().isCreated())
                .andExpect(jsonPath("$.milestones", hasSize(2)));
    }

    @Test
    void TC_P07_invalidDatesAndBlankName_return400() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String email = uniqueEmail();
        registerOk(email);
        postAuth("/api/staff/projects", manager, "{\"customerEmail\":\"" + email + "\",\"name\":\"X\","
                + "\"serviceType\":\"OTHER\",\"startDate\":\"2026-10-10\",\"dueDate\":\"2026-10-01\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Hạn hoàn thành phải sau ngày bắt đầu"));
        postAuth("/api/staff/projects", manager, "{\"customerEmail\":\"" + email + "\",\"name\":\"\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void TC_P08_milestoneDone_progressAndAutoInProgress() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String pid = projectForNewCustomer(manager)[0];
        String mid = json(mockMvc.perform(get("/api/staff/projects/" + pid).header("Authorization", manager))
                .andReturn().getResponse().getContentAsString(), "$.milestones[0].id");
        patchAuth("/api/staff/projects/" + pid + "/milestones/" + mid, manager, "{\"status\":\"DONE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt", notNullValue()));
        mockMvc.perform(get("/api/staff/projects/" + pid).header("Authorization", manager))
                .andExpect(jsonPath("$.summary.progress").value(20))
                .andExpect(jsonPath("$.summary.status").value("IN_PROGRESS"));
        patchAuth("/api/staff/projects/" + pid + "/milestones/" + mid, manager, "{\"status\":\"TODO\"}")
                .andExpect(jsonPath("$.completedAt").doesNotExist());
    }

    @Test
    void TC_P09_addAndDeleteMilestone() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String pid = projectForNewCustomer(manager)[0];
        String mid = json(postAuth("/api/staff/projects/" + pid + "/milestones", manager,
                        "{\"title\":\"Đào tạo sử dụng\",\"dueDate\":\"2026-12-01\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sortOrder").value(6))
                .andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(delete("/api/staff/projects/" + pid + "/milestones/" + mid).header("Authorization", manager))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/staff/projects/" + pid).header("Authorization", manager))
                .andExpect(jsonPath("$.milestones", hasSize(5)));
        postAuth("/api/staff/projects/" + pid + "/milestones", manager, "{\"title\":\"\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void TC_P10_onlyProjectManagerOrManagerRoleCanEdit() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String otherStaff = userWithRole("STAFF", uniqueEmail());
        String pid = projectForNewCustomer(manager)[0];
        patchAuth("/api/staff/projects/" + pid, otherStaff, "{\"status\":\"ON_HOLD\"}")
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/staff/projects/" + pid).header("Authorization", otherStaff))
                .andExpect(status().isOk()); // van xem duoc
        patchAuth("/api/staff/projects/" + pid, manager, "{\"status\":\"ON_HOLD\",\"name\":\"Tên mới\"}")
                .andExpect(jsonPath("$.summary.status").value("ON_HOLD"))
                .andExpect(jsonPath("$.summary.name").value("Tên mới"));
    }

    @Test
    void TC_P11_updates_hiddenVsVisible_mailOnlyWhenVisible() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String[] pc = projectForNewCustomer(manager);
        postAuth("/api/staff/projects/" + pc[0] + "/updates", manager,
                "{\"content\":\"Ghi chú nội bộ\",\"visibleToCustomer\":false}").andExpect(status().isCreated());
        verify(mailService, never()).send(anyString(), contains("Cập nhật dự án"), anyString());
        postAuth("/api/staff/projects/" + pc[0] + "/updates", manager, "{\"content\":\"Đã xong wireframe\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.visibleToCustomer").value(true));
        verify(mailService).send(anyString(), contains("Cập nhật dự án"), contains("Đã xong wireframe"));
        mockMvc.perform(get("/api/staff/projects/" + pc[0]).header("Authorization", manager))
                .andExpect(jsonPath("$.updates", hasSize(2)));
        mockMvc.perform(get("/api/projects/" + pc[0]).header("Authorization", pc[1]))
                .andExpect(jsonPath("$.updates", hasSize(1)));
    }

    @Test
    void TC_P12_searchFilterAndMine() throws Exception {
        String managerEmail = uniqueEmail();
        String manager = userWithRole("MANAGER", managerEmail);
        String pid = projectForNewCustomer(manager)[0];
        String code = json(mockMvc.perform(get("/api/staff/projects/" + pid).header("Authorization", manager))
                .andReturn().getResponse().getContentAsString(), "$.summary.code");
        mockMvc.perform(get("/api/staff/projects?q=" + code).header("Authorization", manager))
                .andExpect(jsonPath("$.items", hasSize(1)));
        mockMvc.perform(get("/api/staff/projects?status=COMPLETED&q=" + code).header("Authorization", manager))
                .andExpect(jsonPath("$.items", hasSize(0)));
        mockMvc.perform(get("/api/staff/projects?mine=true").header("Authorization", manager))
                .andExpect(jsonPath("$.items[0].manager.email").value(managerEmail));
    }

    @Test
    void TC_P13_customerCannotUseStaffApi() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String[] pc = projectForNewCustomer(manager);
        mockMvc.perform(get("/api/staff/projects").header("Authorization", pc[1])).andExpect(status().isForbidden());
        patchAuth("/api/staff/projects/" + pc[0], pc[1], "{\"status\":\"COMPLETED\"}").andExpect(status().isForbidden());
    }

    @Test
    void TC_P14_staffCannotReassignManager() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String staffEmail = uniqueEmail();
        String staff = userWithRole("STAFF", staffEmail);
        String other = uniqueEmail();
        userWithRole("STAFF", other);
        String pid = projectForNewCustomer(admin)[0];
        patchAuth("/api/staff/projects/" + pid, admin, "{\"managerId\":\"" + userId(staffEmail) + "\"}")
                .andExpect(jsonPath("$.summary.manager.email").value(staffEmail));
        patchAuth("/api/staff/projects/" + pid, staff, "{\"managerId\":\"" + userId(other) + "\"}")
                .andExpect(status().isForbidden());
        patchAuth("/api/staff/projects/" + pid, staff, "{\"status\":\"IN_PROGRESS\"}").andExpect(status().isOk());
    }
}
