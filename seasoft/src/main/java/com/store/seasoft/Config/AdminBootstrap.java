package com.store.seasoft.Config;

import com.store.seasoft.Model.Role;
import com.store.seasoft.Model.User;
import com.store.seasoft.Model.UserStatus;
import com.store.seasoft.Repository.RoleRepository;
import com.store.seasoft.Repository.UserRepository;
import com.store.seasoft.Service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// Tao tai khoan ADMIN dau tien tu env ADMIN_EMAIL / ADMIN_PASSWORD
// khi he thong chua co admin nao. Da co admin thi khong lam gi.
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.email:}")
    private String email;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    // Mac dinh dat trong code: file .properties doc theo ISO-8859-1 nen chu co dau se bi loi font
    @Value("${app.bootstrap-admin.name:}")
    private String fullName;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void createFirstAdmin() {
        if (email.isBlank() || password.isBlank() || userRepository.countByRoleCode("ADMIN") > 0) {
            return;
        }
        if (password.length() < 8) {
            log.warn("ADMIN_PASSWORD qua ngan (< 8 ky tu), bo qua tao admin");
            return;
        }
        String normalized = AuthService.normalizeEmail(email);
        if (userRepository.existsByEmail(normalized)) {
            log.warn("Email {} da ton tai nhung khong phai ADMIN, bo qua tao admin", normalized);
            return;
        }
        Role admin = roleRepository.findByCode("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Chưa seed role ADMIN"));
        User u = new User();
        u.setFullName(fullName.isBlank() ? "Quản trị viên" : fullName);
        u.setEmail(normalized);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(admin);
        u.setStatus(UserStatus.ACTIVE);
        u.setEmailVerifiedAt(Instant.now());
        userRepository.save(u);
        log.info("Da tao tai khoan ADMIN dau tien: {}", normalized);
    }
}
