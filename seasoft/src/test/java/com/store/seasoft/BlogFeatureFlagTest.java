package com.store.seasoft;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// FEATURE_BLOG=false: blog cong khai bi an hoan toan, phan con lai cua web khong anh huong
@TestPropertySource(properties = "app.features.blog=false")
class BlogFeatureFlagTest extends ContentTestSupport {

    @Test
    void TC_BF01_blogPageRedirectsHome() throws Exception {
        mockMvc.perform(get("/blog.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/Page.html"));
    }

    @Test
    void TC_BF02_postPageRedirectsHome() throws Exception {
        mockMvc.perform(get("/post.html").param("slug", "bat-ky"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/Page.html"));
    }

    @Test
    void TC_BF03_publicBlogListIs404Json() throws Exception {
        mockMvc.perform(get("/api/public/blog"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void TC_BF04_publishedPostDetailIs404() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String slug = json(createPost(admin, "Bài đã đăng nhưng blog tắt", "PUBLISHED"), "$.summary.slug");
        em.flush();
        mockMvc.perform(get("/api/public/blog/" + slug)).andExpect(status().isNotFound());
    }

    @Test
    void TC_BF05_sitemapHasNoBlog() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        String slug = json(createPost(admin, "Bài không vào sitemap", "PUBLISHED"), "$.summary.slug");
        em.flush();
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/Page.html</loc>")))
                .andExpect(content().string(not(containsString("blog.html"))))
                .andExpect(content().string(not(containsString(slug))));
    }

    @Test
    void TC_BF06_adminCanStillWriteAndListPosts() throws Exception {
        String admin = userWithRole("ADMIN", uniqueEmail());
        createPost(admin, "Soạn sẵn chờ bật blog", "DRAFT");
        em.flush();
        mockMvc.perform(get("/api/admin/content/blog").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Soạn sẵn chờ bật blog")));
    }

    @Test
    void TC_BF07_otherPublicContentUnaffected() throws Exception {
        mockMvc.perform(get("/api/public/portfolio")).andExpect(status().isOk());
        mockMvc.perform(get("/api/public/testimonials")).andExpect(status().isOk());
    }

    @Test
    void TC_BF08_homeAndSeoStillServed() throws Exception {
        mockMvc.perform(get("/Page.html")).andExpect(status().isOk());
        mockMvc.perform(get("/robots.txt")).andExpect(status().isOk());
    }
}
