package com.store.seasoft;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Quan ly noi dung (MANAGER/ADMIN)
class AdminContentApiTest extends ContentTestSupport {

    @Test
    void TC_AC01_accessControl() throws Exception {
        mockMvc.perform(get("/api/admin/content/blog").header("Authorization", userWithRole("STAFF", uniqueEmail())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/content/blog").header("Authorization", bearer(registerOk(uniqueEmail()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/content/blog").header("Authorization", userWithRole("MANAGER", uniqueEmail())))
                .andExpect(status().isOk());
    }

    @Test
    void TC_AC02_autoSlugFromVietnameseTitle() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        send("POST", "/api/admin/content/portfolio", admin, "{\"title\":\"Thiết kế Website Đẹp & Nhanh!\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("thiet-ke-website-dep-nhanh"))
                .andExpect(jsonPath("$.published").value(false));
    }

    @Test
    void TC_AC03_slugConflicts() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        send("POST", "/api/admin/content/portfolio", admin, "{\"title\":\"Trùng Tên\"}");
        send("POST", "/api/admin/content/portfolio", admin, "{\"title\":\"Trùng Tên\"}")
                .andExpect(jsonPath("$.slug").value("trung-ten-2"));
        send("POST", "/api/admin/content/portfolio", admin, "{\"title\":\"Khác\",\"slug\":\"trung-ten\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void TC_AC04_rejectDangerousUrlAndBadSlug() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        send("POST", "/api/admin/content/portfolio", admin,
                "{\"title\":\"X\",\"projectUrl\":\"javascript:alert(1)\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.projectUrl").value("Link phải bắt đầu bằng http:// hoặc https://"));
        send("POST", "/api/admin/content/blog", admin,
                "{\"title\":\"X\",\"content\":\"Y\",\"coverImageUrl\":\"data:image/png;base64,AAA\"}")
                .andExpect(status().isBadRequest());
        send("POST", "/api/admin/content/portfolio", admin, "{\"title\":\"X\",\"slug\":\"Có Dấu\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.slug").exists());
    }

    @Test
    void TC_AC05_updateAndPublishPortfolio() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String id = json(send("POST", "/api/admin/content/portfolio", admin, "{\"title\":\"Dự án AC05\"}")
                .andReturn().getResponse().getContentAsString(), "$.id");
        send("PUT", "/api/admin/content/portfolio/" + id, admin,
                "{\"title\":\"Dự án AC05 mới\",\"published\":true,\"sortOrder\":99,\"projectUrl\":\"https://acme.vn\"}")
                .andExpect(jsonPath("$.title").value("Dự án AC05 mới"))
                .andExpect(jsonPath("$.slug").value("du-an-ac05-moi"));
        mockMvc.perform(get("/api/public/portfolio"))
                .andExpect(jsonPath("$[?(@.title=='Dự án AC05 mới')]", hasSize(1)));
    }

    @Test
    void TC_AC06_deletePortfolio() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String id = json(send("POST", "/api/admin/content/portfolio", admin, "{\"title\":\"Xoá tôi\"}")
                .andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(delete("/api/admin/content/portfolio/" + id).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/admin/content/portfolio/" + id).header("Authorization", admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void TC_AC07_publishLifecycle() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String body = createPost(admin, "Vòng đời bài viết", "DRAFT");
        String id = json(body, "$.summary.id");
        String slug = json(body, "$.summary.slug");
        org.assertj.core.api.Assertions.assertThat((Object) com.jayway.jsonpath.JsonPath.read(body, "$.summary.publishedAt")).isNull();
        String published = send("PUT", "/api/admin/content/blog/" + id, admin,
                "{\"title\":\"Vòng đời bài viết\",\"content\":\"Nội dung\",\"status\":\"PUBLISHED\"}")
                .andReturn().getResponse().getContentAsString();
        String firstDate = json(published, "$.summary.publishedAt");
        mockMvc.perform(get("/api/public/blog/" + slug)).andExpect(status().isOk());
        send("PUT", "/api/admin/content/blog/" + id, admin,
                "{\"title\":\"Vòng đời bài viết\",\"content\":\"Nội dung\",\"status\":\"DRAFT\"}")
                .andExpect(jsonPath("$.summary.publishedAt").value(firstDate));
        mockMvc.perform(get("/api/public/blog/" + slug)).andExpect(status().isNotFound());
    }

    @Test
    void TC_AC08_authorIsCreator() throws Exception {
        String email = uniqueEmail();
        String manager = userWithRole("MANAGER", email);
        String body = createPost(manager, "Bài của quản lý", "PUBLISHED");
        org.assertj.core.api.Assertions.assertThat(json(body, "$.summary.authorName")).isEqualTo("Test User");
    }

    @Test
    void TC_AC09_blogValidation() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        send("POST", "/api/admin/content/blog", admin, "{\"title\":\"Thiếu nội dung\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.content").exists());
        send("POST", "/api/admin/content/blog", admin, "{\"title\":\"\",\"content\":\"x\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void TC_AC10_testimonialRatingRange() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        send("POST", "/api/admin/content/testimonials", admin, "{\"customerName\":\"A\",\"quote\":\"Q\",\"rating\":6}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.rating").value("Điểm từ 1 đến 5"));
        send("POST", "/api/admin/content/testimonials", admin, "{\"customerName\":\"A\",\"quote\":\"Q\",\"rating\":0}")
                .andExpect(status().isBadRequest());
        send("POST", "/api/admin/content/testimonials", admin, "{\"customerName\":\"A\",\"quote\":\"Q\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void TC_AC11_updateAndDeleteTestimonial() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String id = json(send("POST", "/api/admin/content/testimonials", admin,
                "{\"customerName\":\"Khách AC11\",\"quote\":\"Ổn\"}").andReturn().getResponse().getContentAsString(), "$.id");
        send("PUT", "/api/admin/content/testimonials/" + id, admin,
                "{\"customerName\":\"Khách AC11\",\"quote\":\"Rất ổn\",\"rating\":4,\"published\":true}")
                .andExpect(jsonPath("$.quote").value("Rất ổn")).andExpect(jsonPath("$.published").value(true));
        mockMvc.perform(delete("/api/admin/content/testimonials/" + id).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/public/testimonials"))
                .andExpect(jsonPath("$[?(@.customerName=='Khách AC11')]", hasSize(0)));
    }

    @Test
    void TC_AC12_adminListIncludesDrafts() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        createPost(admin, "Nháp AC12", "DRAFT");
        mockMvc.perform(get("/api/admin/content/blog?size=100").header("Authorization", admin))
                .andExpect(jsonPath("$.items[?(@.title=='Nháp AC12')].status").value("DRAFT"));
        mockMvc.perform(get("/api/admin/content/blog/00000000-0000-0000-0000-000000000000").header("Authorization", admin))
                .andExpect(status().isNotFound());
    }
}
