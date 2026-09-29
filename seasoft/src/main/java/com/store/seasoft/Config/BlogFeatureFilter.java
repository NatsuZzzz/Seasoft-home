package com.store.seasoft.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Tat blog cong khai khi app.features.blog=false (env FEATURE_BLOG).
// Trang blog -> ve trang chu, API blog cong khai -> 404. Admin van soan bai duoc de bat lai sau.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class BlogFeatureFilter extends OncePerRequestFilter {

    @Value("${app.features.blog:false}")
    private boolean blogEnabled;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return blogEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.equals("/blog.html") || path.equals("/post.html")) {
            res.sendRedirect(req.getContextPath() + "/Page.html");
            return;
        }
        if (path.equals("/api/public/blog") || path.startsWith("/api/public/blog/")) {
            res.setStatus(404);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"status\":404,\"message\":\"Không tìm thấy\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
