package com.store.seasoft.Service;

import com.store.seasoft.Config.ApiException;
import com.store.seasoft.Dto.PageResponse;
import com.store.seasoft.Dto.ProjectDtos.CreateRequest;
import com.store.seasoft.Dto.ProjectDtos.CustomerDetail;
import com.store.seasoft.Dto.ProjectDtos.CustomerSummary;
import com.store.seasoft.Dto.ProjectDtos.Detail;
import com.store.seasoft.Dto.ProjectDtos.MilestoneRequest;
import com.store.seasoft.Dto.ProjectDtos.MilestoneUpdateRequest;
import com.store.seasoft.Dto.ProjectDtos.MilestoneView;
import com.store.seasoft.Dto.ProjectDtos.PostUpdateRequest;
import com.store.seasoft.Dto.ProjectDtos.Summary;
import com.store.seasoft.Dto.ProjectDtos.UpdateRequest;
import com.store.seasoft.Dto.ProjectDtos.UpdateView;
import com.store.seasoft.Model.ConsultationEnums;
import com.store.seasoft.Model.ConsultationRequest;
import com.store.seasoft.Model.Project;
import com.store.seasoft.Model.ProjectMilestone;
import com.store.seasoft.Model.ProjectUpdate;
import com.store.seasoft.Model.User;
import com.store.seasoft.Model.UserStatus;
import com.store.seasoft.Repository.ConsultationRepository;
import com.store.seasoft.Repository.ProjectMilestoneRepository;
import com.store.seasoft.Repository.ProjectRepository;
import com.store.seasoft.Repository.ProjectUpdateRepository;
import com.store.seasoft.Repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    // 5 moc mac dinh = dung "Quy trinh lam viec" tren trang chu
    private static final List<MilestoneRequest> DEFAULT_MILESTONES = List.of(
            new MilestoneRequest("Tư vấn & khảo sát", "Chốt yêu cầu, phạm vi và kiến trúc website", null),
            new MilestoneRequest("UI/UX Design", "Wireframe và giao diện mẫu để khách duyệt", null),
            new MilestoneRequest("Development", "Lập trình front-end, back-end, tích hợp", null),
            new MilestoneRequest("Testing", "Kiểm thử đa thiết bị, tốc độ, bảo mật", null),
            new MilestoneRequest("Launch", "Triển khai tên miền, bàn giao và hướng dẫn sử dụng", null));

    private final ProjectRepository projectRepository;
    private final ProjectMilestoneRepository milestoneRepository;
    private final ProjectUpdateRepository updateRepository;
    private final ConsultationRepository consultationRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ======================= NHAN VIEN =======================

    @Transactional
    public Detail create(CreateRequest req, User actor) {
        boolean manager = ConsultationService.isManager(actor);
        Project p = new Project();
        User customer;

        if (req.consultationId() != null) {
            ConsultationRequest lead = consultationRepository.findById(req.consultationId())
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy yêu cầu tư vấn"));
            if (lead.getStatus() != ConsultationEnums.Status.WON) {
                throw ApiException.conflict("Chỉ tạo dự án từ lead đã chốt (Thành công)");
            }
            if (projectRepository.existsByConsultationId(lead.getId())) {
                throw ApiException.conflict("Lead này đã có dự án");
            }
            if (!manager && (lead.getAssignedStaff() == null || !lead.getAssignedStaff().getId().equals(actor.getId()))) {
                throw ApiException.forbidden("Bạn chỉ tạo được dự án từ lead mình phụ trách");
            }
            customer = lead.getCustomer() != null ? lead.getCustomer()
                    : findCustomer(req.customerEmail() != null ? req.customerEmail() : lead.getEmail());
            p.setConsultation(lead);
            p.setServiceType(req.serviceType() != null ? req.serviceType() : lead.getServiceType());
            p.setManager(lead.getAssignedStaff());
        } else {
            if (!manager) {
                throw ApiException.forbidden("Nhân viên chỉ tạo dự án từ lead mình phụ trách");
            }
            if (req.customerEmail() == null || req.customerEmail().isBlank()) {
                throw new IllegalArgumentException("Vui lòng chọn lead hoặc nhập email khách hàng");
            }
            if (req.serviceType() == null) {
                throw new IllegalArgumentException("Vui lòng chọn loại dịch vụ");
            }
            customer = findCustomer(req.customerEmail());
            p.setServiceType(req.serviceType());
        }

        checkDates(req.startDate(), req.dueDate());
        if (req.managerId() != null) {
            if (!manager && !req.managerId().equals(actor.getId())) {
                throw ApiException.forbidden("Chỉ quản lý mới được chỉ định người phụ trách khác");
            }
            p.setManager(findStaff(req.managerId()));
        }
        if (p.getManager() == null) {
            p.setManager(actor);
        }

        p.setCode("SS-" + Year.now().getValue() + "-" + String.format("%04d", projectRepository.nextCodeNumber()));
        p.setName(req.name().trim());
        p.setCustomer(customer);
        p.setDescription(blankToNull(req.description()));
        p.setStartDate(req.startDate());
        p.setDueDate(req.dueDate());

        List<MilestoneRequest> ms = req.milestones() == null || req.milestones().isEmpty()
                ? DEFAULT_MILESTONES : req.milestones();
        for (MilestoneRequest m : ms) {
            addMilestoneTo(p, m);
        }
        projectRepository.save(p);

        mailService.send(customer.getEmail(), "SeaSoft - Dự án " + p.getCode() + " đã được khởi tạo",
                "Xin chào " + customer.getFullName() + ",\n\n"
                        + "Dự án \"" + p.getName() + "\" (" + p.getCode() + ") đã được tạo.\n"
                        + "Theo dõi tiến độ tại: " + frontendUrl + "/project.html?id=" + p.getId() + "\n\n"
                        + "SeaSoft Team");
        return detail(p, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<Summary> search(Project.Status status, String q, UUID managerId, int page, int size) {
        Specification<Project> spec = (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("customer", JoinType.LEFT);
                root.fetch("manager", JoinType.LEFT).fetch("role", JoinType.LEFT);
            }
            return cb.conjunction();
        };
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (managerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("manager").get("id"), managerId));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> {
                var customer = root.join("customer", JoinType.LEFT);
                return cb.or(cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(cb.lower(customer.get("fullName")), like),
                        cb.like(cb.lower(customer.get("email")), like));
            });
        }
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        return PageResponse.of(projectRepository.findAll(spec, pageable), Summary::from);
    }

    @Transactional(readOnly = true)
    public Detail get(UUID id) {
        return detail(load(id), true);
    }

    @Transactional
    public Detail update(UUID id, UpdateRequest req, User actor) {
        Project p = load(id);
        ensureCanEdit(p, actor);

        if (req.name() != null && !req.name().isBlank()) {
            p.setName(req.name().trim());
        }
        if (req.status() != null) {
            p.setStatus(req.status());
        }
        if (req.description() != null) {
            p.setDescription(blankToNull(req.description()));
        }
        LocalDate start = req.startDate() != null ? req.startDate() : p.getStartDate();
        LocalDate due = req.dueDate() != null ? req.dueDate() : p.getDueDate();
        checkDates(start, due);
        p.setStartDate(start);
        p.setDueDate(due);
        if (req.managerId() != null) {
            if (!ConsultationService.isManager(actor)) {
                throw ApiException.forbidden("Chỉ quản lý mới được đổi người phụ trách");
            }
            p.setManager(findStaff(req.managerId()));
        }
        return detail(p, true);
    }

    @Transactional
    public MilestoneView addMilestone(UUID projectId, MilestoneRequest req, User actor) {
        Project p = load(projectId);
        ensureCanEdit(p, actor);
        ProjectMilestone m = addMilestoneTo(p, req);
        milestoneRepository.save(m);
        return MilestoneView.from(m);
    }

    @Transactional
    public MilestoneView updateMilestone(UUID projectId, UUID milestoneId, MilestoneUpdateRequest req, User actor) {
        Project p = load(projectId);
        ensureCanEdit(p, actor);
        ProjectMilestone m = milestoneRepository.findByIdAndProjectId(milestoneId, projectId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy mốc tiến độ"));
        if (req.title() != null && !req.title().isBlank()) {
            m.setTitle(req.title().trim());
        }
        if (req.dueDate() != null) {
            m.setDueDate(req.dueDate());
        }
        if (req.status() != null) {
            m.changeStatus(req.status());
            // Bat dau lam moc dau tien -> du an chuyen sang "Dang thuc hien"
            if (req.status() != ProjectMilestone.Status.TODO && p.getStatus() == Project.Status.PLANNING) {
                p.setStatus(Project.Status.IN_PROGRESS);
            }
        }
        return MilestoneView.from(m);
    }

    @Transactional
    public void deleteMilestone(UUID projectId, UUID milestoneId, User actor) {
        Project p = load(projectId);
        ensureCanEdit(p, actor);
        ProjectMilestone m = milestoneRepository.findByIdAndProjectId(milestoneId, projectId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy mốc tiến độ"));
        p.getMilestones().remove(m);
    }

    @Transactional
    public UpdateView postUpdate(UUID projectId, PostUpdateRequest req, User actor) {
        Project p = load(projectId);
        ensureCanEdit(p, actor);
        boolean visible = req.visibleToCustomer() == null || req.visibleToCustomer();
        ProjectUpdate u = saveUpdate(p, actor, req.content(), visible);
        if (visible) {
            mailService.send(p.getCustomer().getEmail(), "SeaSoft - Cập nhật dự án " + p.getCode(),
                    actor.getFullName() + " vừa cập nhật dự án \"" + p.getName() + "\":\n\n" + u.getContent()
                            + "\n\nXem chi tiết: " + frontendUrl + "/project.html?id=" + p.getId());
        }
        return UpdateView.from(u);
    }

    // ======================= KHACH HANG =======================

    @Transactional(readOnly = true)
    public List<CustomerSummary> listMine(UUID customerId) {
        return projectRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(CustomerSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public CustomerDetail getMine(UUID projectId, UUID customerId) {
        Project p = loadMine(projectId, customerId);
        return new CustomerDetail(CustomerSummary.from(p), p.getDescription(),
                p.getMilestones().stream().map(MilestoneView::from).toList(),
                updateRepository.findByProjectIdAndVisibleToCustomerTrueOrderByCreatedAtDesc(p.getId()).stream()
                        .map(UpdateView::from).toList());
    }

    @Transactional
    public UpdateView comment(UUID projectId, String content, User customer) {
        Project p = loadMine(projectId, customer.getId());
        ProjectUpdate u = saveUpdate(p, customer, content, true);
        if (p.getManager() != null) {
            mailService.send(p.getManager().getEmail(), "[" + p.getCode() + "] Khách hàng vừa bình luận",
                    customer.getFullName() + ":\n\n" + u.getContent());
        }
        return UpdateView.from(u);
    }

    // ======================= helpers =======================

    private Project load(UUID id) {
        return projectRepository.findWithDetailsById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy dự án"));
    }

    // Du an cua nguoi khac cung tra 404 (khong tiet lo la du an co ton tai)
    private Project loadMine(UUID id, UUID customerId) {
        return projectRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy dự án"));
    }

    private Detail detail(Project p, boolean includeHidden) {
        List<UpdateView> updates = (includeHidden
                ? updateRepository.findByProjectIdOrderByCreatedAtDesc(p.getId())
                : updateRepository.findByProjectIdAndVisibleToCustomerTrueOrderByCreatedAtDesc(p.getId()))
                .stream().map(UpdateView::from).toList();
        return new Detail(Summary.from(p), p.getDescription(),
                p.getConsultation() == null ? null : p.getConsultation().getId(),
                p.getMilestones().stream().map(MilestoneView::from).toList(), updates);
    }

    private ProjectMilestone addMilestoneTo(Project p, MilestoneRequest req) {
        ProjectMilestone m = new ProjectMilestone();
        m.setProject(p);
        m.setTitle(req.title().trim());
        m.setDescription(blankToNull(req.description()));
        m.setDueDate(req.dueDate());
        m.setSortOrder(p.getMilestones().stream().mapToInt(ProjectMilestone::getSortOrder).max().orElse(0) + 1);
        p.getMilestones().add(m);
        return m;
    }

    private ProjectUpdate saveUpdate(Project p, User author, String content, boolean visible) {
        ProjectUpdate u = new ProjectUpdate();
        u.setProject(p);
        u.setAuthor(author);
        u.setContent(content.trim());
        u.setVisibleToCustomer(visible);
        return updateRepository.save(u);
    }

    private void ensureCanEdit(Project p, User actor) {
        if (ConsultationService.isManager(actor)) {
            return;
        }
        if (p.getManager() == null || !p.getManager().getId().equals(actor.getId())) {
            throw ApiException.forbidden("Bạn chỉ được cập nhật dự án do mình phụ trách");
        }
    }

    private User findCustomer(String email) {
        String normalized = AuthService.normalizeEmail(email);
        return userRepository.findByEmail(normalized)
                .filter(u -> u.getRole().getCode().equals("CUSTOMER"))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Khách hàng " + normalized + " chưa có tài khoản. Hãy mời khách đăng ký bằng email này."));
    }

    private User findStaff(UUID id) {
        return userRepository.findById(id)
                .filter(u -> ConsultationService.STAFF_ROLES.contains(u.getRole().getCode())
                        && u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Người phụ trách phải là nhân viên đang hoạt động"));
    }

    private static void checkDates(LocalDate start, LocalDate due) {
        if (start != null && due != null && due.isBefore(start)) {
            throw new IllegalArgumentException("Hạn hoàn thành phải sau ngày bắt đầu");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
