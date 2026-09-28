package com.store.seasoft.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// Danh gia that cua khach hang (nhap tay boi quan tri, co su dong y cua khach)
@Entity
@Table(name = "testimonials")
@Getter
@Setter
public class Testimonial {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(length = 200)
    private String company;

    @Column(length = 100)
    private String position;

    @Column(nullable = false, columnDefinition = "text")
    private String quote;

    @Column(nullable = false)
    private short rating = 5;

    @Column(name = "avatar_url", length = 1000)
    private String avatarUrl;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
