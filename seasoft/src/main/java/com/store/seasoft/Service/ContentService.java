package com.store.seasoft.Service;

import com.store.seasoft.Config.ApiException;
import com.store.seasoft.Dto.ContentDtos.BlogDetail;
import com.store.seasoft.Dto.ContentDtos.BlogRequest;
import com.store.seasoft.Dto.ContentDtos.BlogSummary;
import com.store.seasoft.Dto.ContentDtos.PortfolioRequest;
import com.store.seasoft.Dto.ContentDtos.PortfolioView;
import com.store.seasoft.Dto.ContentDtos.TestimonialRequest;
import com.store.seasoft.Dto.ContentDtos.TestimonialView;
import com.store.seasoft.Dto.PageResponse;
import com.store.seasoft.Model.BlogPost;
import com.store.seasoft.Model.PortfolioItem;
import com.store.seasoft.Model.Testimonial;
import com.store.seasoft.Model.User;
import com.store.seasoft.Repository.BlogPostRepository;
import com.store.seasoft.Repository.PortfolioRepository;
import com.store.seasoft.Repository.TestimonialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final PortfolioRepository portfolioRepository;
    private final BlogPostRepository blogRepository;
    private final TestimonialRepository testimonialRepository;

    // ======================= PUBLIC =======================

    @Transactional(readOnly = true)
    public List<PortfolioView> publishedPortfolio() {
        return portfolioRepository.findByPublishedTrueOrderBySortOrderAscCreatedAtDesc().stream()
                .map(PortfolioView::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<BlogSummary> publishedPosts(int page, int size) {
        return PageResponse.of(blogRepository.findByStatusOrderByPublishedAtDesc(BlogPost.Status.PUBLISHED,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50))), BlogSummary::from);
    }

    // Ban nhap khong lo ra ngoai: tra 404 nhu khong ton tai
    @Transactional(readOnly = true)
    public BlogDetail publishedPost(String slug) {
        return blogRepository.findBySlugAndStatus(slug, BlogPost.Status.PUBLISHED).map(BlogDetail::from)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy bài viết"));
    }

    @Transactional(readOnly = true)
    public List<TestimonialView> publishedTestimonials() {
        return testimonialRepository.findByPublishedTrueOrderBySortOrderAscCreatedAtDesc().stream()
                .map(TestimonialView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BlogPost> postsForSitemap() {
        return blogRepository.findByStatus(BlogPost.Status.PUBLISHED);
    }

    // ======================= DU AN TIEU BIEU =======================

    @Transactional(readOnly = true)
    public List<PortfolioView> allPortfolio() {
        return portfolioRepository.findAllByOrderBySortOrderAscCreatedAtDesc().stream().map(PortfolioView::from).toList();
    }

    @Transactional
    public PortfolioView savePortfolio(UUID id, PortfolioRequest r) {
        PortfolioItem p = id == null ? new PortfolioItem()
                : portfolioRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy dự án tiêu biểu"));
        p.setTitle(r.title().trim());
        p.setSlug(resolveSlug(r.slug(), r.title(), id, portfolioRepository::existsBySlug,
                s -> portfolioRepository.existsBySlugAndIdNot(s, id)));
        p.setCategory(blankToNull(r.category()));
        p.setClientName(blankToNull(r.clientName()));
        p.setSummary(blankToNull(r.summary()));
        p.setContent(blankToNull(r.content()));
        p.setCoverImageUrl(blankToNull(r.coverImageUrl()));
        p.setProjectUrl(blankToNull(r.projectUrl()));
        p.setPublished(Boolean.TRUE.equals(r.published()));
        p.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder());
        return PortfolioView.from(portfolioRepository.save(p));
    }

    @Transactional
    public void deletePortfolio(UUID id) {
        if (!portfolioRepository.existsById(id)) {
            throw ApiException.notFound("Không tìm thấy dự án tiêu biểu");
        }
        portfolioRepository.deleteById(id);
    }

    // ======================= BLOG =======================

    @Transactional(readOnly = true)
    public PageResponse<BlogSummary> allPosts(int page, int size) {
        return PageResponse.of(blogRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))), BlogSummary::from);
    }

    @Transactional(readOnly = true)
    public BlogDetail post(UUID id) {
        return BlogDetail.from(loadPost(id));
    }

    @Transactional
    public BlogDetail savePost(UUID id, BlogRequest r, User author) {
        BlogPost b = id == null ? new BlogPost() : loadPost(id);
        b.setTitle(r.title().trim());
        b.setSlug(resolveSlug(r.slug(), r.title(), id, blogRepository::existsBySlug,
                s -> blogRepository.existsBySlugAndIdNot(s, id)));
        b.setExcerpt(blankToNull(r.excerpt()));
        b.setContent(r.content().trim());
        b.setCoverImageUrl(blankToNull(r.coverImageUrl()));
        b.setTags(blankToNull(r.tags()));
        if (b.getAuthor() == null) {
            b.setAuthor(author);
        }
        b.changeStatus(r.status() == null ? BlogPost.Status.DRAFT : r.status());
        return BlogDetail.from(blogRepository.save(b));
    }

    @Transactional
    public void deletePost(UUID id) {
        blogRepository.delete(loadPost(id));
    }

    // ======================= DANH GIA =======================

    @Transactional(readOnly = true)
    public List<TestimonialView> allTestimonials() {
        return testimonialRepository.findAllByOrderBySortOrderAscCreatedAtDesc().stream()
                .map(TestimonialView::from).toList();
    }

    @Transactional
    public TestimonialView saveTestimonial(UUID id, TestimonialRequest r) {
        Testimonial t = id == null ? new Testimonial()
                : testimonialRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đánh giá"));
        t.setCustomerName(r.customerName().trim());
        t.setCompany(blankToNull(r.company()));
        t.setPosition(blankToNull(r.position()));
        t.setQuote(r.quote().trim());
        t.setRating((short) (r.rating() == null ? 5 : r.rating()));
        t.setAvatarUrl(blankToNull(r.avatarUrl()));
        t.setPublished(Boolean.TRUE.equals(r.published()));
        t.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder());
        return TestimonialView.from(testimonialRepository.save(t));
    }

    @Transactional
    public void deleteTestimonial(UUID id) {
        if (!testimonialRepository.existsById(id)) {
            throw ApiException.notFound("Không tìm thấy đánh giá");
        }
        testimonialRepository.deleteById(id);
    }

    // ======================= helpers =======================

    private BlogPost loadPost(UUID id) {
        return blogRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy bài viết"));
    }

    // Slug nhap tay: phai chua ai dung (409). Bo trong: sinh tu tieu de, tu them -2, -3 neu trung.
    private static String resolveSlug(String requested, String title, UUID id,
                                      java.util.function.Predicate<String> existsForNew,
                                      java.util.function.Predicate<String> existsForOther) {
        java.util.function.Predicate<String> taken = id == null ? existsForNew : existsForOther;
        if (requested != null && !requested.isBlank()) {
            if (taken.test(requested)) {
                throw ApiException.conflict("Slug \"" + requested + "\" đã được dùng");
            }
            return requested;
        }
        return SlugUtils.unique(SlugUtils.slugify(title), taken);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
