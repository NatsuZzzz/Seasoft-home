package com.store.seasoft;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Form "Nhan tu van" cong khai + khach xem yeu cau cua minh
class ConsultationApiTest extends ApiTestBase {

    static String leadJson(String email) {
        return """
                {"fullName":"Trần Thị B","email":"%s","phone":"0901234567","companyName":"ACME",
                 "serviceType":"LANDING_PAGE","budgetRange":"FROM_20M_TO_50M","message":"Cần landing page"}"""
                .formatted(email);
    }

    ResultActions submit(String json, String ip, String auth) throws Exception {
        var rb = post("/api/consultations").contentType(MediaType.APPLICATION_JSON).content(json).with(fromIp(ip));
        if (auth != null) {
            rb.header("Authorization", auth);
        }
        return mockMvc.perform(rb);
    }

    ResultActions submit(String json) throws Exception {
        return submit(json, randomIp(), null);
    }

    long countByEmail(String email) {
        return queryOne("select count(*) from consultation_requests where email = ?", Long.class, email);
    }

    @Test
    void TC_C01_guestSubmit_created() throws Exception {
        String email = uniqueEmail();
        submit(leadJson(email), "10.1.1.1", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty());
        assertThat(queryOne("select status from consultation_requests where email = ?", String.class, email))
                .isEqualTo("NEW");
        assertThat(queryOne("select ip_address from consultation_requests where email = ?", String.class, email))
                .isEqualTo("10.1.1.1");
        assertThat(queryOne("select customer_id is null from consultation_requests where email = ?", Boolean.class, email))
                .isTrue();
    }

    @Test
    void TC_C02_loggedInCustomer_linkedAndVisibleInMine() throws Exception {
        String email = uniqueEmail();
        String auth = bearer(registerOk(email));
        submit(leadJson(email), randomIp(), auth).andExpect(status().isCreated());
        mockMvc.perform(get("/api/consultations/mine").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].serviceType").value("LANDING_PAGE"))
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    @Test
    void TC_C03_missingRequired_returnsFieldErrors() throws Exception {
        submit("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phone").exists())
                .andExpect(jsonPath("$.errors.serviceType").value("Vui lòng chọn dịch vụ"));
    }

    @Test
    void TC_C04_invalidEmail_returns400() throws Exception {
        submit(leadJson("khong-phai-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("Email không hợp lệ"));
    }

    @Test
    void TC_C05_invalidPhone_returns400() throws Exception {
        submit(leadJson(uniqueEmail()).replace("0901234567", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").value("Số điện thoại không hợp lệ"));
    }

    @Test
    void TC_C06_unknownServiceType_returns400() throws Exception {
        submit(leadJson(uniqueEmail()).replace("LANDING_PAGE", "HACK"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void TC_C07_budgetDefaultsToUndecided() throws Exception {
        String email = uniqueEmail();
        submit(leadJson(email).replace("\"budgetRange\":\"FROM_20M_TO_50M\",", "")).andExpect(status().isCreated());
        assertThat(queryOne("select budget_range from consultation_requests where email = ?", String.class, email))
                .isEqualTo("UNDECIDED");
    }

    @Test
    void TC_C08_honeypotFilled_fakeSuccessButNotSaved() throws Exception {
        String email = uniqueEmail();
        submit(leadJson(email).replace("\"message\"", "\"website\":\"http://spam.bot\",\"message\""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").doesNotExist());
        assertThat(countByEmail(email)).isZero();
    }

    @Test
    void TC_C09_rateLimit_sixthRequestFromSameIp_returns429() throws Exception {
        String ip = randomIp();
        for (int i = 0; i < 5; i++) {
            submit(leadJson(uniqueEmail()), ip, null).andExpect(status().isCreated());
        }
        submit(leadJson(uniqueEmail()), ip, null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
        // IP khac khong bi anh huong
        submit(leadJson(uniqueEmail()), randomIp(), null).andExpect(status().isCreated());
    }

    @Test
    void TC_C10_emailNormalized() throws Exception {
        String email = uniqueEmail();
        submit(leadJson("  " + email.toUpperCase() + " ")).andExpect(status().isCreated());
        assertThat(countByEmail(email)).isEqualTo(1);
    }

    @Test
    void TC_C11_confirmationMailSentToRequester() throws Exception {
        String email = uniqueEmail();
        submit(leadJson(email)).andExpect(status().isCreated());
        verify(mailService).send(eq(email), contains("đã nhận yêu cầu tư vấn"), anyString());
    }

    @Test
    void TC_C12_mine_requiresLogin_andShowsOnlyOwn() throws Exception {
        mockMvc.perform(get("/api/consultations/mine")).andExpect(status().isUnauthorized());

        String a = uniqueEmail();
        String b = uniqueEmail();
        String authA = bearer(registerOk(a));
        String authB = bearer(registerOk(b));
        submit(leadJson(a), randomIp(), authA);
        submit(leadJson(b), randomIp(), authB);
        submit(leadJson(b), randomIp(), authB);
        mockMvc.perform(get("/api/consultations/mine").header("Authorization", authA))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void TC_C13_messageTooLong_returns400() throws Exception {
        submit(leadJson(uniqueEmail()).replace("Cần landing page", "x".repeat(2001)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.message").exists());
    }

    @Test
    void TC_C14_customerViewHidesInternalFields() throws Exception {
        String email = uniqueEmail();
        String auth = bearer(registerOk(email));
        submit(leadJson(email), randomIp(), auth);
        sql("update consultation_requests set internal_note = 'khach kho tinh' where email = ?", email);
        mockMvc.perform(get("/api/consultations/mine").header("Authorization", auth))
                .andExpect(jsonPath("$[0].internalNote").doesNotExist())
                .andExpect(jsonPath("$[0].ipAddress").doesNotExist())
                .andExpect(jsonPath("$[0].phone").doesNotExist());
    }
}
