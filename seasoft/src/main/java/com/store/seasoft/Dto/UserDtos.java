package com.store.seasoft.Dto;

import com.store.seasoft.Model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

// DTO cho /api/users/me
public final class UserDtos {

    private UserDtos() {
    }

    public record UserResponse(UUID id, String fullName, String email, String phone, String avatarUrl,
                               String role, String status, Instant createdAt, Instant lastLoginAt) {

        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getAvatarUrl(),
                    u.getRole().getCode(), u.getStatus().name(), u.getCreatedAt(), u.getLastLoginAt());
        }
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "Họ tên không được để trống")
            @Size(max = 150, message = "Họ tên tối đa 150 ký tự") String fullName,
            @Pattern(regexp = "^$|^\\+?[0-9 .-]{8,20}$", message = "Số điện thoại không hợp lệ") String phone) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Nhập mật khẩu hiện tại")
            @Size(max = 100, message = "Mật khẩu tối đa 100 ký tự") String currentPassword,
            @NotBlank(message = "Mật khẩu mới không được để trống")
            @Size(min = 8, max = 100, message = "Mật khẩu từ 8 đến 100 ký tự") String newPassword) {
    }
}
