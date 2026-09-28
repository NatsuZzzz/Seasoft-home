package com.store.seasoft;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tien ich chung cho test du an: tao lead da chot (WON), tao du an
abstract class ProjectTestSupport extends ApiTestBase {

    ResultActions postAuth(String url, String auth, String json) throws Exception {
        return mockMvc.perform(post(url).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    ResultActions patchAuth(String url, String auth, String json) throws Exception {
        return mockMvc.perform(patch(url).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // Lead cua email cho truoc (customerAuth != null -> gui khi dang dang nhap), chuyen toi WON
    String wonLead(String managerAuth, String email, String customerAuth) throws Exception {
        var rb = post("/api/consultations").with(fromIp(randomIp()))
                .contentType(MediaType.APPLICATION_JSON).content(ConsultationApiTest.leadJson(email));
        if (customerAuth != null) {
            rb.header("Authorization", customerAuth);
        }
        String id = json(mockMvc.perform(rb).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString(), "$.id");
        for (String s : new String[]{"CONTACTED", "QUOTED", "WON"}) {
            patchAuth("/api/staff/consultations/" + id, managerAuth, "{\"status\":\"" + s + "\"}")
                    .andExpect(status().isOk());
        }
        return id;
    }

    ResultActions createFromLead(String auth, String leadId) throws Exception {
        return postAuth("/api/staff/projects", auth,
                "{\"consultationId\":\"" + leadId + "\",\"name\":\"Website ACME\"}");
    }

    // Tao nhanh du an cho khach: tra ve [projectId, customerAuth]
    String[] projectForNewCustomer(String managerAuth) throws Exception {
        String email = uniqueEmail();
        String customerAuth = bearer(registerOk(email));
        String leadId = wonLead(managerAuth, email, customerAuth);
        String body = createFromLead(managerAuth, leadId).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new String[]{json(body, "$.summary.id"), customerAuth};
    }
}
