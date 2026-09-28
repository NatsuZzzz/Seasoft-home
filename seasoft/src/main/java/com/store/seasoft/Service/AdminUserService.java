package com.store.seasoft.Service;

import com.store.seasoft.Config.ApiException;
import com.store.seasoft.Dto.AdminDtos.CreateStaffRequest;
import com.store.seasoft.Dto.AdminDtos.UpdateUserRequest;
import com.store.seasoft.Dto.AdminDtos.UserView;
import com.store.seasoft.Dto.PageResponse;
import com.store.seasoft.Model.Role;
import com.store.seasoft.Model.StaffProfile;
import com.store.seasoft.Model.User;
import com.store.seasoft.Model.UserStatus;
import com.store.seasoft.Repository.RoleRepository;
import com.store.seasoft.Repository.StaffProfileRepository;
import com.store.seasoft.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final Set<String> ALL_ROLES = Set.of("CUSTOMER", "STAFF", "MANAGER", "ADMIN");
    private static final Duration INVITE_TTL = Duration.ofHours(48);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final MailService mailService;

    @Transactional(readOnly = true)
    public PageResponse<UserView> search(String role, UserStatus status, String q, int page, int size) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();
        if (role != null && !role.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role").get("code"), role));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), like),
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.coalesce(root.get("phone"), ""), like)));
        }
        Page<User> users = userRepository.findAll(spec, PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<UUID, StaffProfile> profiles = staffProfileRepository
                .findAllById(users.map(User::getId).getContent()).stream()
                .collect(Collectors.toMap(StaffProfile::getUserId, Function.identity()));
        return PageResponse.of(users, u -> UserView.from(u, profiles.get(u.getId())));
    }

    @Transactional(readOnly = true)
    public UserView get(UUID id) {
        User u = load(id);
        return UserView.from(u, staffProfileRepository.findById(id).orElse(null));
    }

    @Transactional
    public UserView createStaff(CreateStaffRequest req, User actor) {
        String role = req.role().toUpperCase(Locale.ROOT);
        if (!ConsultationService.STAFF_ROLES.contains(role)) {
            throw new IllegalArgumentException("Vai trò phải là STAFF, MANAGER hoặc ADMIN");
        }
        if (!isAdmin(actor) && !role.equals("STAFF")) {
            throw ApiException.forbidden("Quản lý chỉ được tạo tài khoản nhân viên (STAFF)");
        }
        String email = AuthService.normalizeEmail(req.email());
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Email đã được sử dụng");
        }
        String code = blankToNull(req.employeeCode());
        if (code != null && staffProfileRepository.existsByEmployeeCode(code)) {
            throw ApiException.conflict("Mã nhân viên đã tồn tại");
        }

        User u = new User();
        u.setFullName(req.fullName().trim());
        u.setEmail(email);
        u.setPhone(blankToNull(req.phone()));
        // Mat khau ngau nhien khong ai biet; nhan vien tu dat qua link moi
        u.setPasswordHash(passwordEncoder.encode(TokenUtils.randomToken()));
        u.setRole(roleByCode(role));
        u.setStatus(UserStatus.ACTIVE);
        userRepository.save(u);

        StaffProfile p = new StaffProfile();
        p.setUser(u);
        p.setEmployeeCode(code);
        p.setDepartment(blankToNull(req.department()));
        p.setPosition(blankToNull(req.position()));
        p.setHireDate(req.hireDate());
        staffProfileRepository.save(p);

        String link = authService.createPasswordLink(u, INVITE_TTL);
        mailService.send(u.getEmail(), "Mời bạn tham gia hệ thống SeaSoft",
                "Xin chào " + u.getFullName() + ",\n\n"
                        + actor.getFullName() + " đã tạo tài khoản SeaSoft cho bạn với vai trò " + role + ".\n"
                        + "Bấm vào link sau để đặt mật khẩu (hết hạn sau 48 giờ):\n" + link + "\n\n"
                        + "SeaSoft Team");
        return UserView.from(u, p);
    }

    @Transactional
    public UserView update(UUID id, UpdateUserRequest req, User actor) {
        User u = load(id);
        ensureCanManage(u, actor);
        if (req.fullName() != null && !req.fullName().isBlank()) {
            u.setFullName(req.fullName().trim());
        }
        if (req.phone() != null) {
            u.setPhone(blankToNull(req.phone()));
        }

        StaffProfile p = staffProfileRepository.findById(id).orElse(null);
        boolean touchesProfile = req.employeeCode() != null || req.department() != null
                || req.position() != null || req.hireDate() != null;
        if (touchesProfile) {
            if (!ConsultationService.STAFF_ROLES.contains(u.getRole().getCode())) {
                throw new IllegalArgumentException("Chỉ nhân viên mới có hồ sơ nhân sự");
            }
            if (p == null) {
                p = new StaffProfile();
                p.setUser(u);
            }
            if (req.employeeCode() != null) {
                String code = blankToNull(req.employeeCode());
                if (code != null && staffProfileRepository.existsByEmployeeCodeAndUserIdNot(code, id)) {
                    throw ApiException.conflict("Mã nhân viên đã tồn tại");
                }
                p.setEmployeeCode(code);
            }
            if (req.department() != null) {
                p.setDepartment(blankToNull(req.department()));
            }
            if (req.position() != null) {
                p.setPosition(blankToNull(req.position()));
            }
            if (req.hireDate() != null) {
                p.setHireDate(req.hireDate());
            }
            staffProfileRepository.save(p);
        }
        return UserView.from(u, p);
    }

    @Transactional
    public UserView changeStatus(UUID id, UserStatus status, User actor) {
        User u = load(id);
        if (u.getId().equals(actor.getId())) {
            throw new IllegalArgumentException("Không thể tự đổi trạng thái tài khoản của mình");
        }
        ensureCanManage(u, actor);
        u.setStatus(status);
        if (status != UserStatus.ACTIVE) {
            // Khoa -> dang xuat moi thiet bi (access token cu bi filter chan ngay)
            refreshTokenService.revokeAll(u);
        }
        return UserView.from(u, staffProfileRepository.findById(id).orElse(null));
    }

    @Transactional
    public UserView changeRole(UUID id, String rawRole, User actor) {
        if (!isAdmin(actor)) {
            throw ApiException.forbidden("Chỉ quản trị viên mới được đổi vai trò");
        }
        String role = rawRole.toUpperCase(Locale.ROOT);
        if (!ALL_ROLES.contains(role)) {
            throw new IllegalArgumentException("Vai trò không hợp lệ");
        }
        User u = load(id);
        if (u.getId().equals(actor.getId())) {
            throw new IllegalArgumentException("Không thể tự đổi vai trò của mình");
        }
        if (u.getRole().getCode().equals("ADMIN") && !role.equals("ADMIN")
                && userRepository.countByRoleCode("ADMIN") <= 1) {
            throw ApiException.conflict("Không thể hạ quyền quản trị viên cuối cùng");
        }
        u.setRole(roleByCode(role));
        refreshTokenService.revokeAll(u);
        return UserView.from(u, staffProfileRepository.findById(id).orElse(null));
    }

    @Transactional
    public void sendPasswordReset(UUID id, User actor) {
        User u = load(id);
        ensureCanManage(u, actor);
        String link = authService.createPasswordLink(u, INVITE_TTL);
        mailService.send(u.getEmail(), "SeaSoft - Đặt lại mật khẩu",
                "Xin chào " + u.getFullName() + ",\n\n"
                        + "Quản trị viên đã gửi cho bạn link đặt lại mật khẩu (hết hạn sau 48 giờ):\n"
                        + link + "\n\nSeaSoft Team");
    }

    // MANAGER khong duoc dung toi tai khoan MANAGER/ADMIN khac
    private void ensureCanManage(User target, User actor) {
        if (isAdmin(actor)) {
            return;
        }
        String role = target.getRole().getCode();
        if (role.equals("ADMIN") || role.equals("MANAGER")) {
            throw ApiException.forbidden("Quản lý không được thay đổi tài khoản quản lý / quản trị viên");
        }
    }

    private static boolean isAdmin(User u) {
        return u.getRole().getCode().equals("ADMIN");
    }

    private User load(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy người dùng"));
    }

    private Role roleByCode(String code) {
        return roleRepository.findByCode(code).orElseThrow(() -> new IllegalStateException("Chưa seed role " + code));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
