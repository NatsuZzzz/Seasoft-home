package com.store.seasoft;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Thong ke tong quan. Test tinh theo "chenh lech truoc/sau" de khong phu thuoc du lieu co san.
class AdminStatsApiTest extends ProjectTestSupport {

    String stats(String auth) throws Exception {
        em.flush(); // du lieu tao qua JPA chua flush thi SQL thong ke khong thay
        return mockMvc.perform(get("/api/admin/stats").header("Authorization", auth))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    long num(String body, String path) {
        Number n = com.jayway.jsonpath.JsonPath.read(body, path);
        return n.longValue();
    }

    String newLead(String status, String manager) throws Exception {
        String id = json(mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/consultations").with(fromIp(randomIp()))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(ConsultationApiTest.leadJson(uniqueEmail())))
                .andReturn().getResponse().getContentAsString(), "$.id");
        List<String> path = switch (status) {
            case "WON" -> List.of("CONTACTED", "QUOTED", "WON");
            case "LOST" -> List.of("LOST");
            default -> List.of();
        };
        for (String s : path) {
            patchAuth("/api/staff/consultations/" + id, manager, "{\"status\":\"" + s + "\"}");
        }
        return id;
    }

    @Test
    void TC_ST01_accessControl() throws Exception {
        mockMvc.perform(get("/api/admin/stats").header("Authorization", userWithRole("STAFF", uniqueEmail())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/stats").header("Authorization", bearer(registerOk(uniqueEmail()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/stats")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/stats").header("Authorization", userWithRole("MANAGER", uniqueEmail())))
                .andExpect(status().isOk());
    }

    @Test
    void TC_ST02_allKeysPresentEvenWhenZero() throws Exception {
        String body = stats(userWithRole("ADMIN", uniqueEmail()));
        for (String k : List.of("NEW", "CONTACTED", "QUOTED", "WON", "LOST")) {
            assertThat((Object) com.jayway.jsonpath.JsonPath.read(body, "$.leadsByStatus." + k)).isNotNull();
        }
        for (String k : List.of("PLANNING", "IN_PROGRESS", "ON_HOLD", "COMPLETED", "CANCELLED")) {
            assertThat((Object) com.jayway.jsonpath.JsonPath.read(body, "$.projectsByStatus." + k)).isNotNull();
        }
        for (String k : List.of("CUSTOMER", "STAFF", "MANAGER", "ADMIN")) {
            assertThat((Object) com.jayway.jsonpath.JsonPath.read(body, "$.usersByRole." + k)).isNotNull();
        }
    }

    @Test
    void TC_ST03_leadTotalsAndLast30Days() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String before = stats(admin);
        newLead("NEW", admin);
        newLead("NEW", admin);
        String after = stats(admin);
        assertThat(num(after, "$.leadsTotal") - num(before, "$.leadsTotal")).isEqualTo(2);
        assertThat(num(after, "$.leadsLast30Days") - num(before, "$.leadsLast30Days")).isEqualTo(2);
        assertThat(num(after, "$.leadsByStatus.NEW") - num(before, "$.leadsByStatus.NEW")).isEqualTo(2);
    }

    @Test
    void TC_ST04_leadsByStatusFollowsTransitions() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String before = stats(admin);
        newLead("WON", admin);
        newLead("LOST", admin);
        String after = stats(admin);
        assertThat(num(after, "$.leadsByStatus.WON") - num(before, "$.leadsByStatus.WON")).isEqualTo(1);
        assertThat(num(after, "$.leadsByStatus.LOST") - num(before, "$.leadsByStatus.LOST")).isEqualTo(1);
        assertThat(num(after, "$.leadsByStatus.NEW") - num(before, "$.leadsByStatus.NEW")).isZero();
    }

    @Test
    void TC_ST05_conversionRateFormula() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        newLead("WON", admin);
        newLead("WON", admin);
        newLead("WON", admin);
        newLead("LOST", admin);
        String body = stats(admin);
        long won = num(body, "$.leadsByStatus.WON");
        long lost = num(body, "$.leadsByStatus.LOST");
        double rate = ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.conversionRate")).doubleValue();
        assertThat(rate).isEqualTo(Math.round(won * 1000.0 / (won + lost)) / 10.0);
    }

    @Test
    void TC_ST06_eightWeeksEndingThisWeek() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        newLead("NEW", admin);
        String body = stats(admin);
        List<String> weeks = com.jayway.jsonpath.JsonPath.read(body, "$.leadsPerWeek[*].weekStart");
        assertThat(weeks).hasSize(8);
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        assertThat(weeks.get(7)).isEqualTo(monday.toString());
        assertThat(num(body, "$.leadsPerWeek[7].count")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void TC_ST07_projectsByStatusAndTotal() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String before = stats(admin);
        projectForNewCustomer(admin);
        String after = stats(admin);
        assertThat(num(after, "$.projectsTotal") - num(before, "$.projectsTotal")).isEqualTo(1);
        assertThat(num(after, "$.projectsByStatus.PLANNING") - num(before, "$.projectsByStatus.PLANNING")).isEqualTo(1);
    }

    @Test
    void TC_ST08_avgProgressInRange() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String pid = projectForNewCustomer(admin)[0];
        String mid = json(mockMvc.perform(get("/api/staff/projects/" + pid).header("Authorization", admin))
                .andReturn().getResponse().getContentAsString(), "$.milestones[0].id");
        patchAuth("/api/staff/projects/" + pid + "/milestones/" + mid, admin, "{\"status\":\"DONE\"}");
        double avg = ((Number) com.jayway.jsonpath.JsonPath.read(stats(admin), "$.avgProgress")).doubleValue();
        assertThat(avg).isBetween(0.0, 100.0);
    }

    @Test
    void TC_ST09_overdueProjects() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String email = uniqueEmail();
        registerOk(email);
        String before = stats(admin);
        String pid = json(postAuth("/api/staff/projects", admin, "{\"customerEmail\":\"" + email + "\",\"name\":\"Trễ\","
                + "\"serviceType\":\"OTHER\",\"dueDate\":\"2020-01-01\"}").andReturn().getResponse().getContentAsString(),
                "$.summary.id");
        String after = stats(admin);
        assertThat(num(after, "$.projectsOverdue") - num(before, "$.projectsOverdue")).isEqualTo(1);
        patchAuth("/api/staff/projects/" + pid, admin, "{\"status\":\"COMPLETED\"}");
        assertThat(num(stats(admin), "$.projectsOverdue")).isEqualTo(num(before, "$.projectsOverdue"));
    }

    @Test
    void TC_ST10_usersByRole() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String before = stats(admin);
        userWithRole("STAFF", uniqueEmail());
        registerOk(uniqueEmail());
        String after = stats(admin);
        assertThat(num(after, "$.usersByRole.STAFF") - num(before, "$.usersByRole.STAFF")).isEqualTo(1);
        assertThat(num(after, "$.usersByRole.CUSTOMER") - num(before, "$.usersByRole.CUSTOMER")).isEqualTo(1);
    }

    @Test
    void TC_ST11_leadsByService() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String before = stats(admin);
        newLead("NEW", admin); // leadJson dung LANDING_PAGE
        String after = stats(admin);
        assertThat(num(after, "$.leadsByService.LANDING_PAGE") - num(before, "$.leadsByService.LANDING_PAGE"))
                .isEqualTo(1);
    }
}
