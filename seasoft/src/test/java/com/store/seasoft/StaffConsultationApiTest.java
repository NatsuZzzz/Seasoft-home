package com.store.seasoft;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Nhan vien quan ly lead: phan quyen, loc, doi trang thai, phan cong
class StaffConsultationApiTest extends ApiTestBase {

    String newLead() throws Exception {
        String email = uniqueEmail();
        String body = mockMvc.perform(post("/api/consultations").with(fromIp(randomIp()))
                        .contentType(MediaType.APPLICATION_JSON).content(ConsultationApiTest.leadJson(email)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json(body, "$.id");
    }

    ResultActions patchJson(String url, String auth, String json) throws Exception {
        return mockMvc.perform(patch(url).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    ResultActions setStatus(String id, String auth, String status) throws Exception {
        return patchJson("/api/staff/consultations/" + id, auth, "{\"status\":\"" + status + "\"}");
    }

    ResultActions assign(String id, String auth, String staffId) throws Exception {
        return patchJson("/api/staff/consultations/" + id + "/assign", auth,
                staffId == null ? "{\"staffId\":null}" : "{\"staffId\":\"" + staffId + "\"}");
    }

    @Test
    void TC_S01_customerForbidden_guestUnauthorized() throws Exception {
        String customer = bearer(registerOk(uniqueEmail()));
        mockMvc.perform(get("/api/staff/consultations").header("Authorization", customer))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/staff/consultations")).andExpect(status().isUnauthorized());
    }

    @Test
    void TC_S02_staffListsLeadsWithPaging() throws Exception {
        String staff = userWithRole("STAFF", uniqueEmail());
        newLead();
        newLead();
        mockMvc.perform(get("/api/staff/consultations?size=1").header("Authorization", staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(2)));
    }

    @Test
    void TC_S03_filterByStatusAndSearch() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String id = newLead();
        setStatus(id, manager, "CONTACTED").andExpect(status().isOk());
        mockMvc.perform(get("/api/staff/consultations?status=CONTACTED&q=trần thị").header("Authorization", manager))
                .andExpect(jsonPath("$.items[*].status", everyItem(is("CONTACTED"))))
                .andExpect(jsonPath("$.items[?(@.id=='" + id + "')]", hasSize(1)));
        mockMvc.perform(get("/api/staff/consultations?status=KHONG_CO").header("Authorization", manager))
                .andExpect(status().isBadRequest());
    }

    @Test
    void TC_S04_staffCannotEditUnassignedLead() throws Exception {
        String staff = userWithRole("STAFF", uniqueEmail());
        setStatus(newLead(), staff, "CONTACTED")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn chỉ được cập nhật lead do mình phụ trách"));
    }

    @Test
    void TC_S05_staffClaimsLeadThenUpdates() throws Exception {
        String email = uniqueEmail();
        String staff = userWithRole("STAFF", email);
        String id = newLead();
        assign(id, staff, userId(email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedStaff.email").value(email));
        setStatus(id, staff, "CONTACTED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONTACTED"));
        mockMvc.perform(get("/api/staff/consultations?mine=true").header("Authorization", staff))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void TC_S06_invalidTransition_returns409() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String id = newLead();
        setStatus(id, manager, "WON")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Không thể chuyển trạng thái từ NEW sang WON"));
    }

    @Test
    void TC_S07_fullFlow_andWonIsFinal_lostCanReopen() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String id = newLead();
        for (String s : new String[]{"CONTACTED", "QUOTED", "WON"}) {
            setStatus(id, manager, s).andExpect(status().isOk());
        }
        setStatus(id, manager, "LOST").andExpect(status().isConflict());

        String id2 = newLead();
        setStatus(id2, manager, "LOST").andExpect(status().isOk());
        setStatus(id2, manager, "CONTACTED").andExpect(status().isOk());
    }

    @Test
    void TC_S08_staffCannotTakeLeadOfAnotherStaff() throws Exception {
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();
        String staffA = userWithRole("STAFF", emailA);
        String staffB = userWithRole("STAFF", emailB);
        String id = newLead();
        assign(id, staffA, userId(emailA)).andExpect(status().isOk());
        assign(id, staffB, userId(emailB)).andExpect(status().isConflict());
        setStatus(id, staffB, "CONTACTED").andExpect(status().isForbidden());
    }

    @Test
    void TC_S09_staffCannotAssignToOthers() throws Exception {
        String staff = userWithRole("STAFF", uniqueEmail());
        String other = uniqueEmail();
        userWithRole("STAFF", other);
        assign(newLead(), staff, userId(other))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Nhân viên chỉ được tự nhận lead cho mình"));
    }

    @Test
    void TC_S10_managerAssignsAndUnassigns() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String staffEmail = uniqueEmail();
        userWithRole("STAFF", staffEmail);
        String id = newLead();
        assign(id, manager, userId(staffEmail)).andExpect(jsonPath("$.assignedStaff.email").value(staffEmail));
        assign(id, manager, null).andExpect(status().isOk()).andExpect(jsonPath("$.assignedStaff").doesNotExist());
    }

    @Test
    void TC_S11_cannotAssignToCustomerOrSuspendedStaff() throws Exception {
        String manager = userWithRole("MANAGER", uniqueEmail());
        String customer = uniqueEmail();
        registerOk(customer);
        String id = newLead();
        assign(id, manager, userId(customer)).andExpect(status().isBadRequest());

        String suspended = uniqueEmail();
        userWithRole("STAFF", suspended);
        sql("update users set status = 'SUSPENDED' where email = ?", suspended);
        assign(id, manager, userId(suspended)).andExpect(status().isBadRequest());
    }

    @Test
    void TC_S12_notFoundAndBadId() throws Exception {
        String staff = userWithRole("STAFF", uniqueEmail());
        mockMvc.perform(get("/api/staff/consultations/00000000-0000-0000-0000-000000000000").header("Authorization", staff))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/staff/consultations/khong-phai-uuid").header("Authorization", staff))
                .andExpect(status().isBadRequest());
    }

    @Test
    void TC_S13_internalNoteSaved() throws Exception {
        String manager = userWithRole("ADMIN", uniqueEmail());
        String id = newLead();
        patchJson("/api/staff/consultations/" + id, manager, "{\"internalNote\":\"Gọi lại chiều thứ 6\"}")
                .andExpect(jsonPath("$.internalNote").value("Gọi lại chiều thứ 6"))
                .andExpect(jsonPath("$.status").value("NEW"));
        mockMvc.perform(get("/api/staff/consultations/" + id).header("Authorization", manager))
                .andExpect(jsonPath("$.internalNote").value("Gọi lại chiều thứ 6"))
                .andExpect(jsonPath("$.phone").value("0901234567"));
    }

    @Test
    void TC_S14_assigneesListOnlyActiveStaff() throws Exception {
        String staffEmail = uniqueEmail();
        String staff = userWithRole("STAFF", staffEmail);
        String customer = uniqueEmail();
        registerOk(customer);
        mockMvc.perform(get("/api/staff/assignees").header("Authorization", staff))
                .andExpect(jsonPath("$[?(@.email=='" + staffEmail + "')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.email=='" + customer + "')]", hasSize(0)));
    }
}
