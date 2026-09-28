package com.store.seasoft.Controller;

import com.store.seasoft.Dto.ContentDtos.BlogDetail;
import com.store.seasoft.Dto.ContentDtos.BlogSummary;
import com.store.seasoft.Dto.ContentDtos.PortfolioView;
import com.store.seasoft.Dto.ContentDtos.TestimonialView;
import com.store.seasoft.Dto.PageResponse;
import com.store.seasoft.Service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Noi dung da xuat ban cho website cong khai (khong can dang nhap)
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicContentController {

    private final ContentService contentService;

    @GetMapping("/portfolio")
    public List<PortfolioView> portfolio() {
        return contentService.publishedPortfolio();
    }

    @GetMapping("/blog")
    public PageResponse<BlogSummary> blog(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "9") int size) {
        return contentService.publishedPosts(page, size);
    }

    @GetMapping("/blog/{slug}")
    public BlogDetail post(@PathVariable String slug) {
        return contentService.publishedPost(slug);
    }

    @GetMapping("/testimonials")
    public List<TestimonialView> testimonials() {
        return contentService.publishedTestimonials();
    }
}
