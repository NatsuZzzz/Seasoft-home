package com.store.seasoft.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blog_posts")
@Getter
@Setter
public class BlogPost {

    public enum Status { DRAFT, PUBLISHED }

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(length = 500)
    private String excerpt;

    // Van ban thuong: dong trong = xuong doan, dong bat dau "## " = tieu de muc
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;

    @Column(length = 200)
    private String tags;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

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

    // Lan dau xuat ban thi ghi ngay dang; chuyen ve nhap giu nguyen ngay cu
    public void changeStatus(Status next) {
        if (next == Status.PUBLISHED && publishedAt == null) {
            publishedAt = Instant.now();
        }
        status = next;
    }
}
