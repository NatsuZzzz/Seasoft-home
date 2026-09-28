package com.store.seasoft.Controller;

import com.store.seasoft.Dto.ContentDtos.BlogDetail;
import com.store.seasoft.Dto.ContentDtos.BlogRequest;
import com.store.seasoft.Dto.ContentDtos.BlogSummary;
import com.store.seasoft.Dto.ContentDtos.PortfolioRequest;
import com.store.seasoft.Dto.ContentDtos.PortfolioView;
import com.store.seasoft.Dto.ContentDtos.TestimonialRequest;
import com.store.seasoft.Dto.ContentDtos.TestimonialView;
import com.store.seasoft.Dto.PageResponse;
import com.store.seasoft.Service.ContentService;
import com.store.seasoft.Service.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Quan ly noi dung marketing - MANAGER/ADMIN (duoi /api/admin)
@RestController
@RequestMapping("/api/admin/content")
@RequiredArgsConstructor
public class AdminContentController {

    private final ContentService contentService;

    // ---------- Du an tieu bieu ----------

    @GetMapping("/portfolio")
    public List<PortfolioView> portfolio() {
        return contentService.allPortfolio();
    }

    @PostMapping("/portfolio")
    public ResponseEntity<PortfolioView> createPortfolio(@Valid @RequestBody PortfolioRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.savePortfolio(null, r));
    }

    @PutMapping("/portfolio/{id}")
    public PortfolioView updatePortfolio(@PathVariable UUID id, @Valid @RequestBody PortfolioRequest r) {
        return contentService.savePortfolio(id, r);
    }

    @DeleteMapping("/portfolio/{id}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable UUID id) {
        contentService.deletePortfolio(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Blog ----------

    @GetMapping("/blog")
    public PageResponse<BlogSummary> posts(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return contentService.allPosts(page, size);
    }

    @GetMapping("/blog/{id}")
    public BlogDetail post(@PathVariable UUID id) {
        return contentService.post(id);
    }

    @PostMapping("/blog")
    public ResponseEntity<BlogDetail> createPost(@Valid @RequestBody BlogRequest r,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.savePost(null, r, principal.getUser()));
    }

    @PutMapping("/blog/{id}")
    public BlogDetail updatePost(@PathVariable UUID id, @Valid @RequestBody BlogRequest r,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return contentService.savePost(id, r, principal.getUser());
    }

    @DeleteMapping("/blog/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable UUID id) {
        contentService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Danh gia ----------

    @GetMapping("/testimonials")
    public List<TestimonialView> testimonials() {
        return contentService.allTestimonials();
    }

    @PostMapping("/testimonials")
    public ResponseEntity<TestimonialView> createTestimonial(@Valid @RequestBody TestimonialRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.saveTestimonial(null, r));
    }

    @PutMapping("/testimonials/{id}")
    public TestimonialView updateTestimonial(@PathVariable UUID id, @Valid @RequestBody TestimonialRequest r) {
        return contentService.saveTestimonial(id, r);
    }

    @DeleteMapping("/testimonials/{id}")
    public ResponseEntity<Void> deleteTestimonial(@PathVariable UUID id) {
        contentService.deleteTestimonial(id);
        return ResponseEntity.noContent().build();
    }
}
