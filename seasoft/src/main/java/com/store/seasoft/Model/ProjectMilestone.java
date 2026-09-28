package com.store.seasoft.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "project_milestones")
@Getter
@Setter
public class ProjectMilestone {

    public enum Status { TODO, IN_PROGRESS, DONE }

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.TODO;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    // Chuyen trang thai, tu ghi/xoa thoi diem hoan thanh
    public void changeStatus(Status next) {
        if (next == Status.DONE && status != Status.DONE) {
            completedAt = Instant.now();
        } else if (next != Status.DONE) {
            completedAt = null;
        }
        status = next;
    }
}
