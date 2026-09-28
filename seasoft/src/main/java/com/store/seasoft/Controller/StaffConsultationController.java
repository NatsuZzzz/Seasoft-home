package com.store.seasoft.Controller;

import com.store.seasoft.Dto.ConsultationDtos.AssignRequest;
import com.store.seasoft.Dto.ConsultationDtos.StaffSummary;
import com.store.seasoft.Dto.ConsultationDtos.StaffView;
import com.store.seasoft.Dto.ConsultationDtos.UpdateRequest;
import com.store.seasoft.Dto.PageResponse;
import com.store.seasoft.Model.ConsultationEnums.Status;
import com.store.seasoft.Service.ConsultationService;
import com.store.seasoft.Service.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// /api/staff/** chi danh cho STAFF, MANAGER, ADMIN (xem SecurityConfig)
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffConsultationController {

    private final ConsultationService consultationService;

    @GetMapping("/consultations")
    public PageResponse<StaffView> search(@RequestParam(required = false) Status status,
                                          @RequestParam(required = false) String q,
                                          @RequestParam(required = false) UUID assignedTo,
                                          @RequestParam(defaultValue = "false") boolean unassigned,
                                          @RequestParam(defaultValue = "false") boolean mine,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        UUID assignee = mine ? principal.getUser().getId() : assignedTo;
        return consultationService.search(status, q, assignee, unassigned, page, size);
    }

    @GetMapping("/consultations/{id}")
    public StaffView get(@PathVariable UUID id) {
        return consultationService.get(id);
    }

    @PatchMapping("/consultations/{id}")
    public StaffView update(@PathVariable UUID id, @Valid @RequestBody UpdateRequest request,
                            @AuthenticationPrincipal UserPrincipal principal) {
        return consultationService.update(id, request, principal.getUser());
    }

    @PatchMapping("/consultations/{id}/assign")
    public StaffView assign(@PathVariable UUID id, @RequestBody AssignRequest request,
                            @AuthenticationPrincipal UserPrincipal principal) {
        return consultationService.assign(id, request.staffId(), principal.getUser());
    }

    @GetMapping("/assignees")
    public List<StaffSummary> assignees() {
        return consultationService.assignableStaff();
    }
}
