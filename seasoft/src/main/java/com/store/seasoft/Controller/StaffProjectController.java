package com.store.seasoft.Controller;

import com.store.seasoft.Dto.PageResponse;
import com.store.seasoft.Dto.ProjectDtos.CreateRequest;
import com.store.seasoft.Dto.ProjectDtos.Detail;
import com.store.seasoft.Dto.ProjectDtos.MilestoneRequest;
import com.store.seasoft.Dto.ProjectDtos.MilestoneUpdateRequest;
import com.store.seasoft.Dto.ProjectDtos.MilestoneView;
import com.store.seasoft.Dto.ProjectDtos.PostUpdateRequest;
import com.store.seasoft.Dto.ProjectDtos.Summary;
import com.store.seasoft.Dto.ProjectDtos.UpdateRequest;
import com.store.seasoft.Dto.ProjectDtos.UpdateView;
import com.store.seasoft.Model.Project;
import com.store.seasoft.Service.ProjectService;
import com.store.seasoft.Service.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// Quan ly du an cho STAFF / MANAGER / ADMIN
@RestController
@RequestMapping("/api/staff/projects")
@RequiredArgsConstructor
public class StaffProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<Detail> create(@Valid @RequestBody CreateRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request, principal.getUser()));
    }

    @GetMapping
    public PageResponse<Summary> search(@RequestParam(required = false) Project.Status status,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(defaultValue = "false") boolean mine,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.search(status, q, mine ? principal.getUser().getId() : null, page, size);
    }

    @GetMapping("/{id}")
    public Detail get(@PathVariable UUID id) {
        return projectService.get(id);
    }

    @PatchMapping("/{id}")
    public Detail update(@PathVariable UUID id, @Valid @RequestBody UpdateRequest request,
                         @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.update(id, request, principal.getUser());
    }

    @PostMapping("/{id}/milestones")
    public ResponseEntity<MilestoneView> addMilestone(@PathVariable UUID id, @Valid @RequestBody MilestoneRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.addMilestone(id, request, principal.getUser()));
    }

    @PatchMapping("/{id}/milestones/{milestoneId}")
    public MilestoneView updateMilestone(@PathVariable UUID id, @PathVariable UUID milestoneId,
                                         @Valid @RequestBody MilestoneUpdateRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.updateMilestone(id, milestoneId, request, principal.getUser());
    }

    @DeleteMapping("/{id}/milestones/{milestoneId}")
    public ResponseEntity<Void> deleteMilestone(@PathVariable UUID id, @PathVariable UUID milestoneId,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        projectService.deleteMilestone(id, milestoneId, principal.getUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/updates")
    public ResponseEntity<UpdateView> postUpdate(@PathVariable UUID id, @Valid @RequestBody PostUpdateRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.postUpdate(id, request, principal.getUser()));
    }
}
