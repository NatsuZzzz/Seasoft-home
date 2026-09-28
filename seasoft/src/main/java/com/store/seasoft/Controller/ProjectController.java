package com.store.seasoft.Controller;

import com.store.seasoft.Dto.ProjectDtos.CustomerDetail;
import com.store.seasoft.Dto.ProjectDtos.CustomerSummary;
import com.store.seasoft.Dto.ProjectDtos.UpdateView;
import com.store.seasoft.Service.ProjectService;
import com.store.seasoft.Service.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Khach hang xem du an cua chinh minh
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    public record CommentRequest(
            @NotBlank(message = "Nội dung không được để trống")
            @Size(max = 2000, message = "Bình luận tối đa 2000 ký tự") String content) {
    }

    private final ProjectService projectService;

    @GetMapping
    public List<CustomerSummary> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return projectService.listMine(principal.getUser().getId());
    }

    @GetMapping("/{id}")
    public CustomerDetail get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.getMine(id, principal.getUser().getId());
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<UpdateView> comment(@PathVariable UUID id, @Valid @RequestBody CommentRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.comment(id, request.content(), principal.getUser()));
    }
}
