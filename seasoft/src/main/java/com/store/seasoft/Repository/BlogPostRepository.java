package com.store.seasoft.Repository;

import com.store.seasoft.Model.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {

    @EntityGraph(attributePaths = "author")
    Page<BlogPost> findByStatusOrderByPublishedAtDesc(BlogPost.Status status, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<BlogPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Optional<BlogPost> findBySlugAndStatus(String slug, BlogPost.Status status);

    List<BlogPost> findByStatus(BlogPost.Status status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);
}
