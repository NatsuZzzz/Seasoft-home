package com.store.seasoft.Dto;

import com.store.seasoft.Model.ConsultationEnums.BudgetRange;
import com.store.seasoft.Model.ConsultationEnums.ServiceType;
import com.store.seasoft.Model.ConsultationEnums.Status;
import com.store.seasoft.Model.ConsultationRequest;
import com.store.seasoft.Model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ConsultationDtos {

    private ConsultationDtos() {
    }

    // Form "Nhan tu van" tren website. "website" la o honeypot an: nguoi that khong dien, bot hay dien.
    public record CreateRequest(
            @NotBlank(message = "Vui lòng nhập họ tên")
            @Size(max = 150, message = "Họ tên tối đa 150 ký tự") String fullName,
            @NotBlank(message = "Vui lòng nhập email")
            @Email(message = "Email không hợp lệ")
            @Size(max = 150, message = "Email tối đa 150 ký tự") String email,
            @NotBlank(message = "Vui lòng nhập số điện thoại")
            @Pattern(regexp = "^\\+?[0-9 .-]{8,20}$", message = "Số điện thoại không hợp lệ") String phone,
            @Size(max = 200, message = "Tên công ty tối đa 200 ký tự") String companyName,
            @NotNull(message = "Vui lòng chọn dịch vụ") ServiceType serviceType,
            BudgetRange budgetRange,
            @Size(max = 2000, message = "Nội dung tối đa 2000 ký tự") String message,
            String website) {

        // Bo khoang trang thua truoc khi validate @Email
        public CreateRequest {
            email = email == null ? null : email.trim();
        }
    }

    public record CreateResponse(UUID id, String message) {
    }

    public record StaffSummary(UUID id, String fullName, String email, String role) {
        public static StaffSummary from(User u) {
            return u == null ? null : new StaffSummary(u.getId(), u.getFullName(), u.getEmail(), u.getRole().getCode());
        }
    }

    // Du lieu day du cho nhan vien
    public record StaffView(UUID id, String fullName, String email, String phone, String companyName,
                            ServiceType serviceType, BudgetRange budgetRange, String message, Status status,
                            StaffSummary assignedStaff, boolean hasAccount, String internalNote, String source,
                            Instant createdAt, Instant updatedAt) {

        public static StaffView from(ConsultationRequest c) {
            return new StaffView(c.getId(), c.getFullName(), c.getEmail(), c.getPhone(), c.getCompanyName(),
                    c.getServiceType(), c.getBudgetRange(), c.getMessage(), c.getStatus(),
                    StaffSummary.from(c.getAssignedStaff()), c.getCustomer() != null, c.getInternalNote(),
                    c.getSource(), c.getCreatedAt(), c.getUpdatedAt());
        }
    }

    // Du lieu khach duoc xem (khong co ghi chu noi bo, IP...)
    public record CustomerView(UUID id, ServiceType serviceType, BudgetRange budgetRange, String message,
                               Status status, String assignedStaffName, Instant createdAt) {

        public static CustomerView from(ConsultationRequest c) {
            return new CustomerView(c.getId(), c.getServiceType(), c.getBudgetRange(), c.getMessage(),
                    c.getStatus(), c.getAssignedStaff() == null ? null : c.getAssignedStaff().getFullName(),
                    c.getCreatedAt());
        }
    }

    // Nhan vien cap nhat: moi truong deu tuy chon, null = giu nguyen
    public record UpdateRequest(
            Status status,
            @Size(max = 5000, message = "Ghi chú tối đa 5000 ký tự") String internalNote) {
    }

    // staffId = null -> bo phan cong
    public record AssignRequest(UUID staffId) {
    }
}
