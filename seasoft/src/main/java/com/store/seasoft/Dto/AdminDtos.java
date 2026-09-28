package com.store.seasoft.Dto;

import com.store.seasoft.Model.StaffProfile;
import com.store.seasoft.Model.User;
import com.store.seasoft.Model.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record StaffProfileView(String employeeCode, String department, String position, LocalDate hireDate) {
        public static StaffProfileView from(StaffProfile p) {
            return p == null ? null
                    : new StaffProfileView(p.getEmployeeCode(), p.getDepartment(), p.getPosition(), p.getHireDate());
        }
    }

    public record UserView(UUID id, String fullName, String email, String phone, String role, UserStatus status,
                           Instant createdAt, Instant lastLoginAt, StaffProfileView staffProfile) {
        public static UserView from(User u, StaffProfile p) {
            return new UserView(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole().getCode(),
                    u.getStatus(), u.getCreatedAt(), u.getLastLoginAt(), StaffProfileView.from(p));
        }
    }

    // Tao tai khoan nhan vien: khong nhap mat khau, he thong gui email moi dat mat khau
    public record CreateStaffRequest(
            @NotBlank(message = "Vui lòng nhập họ tên")
            @Size(max = 150, message = "Họ tên tối đa 150 ký tự") String fullName,
            @NotBlank(message = "Vui lòng nhập email")
            @Email(message = "Email không hợp lệ")
            @Size(max = 150, message = "Email tối đa 150 ký tự") String email,
            @Pattern(regexp = "^$|^\\+?[0-9 .-]{8,20}$", message = "Số điện thoại không hợp lệ") String phone,
            @NotBlank(message = "Vui lòng chọn vai trò") String role,
            @Size(max = 30, message = "Mã nhân viên tối đa 30 ký tự") String employeeCode,
            @Size(max = 100, message = "Phòng ban tối đa 100 ký tự") String department,
            @Size(max = 100, message = "Chức vụ tối đa 100 ký tự") String position,
            LocalDate hireDate) {

        public CreateStaffRequest {
            email = email == null ? null : email.trim();
        }
    }

    // Moi truong tuy chon, null = giu nguyen
    public record UpdateUserRequest(
            @Size(max = 150, message = "Họ tên tối đa 150 ký tự") String fullName,
            @Pattern(regexp = "^$|^\\+?[0-9 .-]{8,20}$", message = "Số điện thoại không hợp lệ") String phone,
            @Size(max = 30, message = "Mã nhân viên tối đa 30 ký tự") String employeeCode,
            @Size(max = 100, message = "Phòng ban tối đa 100 ký tự") String department,
            @Size(max = 100, message = "Chức vụ tối đa 100 ký tự") String position,
            LocalDate hireDate) {
    }

    public record StatusRequest(@NotNull(message = "Vui lòng chọn trạng thái") UserStatus status) {
    }

    public record RoleRequest(@NotBlank(message = "Vui lòng chọn vai trò") String role) {
    }

    // ---------- Thong ke ----------

    public record WeekCount(LocalDate weekStart, long count) {
    }

    public record Stats(
            long leadsTotal,
            long leadsLast30Days,
            Map<String, Long> leadsByStatus,
            Map<String, Long> leadsByService,
            List<WeekCount> leadsPerWeek,
            Double conversionRate,       // WON / (WON + LOST), null neu chua co lead ket thuc
            long projectsTotal,
            Map<String, Long> projectsByStatus,
            Double avgProgress,          // % trung binh cua du an dang chay
            long projectsOverdue,        // qua han ma chua hoan thanh/huy
            Map<String, Long> usersByRole) {
    }
}
