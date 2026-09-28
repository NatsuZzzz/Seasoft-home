package com.store.seasoft.Controller;

import com.store.seasoft.Model.BlogPost;
import com.store.seasoft.Service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

// sitemap.xml + robots.txt sinh dong theo domain that (app.frontend-url)
@RestController
@RequiredArgsConstructor
public class SeoController {

    private final ContentService contentService;

    @Value("${app.frontend-url}")
    private String baseUrl;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """);
        url(xml, "/Page.html", null, "weekly", "1.0");
        url(xml, "/blog.html", null, "weekly", "0.8");
        for (BlogPost p : contentService.postsForSitemap()) {
            String lastmod = DateTimeFormatter.ISO_LOCAL_DATE.format(p.getUpdatedAt().atOffset(ZoneOffset.UTC));
            url(xml, "/post.html?slug=" + p.getSlug(), lastmod, "monthly", "0.6");
        }
        return xml.append("</urlset>\n").toString();
    }

    // Chan bot vao trang can dang nhap / trang quan tri
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return """
                User-agent: *
                Allow: /
                Disallow: /admin.html
                Disallow: /dashboard.html
                Disallow: /project.html
                Disallow: /profile.html
                Disallow: /reset-password.html
                Disallow: /api/

                Sitemap: %s/sitemap.xml
                """.formatted(baseUrl);
    }

    private void url(StringBuilder xml, String path, String lastmod, String freq, String priority) {
        xml.append("  <url><loc>").append(HtmlUtils.htmlEscape(baseUrl + path)).append("</loc>");
        if (lastmod != null) {
            xml.append("<lastmod>").append(lastmod).append("</lastmod>");
        }
        xml.append("<changefreq>").append(freq).append("</changefreq><priority>").append(priority)
                .append("</priority></url>\n");
    }
}
