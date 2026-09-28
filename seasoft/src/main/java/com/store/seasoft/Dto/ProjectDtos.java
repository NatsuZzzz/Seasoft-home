package com.store.seasoft.Dto;

import com.store.seasoft.Dto.ConsultationDtos.StaffSummary;
import com.store.seasoft.Model.ConsultationEnums.ServiceType;
import com.store.seasoft.Model.Project;
import com.store.seasoft.Model.ProjectMilestone;
import com.store.seasoft.Model.ProjectUpdate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    // ---------- Request ----------

    // Tao tu lead (consultationId) HOAC chi dinh customerEmail cua khach da co tai khoan
    public record CreateRequest(
            UUID consultationId,
            @Email(message = "Email khách hàng không hợp lệ") String customerEmail,
            @NotBlank(message = "Vui lòng nhập tên dự án")
            @Size(max = 200, message = "Tên dự án tối đa 200 ký tự") String name,
            ServiceType serviceType,
            @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự") String description,
            LocalDate startDate,
            LocalDate dueDate,
            UUID managerId,
            // null -> tao 5 moc mac dinh theo quy trinh SeaSoft
            @Valid List<MilestoneRequest> milestones) {
    }

    // Moi truong tuy chon, null = giu nguyen
    public record UpdateRequest(
            @Size(max = 200, message = "Tên dự án tối đa 200 ký tự") String name,
            Project.Status status,
            @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự") String description,
            LocalDate startDate,
            LocalDate dueDate,
            UUID managerId) {
    }

    public record MilestoneRequest(
            @NotBlank(message = "Vui lòng nhập tên mốc")
            @Size(max = 200, message = "Tên mốc tối đa 200 ký tự") String title,
            @Size(max = 2000, message = "Mô tả mốc tối đa 2000 ký tự") String description,
            LocalDate dueDate) {
    }

    public record MilestoneUpdateRequest(
            @Size(max = 200, message = "Tên mốc tối đa 200 ký tự") String title,
            ProjectMilestone.Status status,
            LocalDate dueDate) {
    }

    public record PostUpdateRequest(
            @NotBlank(message = "Nội dung không được để trống")
            @Size(max = 5000, message = "Nội dung tối đa 5000 ký tự") String content,
            Boolean visibleToCustomer) {
    }

    // ---------- Response ----------

    public record MilestoneView(UUID id, String title, String description, int sortOrder,
                                ProjectMilestone.Status status, LocalDate dueDate, Instant completedAt) {
        public static MilestoneView from(ProjectMilestone m) {
            return new MilestoneView(m.getId(), m.getTitle(), m.getDescription(), m.getSortOrder(),
                    m.getStatus(), m.getDueDate(), m.getCompletedAt());
        }
    }

    public record UpdateView(UUID id, String content, boolean visibleToCustomer, String authorName,
                             String authorRole, Instant createdAt) {
        public static UpdateView from(ProjectUpdate u) {
            return new UpdateView(u.getId(), u.getContent(), u.isVisibleToCustomer(),
                    u.getAuthor() == null ? "(đã xoá)" : u.getAuthor().getFullName(),
                    u.getAuthor() == null ? null : u.getAuthor().getRole().getCode(), u.getCreatedAt());
        }
    }

    // Dong trong danh sach
    public record Summary(UUID id, String code, String name, ServiceType serviceType, Project.Status status,
                          int progress, int milestoneCount, String customerName, String customerEmail,
                          StaffSummary manager, LocalDate startDate, LocalDate dueDate, Instant updatedAt) {
        public static Summary from(Project p) {
            return new Summary(p.getId(), p.getCode(), p.getName(), p.getServiceType(), p.getStatus(),
                    p.progress(), p.getMilestones().size(), p.getCustomer().getFullName(), p.getCustomer().getEmail(),
                    StaffSummary.from(p.getManager()), p.getStartDate(), p.getDueDate(), p.getUpdatedAt());
        }
    }

    public record Detail(Summary summary, String description, UUID consultationId,
                         List<MilestoneView> milestones, List<UpdateView> updates) {
    }

    // Ban cho khach: khong co email khach, lead goc...
    public record CustomerSummary(UUID id, String code, String name, ServiceType serviceType,
                                  Project.Status status, int progress, String managerName,
                                  LocalDate startDate, LocalDate dueDate, Instant updatedAt) {
        public static CustomerSummary from(Project p) {
            return new CustomerSummary(p.getId(), p.getCode(), p.getName(), p.getServiceType(), p.getStatus(),
                    p.progress(), p.getManager() == null ? null : p.getManager().getFullName(),
                    p.getStartDate(), p.getDueDate(), p.getUpdatedAt());
        }
    }

    public record CustomerDetail(CustomerSummary summary, String description, List<MilestoneView> milestones,
                                 List<UpdateView> updates) {
    }
}
