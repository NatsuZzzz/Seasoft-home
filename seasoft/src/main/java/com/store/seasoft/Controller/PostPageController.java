package com.store.seasoft.Controller;

import com.store.seasoft.Config.ApiException;
import com.store.seasoft.Dto.ContentDtos.BlogDetail;
import com.store.seasoft.Dto.ContentDtos.BlogSummary;
import com.store.seasoft.Service.BlogRenderer;
import com.store.seasoft.Service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.store.seasoft.Service.BlogRenderer.esc;

// Server render san noi dung + meta cho post.html -> Google / Facebook doc duoc ma khong can chay JS.
// JS trong trang van chay binh thuong va render lai y het (cung quy tac BlogRenderer / Blog.render).
@RestController
@RequiredArgsConstructor
public class PostPageController {

    private final ContentService contentService;
    private final ResourceLoader resourceLoader;

    @Value("${spring.web.resources.static-locations}")
    private String[] staticLocations;

    @Value("${app.frontend-url}")
    private String baseUrl;

    @GetMapping(value = "/post.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> post(@RequestParam(required = false) String slug) throws IOException {
        String html = template();
        if (slug == null || slug.isBlank()) {
            return ResponseEntity.ok(html);
        }
        BlogDetail d;
        try {
            d = contentService.publishedPost(slug);
        } catch (ApiException e) {
            // Bai khong ton tai / ban nhap: 404 that + noindex, JS hien thong bao
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(html.replace("<!--SSR:HEAD-->", "<meta name=\"robots\" content=\"noindex\" />"));
        }
        BlogSummary p = d.summary();
        String url = baseUrl + "/post.html?slug=" + p.slug();
        String desc = p.excerpt() == null ? p.title() : p.excerpt();

        StringBuilder head = new StringBuilder()
                .append("<link rel=\"canonical\" href=\"").append(esc(url)).append("\" />\n")
                .append("    <meta property=\"og:url\" content=\"").append(esc(url)).append("\" />\n")
                .append("    <meta property=\"og:title\" content=\"").append(esc(p.title())).append("\" />\n")
                .append("    <meta property=\"og:description\" content=\"").append(esc(desc)).append("\" />\n");
        if (p.coverImageUrl() != null) {
            head.append("    <meta property=\"og:image\" content=\"").append(esc(p.coverImageUrl())).append("\" />\n");
        }
        // JSON-LD: escape "<" de chuoi "</script>" trong du lieu khong dong the script som
        String jsonLd = """
                {"@context":"https://schema.org","@type":"BlogPosting","headline":%s,"description":%s,\
                "datePublished":"%s","dateModified":"%s","author":{"@type":"Person","name":%s},\
                "publisher":{"@type":"Organization","name":"SeaSoft"},"mainEntityOfPage":%s}"""
                .formatted(json(p.title()), json(desc), p.publishedAt(), p.updatedAt(), json(p.authorName()), json(url));
        head.append("    <script type=\"application/ld+json\">").append(jsonLd).append("</script>");

        html = html
                .replace("<title>Bài viết — SeaSoft</title>", "<title>" + esc(p.title()) + " | Blog SeaSoft</title>")
                .replace("content=\"Bài viết từ blog SeaSoft.\"", "content=\"" + esc(desc) + "\"")
                .replace("<!--SSR:HEAD-->", head.toString())
                .replace("<div id=\"content\" class=\"hidden ", "<div id=\"content\" class=\"")
                .replace("<h1 id=\"title\" class=\"text-3xl sm:text-4xl font-bold leading-tight\"></h1>",
                        "<h1 id=\"title\" class=\"text-3xl sm:text-4xl font-bold leading-tight\">" + esc(p.title()) + "</h1>")
                .replace("<p id=\"excerpt\" class=\"text-lg text-text-muted\"></p>",
                        "<p id=\"excerpt\" class=\"text-lg text-text-muted\">" + esc(p.excerpt()) + "</p>")
                .replace("<div id=\"body\" class=\"prose-sea glass-card rounded-2xl border border-border/70 p-6 sm:p-10\"></div>",
                        "<div id=\"body\" class=\"prose-sea glass-card rounded-2xl border border-border/70 p-6 sm:p-10\">"
                                + BlogRenderer.render(d.content()) + "</div>");
        return ResponseEntity.ok(html);
    }

    // Tim post.html trong cac thu muc static (dev: ../Html, prod: classpath:/static)
    private String template() throws IOException {
        for (String loc : staticLocations) {
            Resource r = resourceLoader.getResource(loc.trim() + (loc.trim().endsWith("/") ? "" : "/") + "post.html");
            if (r.exists()) {
                return r.getContentAsString(StandardCharsets.UTF_8);
            }
        }
        throw ApiException.notFound("Không tìm thấy trang");
    }

    private static String json(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                case '<' -> b.append("\\u003c");
                default -> {
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        return b.append('"').toString();
    }
}
