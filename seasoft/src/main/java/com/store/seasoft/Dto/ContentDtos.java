package com.store.seasoft.Dto;

import com.store.seasoft.Model.BlogPost;
import com.store.seasoft.Model.PortfolioItem;
import com.store.seasoft.Model.Testimonial;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ContentDtos {

    // Chi cho link http(s): chan "javascript:..." / "data:..." trong href/src
    static final String URL = "^$|^https?://[^\\s\"'<>]+$";
    static final String URL_MSG = "Link phải bắt đầu bằng http:// hoặc https://";
    static final String SLUG = "^$|^[a-z0-9]+(-[a-z0-9]+)*$";
    static final String SLUG_MSG = "Slug chỉ gồm chữ thường không dấu, số và dấu gạch ngang";

    private ContentDtos() {
    }

    // ---------- Du an tieu bieu ----------

    public record PortfolioRequest(
            @NotBlank(message = "Vui lòng nhập tiêu đề") @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự") String title,
            @Size(max = 200, message = "Slug tối đa 200 ký tự") @Pattern(regexp = SLUG, message = SLUG_MSG) String slug,
            @Size(max = 100, message = "Danh mục tối đa 100 ký tự") String category,
            @Size(max = 150, message = "Tên khách hàng tối đa 150 ký tự") String clientName,
            @Size(max = 500, message = "Tóm tắt tối đa 500 ký tự") String summary,
            @Size(max = 20000, message = "Nội dung tối đa 20000 ký tự") String content,
            @Size(max = 1000, message = "Link ảnh tối đa 1000 ký tự") @Pattern(regexp = URL, message = URL_MSG) String coverImageUrl,
            @Size(max = 500, message = "Link dự án tối đa 500 ký tự") @Pattern(regexp = URL, message = URL_MSG) String projectUrl,
            Boolean published,
            Integer sortOrder) {
    }

    public record PortfolioView(UUID id, String title, String slug, String category, String clientName, String summary,
                                String content, String coverImageUrl, String projectUrl, boolean published, int sortOrder,
                                Instant updatedAt) {
        public static PortfolioView from(PortfolioItem p) {
            return new PortfolioView(p.getId(), p.getTitle(), p.getSlug(), p.getCategory(), p.getClientName(),
                    p.getSummary(), p.getContent(), p.getCoverImageUrl(), p.getProjectUrl(), p.isPublished(),
                    p.getSortOrder(), p.getUpdatedAt());
        }
    }

    // ---------- Blog ----------

    public record BlogRequest(
            @NotBlank(message = "Vui lòng nhập tiêu đề") @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự") String title,
            @Size(max = 200, message = "Slug tối đa 200 ký tự") @Pattern(regexp = SLUG, message = SLUG_MSG) String slug,
            @Size(max = 500, message = "Mô tả ngắn tối đa 500 ký tự") String excerpt,
            @NotBlank(message = "Vui lòng nhập nội dung") @Size(max = 50000, message = "Nội dung tối đa 50000 ký tự") String content,
            @Size(max = 1000, message = "Link ảnh tối đa 1000 ký tự") @Pattern(regexp = URL, message = URL_MSG) String coverImageUrl,
            @Size(max = 200, message = "Thẻ tối đa 200 ký tự") String tags,
            BlogPost.Status status) {
    }

    public record BlogSummary(UUID id, String title, String slug, String excerpt, String coverImageUrl, String tags,
                              String authorName, BlogPost.Status status, Instant publishedAt, Instant updatedAt) {
        public static BlogSummary from(BlogPost b) {
            return new BlogSummary(b.getId(), b.getTitle(), b.getSlug(), b.getExcerpt(), b.getCoverImageUrl(),
                    b.getTags(), b.getAuthor() == null ? "SeaSoft" : b.getAuthor().getFullName(), b.getStatus(),
                    b.getPublishedAt(), b.getUpdatedAt());
        }
    }

    public record BlogDetail(BlogSummary summary, String content) {
        public static BlogDetail from(BlogPost b) {
            return new BlogDetail(BlogSummary.from(b), b.getContent());
        }
    }

    // ---------- Danh gia khach hang ----------

    public record TestimonialRequest(
            @NotBlank(message = "Vui lòng nhập tên khách hàng") @Size(max = 150, message = "Tên tối đa 150 ký tự") String customerName,
            @Size(max = 200, message = "Công ty tối đa 200 ký tự") String company,
            @Size(max = 100, message = "Chức vụ tối đa 100 ký tự") String position,
            @NotBlank(message = "Vui lòng nhập nội dung đánh giá") @Size(max = 1000, message = "Đánh giá tối đa 1000 ký tự") String quote,
            @Min(value = 1, message = "Điểm từ 1 đến 5") @Max(value = 5, message = "Điểm từ 1 đến 5") Integer rating,
            @Size(max = 1000, message = "Link ảnh tối đa 1000 ký tự") @Pattern(regexp = URL, message = URL_MSG) String avatarUrl,
            Boolean published,
            Integer sortOrder) {
    }

    public record TestimonialView(UUID id, String customerName, String company, String position, String quote,
                                  int rating, String avatarUrl, boolean published, int sortOrder) {
        public static TestimonialView from(Testimonial t) {
            return new TestimonialView(t.getId(), t.getCustomerName(), t.getCompany(), t.getPosition(), t.getQuote(),
                    t.getRating(), t.getAvatarUrl(), t.isPublished(), t.getSortOrder());
        }
    }
}
