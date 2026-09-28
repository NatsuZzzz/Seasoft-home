package com.store.seasoft.Service;

import com.store.seasoft.Config.ApiException;
import com.store.seasoft.Dto.ConsultationDtos.CreateRequest;
import com.store.seasoft.Dto.ConsultationDtos.CustomerView;
import com.store.seasoft.Dto.ConsultationDtos.StaffSummary;
import com.store.seasoft.Dto.ConsultationDtos.StaffView;
import com.store.seasoft.Dto.ConsultationDtos.UpdateRequest;
import com.store.seasoft.Dto.PageResponse;
import com.store.seasoft.Model.ConsultationEnums.BudgetRange;
import com.store.seasoft.Model.ConsultationEnums.ServiceType;
import com.store.seasoft.Model.ConsultationEnums.Status;
import com.store.seasoft.Model.ConsultationRequest;
import com.store.seasoft.Model.User;
import com.store.seasoft.Model.UserStatus;
import com.store.seasoft.Repository.ConsultationRepository;
import com.store.seasoft.Repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationService {

    public static final Set<String> STAFF_ROLES = Set.of("STAFF", "MANAGER", "ADMIN");

    private static final Map<ServiceType, String> SERVICE_LABELS = Map.of(
            ServiceType.CORPORATE_WEBSITE, "Website doanh nghiệp",
            ServiceType.LANDING_PAGE, "Landing Page",
            ServiceType.ECOMMERCE, "Website bán hàng",
            ServiceType.CUSTOM, "Website theo yêu cầu",
            ServiceType.MAINTENANCE, "Bảo trì & nâng cấp",
            ServiceType.OTHER, "Khác");

    private final ConsultationRepository consultationRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final RateLimiter consultationRateLimiter;

    @Value("${app.lead-notify-email:}")
    private String notifyEmail;

    // ---------------- Khach gui form ----------------

    // Tra ve null neu la bot (honeypot) -> controller van bao thanh cong de bot khong biet
    @Transactional
    public ConsultationRequest create(CreateRequest req, User currentUser, String ip) {
        if (!consultationRateLimiter.tryAcquire(ip == null ? "unknown" : ip)) {
            throw ApiException.tooManyRequests("Bạn gửi quá nhiều yêu cầu, vui lòng thử lại sau ít phút");
        }
        if (req.website() != null && !req.website().isBlank()) {
            log.warn("Honeypot bi dien, bo qua yeu cau tu van tu ip={}", ip);
            return null;
        }

        ConsultationRequest c = new ConsultationRequest();
        c.setFullName(req.fullName().trim());
        c.setEmail(AuthService.normalizeEmail(req.email()));
        c.setPhone(req.phone().trim());
        c.setCompanyName(blankToNull(req.companyName()));
        c.setServiceType(req.serviceType());
        c.setBudgetRange(req.budgetRange() == null ? BudgetRange.UNDECIDED : req.budgetRange());
        c.setMessage(blankToNull(req.message()));
        c.setCustomer(currentUser);
        c.setSource("website");
        c.setIpAddress(ip);
        consultationRepository.save(c);

        String service = SERVICE_LABELS.get(c.getServiceType());
        mailService.send(c.getEmail(), "SeaSoft đã nhận yêu cầu tư vấn của bạn",
                "Xin chào " + c.getFullName() + ",\n\n"
                        + "Cảm ơn bạn đã quan tâm dịch vụ \"" + service + "\" của SeaSoft.\n"
                        + "Chuyên viên sẽ liên hệ với bạn qua số " + c.getPhone() + " trong vòng 24 giờ làm việc.\n\n"
                        + "SeaSoft Team");
        if (!notifyEmail.isBlank()) {
            mailService.send(notifyEmail, "[Lead mới] " + c.getFullName() + " - " + service,
                    "Khách: " + c.getFullName() + " (" + c.getEmail() + ", " + c.getPhone() + ")\n"
                            + "Công ty: " + (c.getCompanyName() == null ? "-" : c.getCompanyName()) + "\n"
                            + "Ngân sách: " + c.getBudgetRange() + "\n\n"
                            + (c.getMessage() == null ? "" : c.getMessage()));
        }
        return c;
    }

    @Transactional(readOnly = true)
    public List<CustomerView> listMine(UUID customerId) {
        return consultationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(CustomerView::from).toList();
    }

    // ---------------- Nhan vien ----------------

    @Transactional(readOnly = true)
    public PageResponse<StaffView> search(Status status, String q, UUID assignedTo, boolean unassigned,
                                          int page, int size) {
        Specification<ConsultationRequest> spec = (root, query, cb) -> {
            // Tranh N+1 khi map assignedStaff; bo qua voi cau count
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("assignedStaff", JoinType.LEFT).fetch("role", JoinType.LEFT);
            }
            return cb.conjunction();
        };
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (assignedTo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assignedStaff").get("id"), assignedTo));
        }
        if (unassigned) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("assignedStaff")));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), like),
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(root.get("phone"), like),
                    cb.like(cb.lower(cb.coalesce(root.get("companyName"), "")), like)));
        }
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(consultationRepository.findAll(spec, pageable), StaffView::from);
    }

    @Transactional(readOnly = true)
    public StaffView get(UUID id) {
        return StaffView.from(load(id));
    }

    @Transactional
    public StaffView update(UUID id, UpdateRequest req, User actor) {
        ConsultationRequest c = load(id);
        ensureCanEdit(c, actor);

        if (req.status() != null && req.status() != c.getStatus()) {
            if (!c.getStatus().canMoveTo(req.status())) {
                throw ApiException.conflict("Không thể chuyển trạng thái từ " + c.getStatus() + " sang " + req.status());
            }
            c.setStatus(req.status());
        }
        if (req.internalNote() != null) {
            c.setInternalNote(blankToNull(req.internalNote()));
        }
        return StaffView.from(c);
    }

    // STAFF chi tu nhan lead chua ai phu trach; MANAGER/ADMIN phan cong/bo phan cong cho bat ky ai
    @Transactional
    public StaffView assign(UUID id, UUID staffId, User actor) {
        ConsultationRequest c = load(id);
        boolean isManager = isManager(actor);

        if (!isManager) {
            if (staffId == null || !staffId.equals(actor.getId())) {
                throw ApiException.forbidden("Nhân viên chỉ được tự nhận lead cho mình");
            }
            if (c.getAssignedStaff() != null && !c.getAssignedStaff().getId().equals(actor.getId())) {
                throw ApiException.conflict("Lead này đã có người phụ trách");
            }
        }

        if (staffId == null) {
            c.setAssignedStaff(null);
        } else {
            User staff = userRepository.findById(staffId)
                    .filter(u -> STAFF_ROLES.contains(u.getRole().getCode()) && u.getStatus() == UserStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("Người được phân công phải là nhân viên đang hoạt động"));
            c.setAssignedStaff(staff);
        }
        return StaffView.from(c);
    }

    @Transactional(readOnly = true)
    public List<StaffSummary> assignableStaff() {
        return userRepository.findActiveByRoleCodes(STAFF_ROLES).stream().map(StaffSummary::from).toList();
    }

    // ---------------- helpers ----------------

    private ConsultationRequest load(UUID id) {
        return consultationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy yêu cầu tư vấn"));
    }

    private void ensureCanEdit(ConsultationRequest c, User actor) {
        if (isManager(actor)) {
            return;
        }
        if (c.getAssignedStaff() == null || !c.getAssignedStaff().getId().equals(actor.getId())) {
            throw ApiException.forbidden("Bạn chỉ được cập nhật lead do mình phụ trách");
        }
    }

    public static boolean isManager(User u) {
        String role = u.getRole().getCode();
        return role.equals("MANAGER") || role.equals("ADMIN");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
