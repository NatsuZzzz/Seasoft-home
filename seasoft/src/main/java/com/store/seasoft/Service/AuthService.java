package com.store.seasoft.Service;

import com.store.seasoft.Dto.AuthResponse;
import com.store.seasoft.Dto.LoginRequest;
import com.store.seasoft.Dto.RegisterRequest;
import com.store.seasoft.Model.PasswordResetToken;
import com.store.seasoft.Model.Role;
import com.store.seasoft.Model.User;
import com.store.seasoft.Model.UserStatus;
import com.store.seasoft.Repository.PasswordResetTokenRepository;
import com.store.seasoft.Repository.RoleRepository;
import com.store.seasoft.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final MailService mailService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // Email luu va so sanh o dang chu thuong, bo khoang trang
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    // Đăng ký công khai luôn gán role CUSTOMER, không cho tự chọn ADMIN/STAFF
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        Role customerRole = roleRepository.findByCode("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("Chưa seed role CUSTOMER trong database"));

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhone(blankToNull(request.getPhone()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(customerRole);
        user.setStatus(UserStatus.ACTIVE);
        user.setLastLoginAt(Instant.now());

        userRepository.save(user);
        return buildResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        // Ném AuthenticationException nếu sai email/mật khẩu hoặc tài khoản bị khoá,
        // được xử lý ở GlobalExceptionHandler
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng"));
        user.setLastLoginAt(Instant.now());
        return buildResponse(user);
    }

    // Khong @Transactional: rotate() tu commit viec thu hoi token, ke ca khi nem 401
    public AuthResponse refresh(String refreshToken) {
        User user = refreshTokenService.rotate(refreshToken);
        ensureCanLogin(user);
        return buildResponse(user);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    // Luon "thanh cong" du email co ton tai hay khong, de khong lo danh sach tai khoan
    @Transactional
    public void forgotPassword(String rawEmail) {
        userRepository.findByEmail(normalizeEmail(rawEmail))
                .filter(u -> u.getStatus() != UserStatus.SUSPENDED)
                .ifPresent(user -> {
                    passwordResetTokenRepository.invalidateAllByUserId(user.getId(), Instant.now());

                    String raw = TokenUtils.randomToken();
                    PasswordResetToken token = new PasswordResetToken();
                    token.setUser(user);
                    token.setTokenHash(TokenUtils.sha256(raw));
                    token.setExpiresAt(Instant.now().plus(RESET_TOKEN_TTL));
                    passwordResetTokenRepository.save(token);

                    String link = frontendUrl + "/reset-password.html?token=" + raw;
                    mailService.send(user.getEmail(), "SeaSoft - Đặt lại mật khẩu",
                            "Xin chào " + user.getFullName() + ",\n\n"
                                    + "Bấm vào link sau để đặt lại mật khẩu (hết hạn sau 30 phút):\n"
                                    + link + "\n\n"
                                    + "Nếu bạn không yêu cầu, hãy bỏ qua email này.");
                });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(TokenUtils.sha256(rawToken))
                .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"));

        // Load lai user theo id: token.getUser() co the la instance da detach
        // (sau cac bulk update clearAutomatically) -> doi mat khau se khong duoc luu
        User user = userRepository.findById(token.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.setUsedAt(Instant.now());
        // Doi mat khau -> dang xuat moi thiet bi
        refreshTokenService.revokeAll(user);
    }

    private void ensureCanLogin(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new LockedException("Tài khoản đã bị khoá");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("Tài khoản chưa được kích hoạt");
        }
    }

    private AuthResponse buildResponse(User user) {
        String accessToken = jwtService.generateToken(new UserPrincipal(user));
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(accessToken, refreshToken, user.getEmail(), user.getFullName(),
                user.getRole().getCode());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
