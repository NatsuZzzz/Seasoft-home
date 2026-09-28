package com.store.seasoft;

import com.jayway.jsonpath.JsonPath;
import com.store.seasoft.Service.MailService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Moi test chay trong transaction va rollback sau khi xong -> DB test luon sach
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class ApiTestBase {

    protected static final String PASSWORD = "Secret123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @MockitoBean
    protected MailService mailService;

    @PersistenceContext
    protected EntityManager em;

    // Chay SQL truc tiep: flush JPA truoc, clear cache sau de lan doc tiep theo thay du lieu moi
    protected void sql(String statement, Object... args) {
        em.flush();
        jdbc.update(statement, args);
        em.clear();
    }

    protected <T> T queryOne(String query, Class<T> type, Object... args) {
        em.flush();
        return jdbc.queryForObject(query, type, args);
    }

    protected static String uniqueEmail() {
        return "u" + UUID.randomUUID().toString().substring(0, 8) + "@seasoft.test";
    }

    protected ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions register(String fullName, String email, String phone, String password) throws Exception {
        String phoneJson = phone == null ? "null" : "\"" + phone + "\"";
        return postJson("/api/auth/register", """
                {"fullName":"%s","email":"%s","phone":%s,"password":"%s"}"""
                .formatted(fullName, email, phoneJson, password));
    }

    protected ResultActions login(String email, String password) throws Exception {
        return postJson("/api/auth/login", """
                {"email":"%s","password":"%s"}""".formatted(email, password));
    }

    // Dang ky user moi, tra ve body JSON (co token + refreshToken)
    protected String registerOk(String email) throws Exception {
        return register("Test User", email, null, PASSWORD)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    protected static String json(String body, String path) {
        return JsonPath.read(body, path);
    }

    protected static String bearer(String body) {
        return "Bearer " + json(body, "$.token");
    }
}
