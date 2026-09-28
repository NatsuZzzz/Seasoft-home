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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
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

    // Tao user moi voi role cho truoc (STAFF/MANAGER/ADMIN/CUSTOMER), tra ve "Bearer <token>"
    protected String userWithRole(String role, String email) throws Exception {
        registerOk(email);
        sql("update users set role_id = (select id from roles where code = ?) where email = ?", role, email);
        return bearer(login(email, PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    protected String userId(String email) {
        return queryOne("select id::text from users where email = ?", String.class, email);
    }

    // Gia lap request den tu IP khac (rate limit tinh theo IP)
    protected static RequestPostProcessor fromIp(String ip) {
        return req -> {
            req.setRemoteAddr(ip);
            return req;
        };
    }

    protected static String randomIp() {
        java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
        return "10." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(1, 255);
    }

    protected static String json(String body, String path) {
        return JsonPath.read(body, path);
    }

    protected static String bearer(String body) {
        return "Bearer " + json(body, "$.token");
    }
}
