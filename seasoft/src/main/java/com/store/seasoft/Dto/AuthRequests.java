package com.store.seasoft.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Cac request nho cho /api/auth (refresh, logout, quen mat khau)
public final class AuthRequests {

    private AuthRequests() {
    }

    public record RefreshRequest(
            @NotBlank(message = "Thiếu refresh token") String refreshToken) {
    }

    public record ForgotPasswordRequest(
            @NotBlank(message = "Email không được để trống")
            @Email(message = "Email không hợp lệ") String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "Thiếu token") String token,
            @NotBlank(message = "Mật khẩu không được để trống")
            @Size(min = 8, max = 100, message = "Mật khẩu từ 8 đến 100 ký tự") String newPassword) {
    }
}
