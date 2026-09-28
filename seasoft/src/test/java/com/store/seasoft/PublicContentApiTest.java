package com.store.seasoft;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Noi dung cong khai + SEO (sitemap, robots)
class PublicContentApiTest extends ContentTestSupport {

    @Test
    void TC_PC01_seededPortfolioVisibleInOrder() throws Exception {
        mockMvc.perform(get("/api/public/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[0].slug").value("he-thong-website-doanh-nghiep-vintech-corp"))
                .andExpect(jsonPath("$[1].clientName").value("Marina Bay"))
                .andExpect(jsonPath("$[2].category").value("E-Commerce Platform"));
    }

    @Test
    void TC_PC02_unpublishedPortfolioHidden() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        send("POST", "/api/admin/content/portfolio", admin, "{\"title\":\"Dự án bí mật\",\"published\":false}");
        mockMvc.perform(get("/api/public/portfolio"))
                .andExpect(jsonPath("$[?(@.title=='Dự án bí mật')]", hasSize(0)));
    }

    @Test
    void TC_PC03_blogListOnlyPublished() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        createPost(admin, "Bài nháp PC03", "DRAFT");
        createPost(admin, "Bài đăng PC03", "PUBLISHED");
        mockMvc.perform(get("/api/public/blog?size=50"))
                .andExpect(jsonPath("$.items[?(@.title=='Bài nháp PC03')]", hasSize(0)))
                .andExpect(jsonPath("$.items[?(@.title=='Bài đăng PC03')]", hasSize(1)))
                .andExpect(jsonPath("$.items[0].content").doesNotExist());
    }

    @Test
    void TC_PC04_postDetailBySlug_draftIs404() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String pub = json(createPost(admin, "Thiết kế web chuẩn SEO", "PUBLISHED"), "$.summary.slug");
        String draft = json(createPost(admin, "Bản nháp chưa đăng", "DRAFT"), "$.summary.slug");
        mockMvc.perform(get("/api/public/blog/" + pub))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", containsString("## Mục 2")))
                .andExpect(jsonPath("$.summary.publishedAt").isNotEmpty());
        mockMvc.perform(get("/api/public/blog/" + draft)).andExpect(status().isNotFound());
    }

    @Test
    void TC_PC05_unknownSlug_404() throws Exception {
        mockMvc.perform(get("/api/public/blog/khong-co-bai-nay"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy bài viết"));
    }

    @Test
    void TC_PC06_testimonialsOnlyPublished() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        send("POST", "/api/admin/content/testimonials", admin,
                "{\"customerName\":\"Anh A\",\"quote\":\"Rất hài lòng\",\"rating\":5,\"published\":true}");
        send("POST", "/api/admin/content/testimonials", admin,
                "{\"customerName\":\"Chị B\",\"quote\":\"Chưa duyệt\",\"published\":false}");
        mockMvc.perform(get("/api/public/testimonials"))
                .andExpect(jsonPath("$[?(@.customerName=='Anh A')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.customerName=='Chị B')]", hasSize(0)));
    }

    @Test
    void TC_PC07_publicIsReadOnly() throws Exception {
        mockMvc.perform(get("/api/public/portfolio")).andExpect(status().isOk());
        mockMvc.perform(post("/api/public/portfolio")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/content/portfolio")).andExpect(status().isUnauthorized());
    }

    @Test
    void TC_PC08_sitemapListsPagesAndPublishedPostsOnly() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String pub = json(createPost(admin, "Bài cho sitemap", "PUBLISHED"), "$.summary.slug");
        String draft = json(createPost(admin, "Nháp không vào sitemap", "DRAFT"), "$.summary.slug");
        em.flush();
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andExpect(content().string(containsString("/Page.html</loc>")))
                .andExpect(content().string(containsString("/blog.html</loc>")))
                .andExpect(content().string(containsString("post.html?slug=" + pub)))
                .andExpect(content().string(not(containsString(draft))));
    }

    @Test
    void TC_PC09_robotsBlocksPrivatePages() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Disallow: /admin.html")))
                .andExpect(content().string(containsString("Disallow: /api/")))
                .andExpect(content().string(containsString("Sitemap: ")));
    }

    @Test
    void TC_PC10_blogPaging() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        createPost(admin, "Trang 1", "PUBLISHED");
        createPost(admin, "Trang 2", "PUBLISHED");
        mockMvc.perform(get("/api/public/blog?size=1"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(2)));
    }
}
