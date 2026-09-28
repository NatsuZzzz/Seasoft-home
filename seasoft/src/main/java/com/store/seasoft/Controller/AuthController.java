package com.store.seasoft.Controller;

import com.store.seasoft.Dto.AuthRequests.ForgotPasswordRequest;
import com.store.seasoft.Dto.AuthRequests.RefreshRequest;
import com.store.seasoft.Dto.AuthRequests.ResetPasswordRequest;
import com.store.seasoft.Dto.AuthResponse;
import com.store.seasoft.Dto.LoginRequest;
import com.store.seasoft.Dto.RegisterRequest;
import com.store.seasoft.Service.AuthRateLimits;
import com.store.seasoft.Service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthRateLimits rateLimits;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        rateLimits.checkRegister(http.getRemoteAddr());
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        rateLimits.checkLogin(http.getRemoteAddr(), AuthService.normalizeEmail(request.getEmail()));
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                              HttpServletRequest http) {
        rateLimits.checkForgot(http.getRemoteAddr(), AuthService.normalizeEmail(request.email()));
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(Map.of("message",
                "Nếu email tồn tại trong hệ thống, link đặt lại mật khẩu đã được gửi"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công, hãy đăng nhập lại"));
    }
}
