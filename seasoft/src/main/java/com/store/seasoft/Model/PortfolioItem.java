package com.store.seasoft.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// Du an tieu bieu hien tren trang chu
@Entity
@Table(name = "portfolio_items")
@Getter
@Setter
public class PortfolioItem {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(length = 100)
    private String category;

    @Column(name = "client_name", length = 150)
    private String clientName;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;

    @Column(name = "project_url", length = 500)
    private String projectUrl;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
