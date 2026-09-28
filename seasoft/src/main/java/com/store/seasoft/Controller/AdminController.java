package com.store.seasoft.Controller;

import com.store.seasoft.Dto.AdminDtos.CreateStaffRequest;
import com.store.seasoft.Dto.AdminDtos.RoleRequest;
import com.store.seasoft.Dto.AdminDtos.Stats;
import com.store.seasoft.Dto.AdminDtos.StatusRequest;
import com.store.seasoft.Dto.AdminDtos.UpdateUserRequest;
import com.store.seasoft.Dto.AdminDtos.UserView;
import com.store.seasoft.Dto.PageResponse;
import com.store.seasoft.Model.UserStatus;
import com.store.seasoft.Service.AdminUserService;
import com.store.seasoft.Service.StatsService;
import com.store.seasoft.Service.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

// /api/admin/** chi MANAGER va ADMIN (SecurityConfig); quy tac chi tiet trong AdminUserService
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;
    private final StatsService statsService;

    @GetMapping("/users")
    public PageResponse<UserView> users(@RequestParam(required = false) String role,
                                        @RequestParam(required = false) UserStatus status,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return adminUserService.search(role, status, q, page, size);
    }

    @GetMapping("/users/{id}")
    public UserView user(@PathVariable UUID id) {
        return adminUserService.get(id);
    }

    @PostMapping("/users")
    public ResponseEntity<UserView> createStaff(@Valid @RequestBody CreateStaffRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminUserService.createStaff(request, principal.getUser()));
    }

    @PatchMapping("/users/{id}")
    public UserView update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request,
                           @AuthenticationPrincipal UserPrincipal principal) {
        return adminUserService.update(id, request, principal.getUser());
    }

    @PatchMapping("/users/{id}/status")
    public UserView status(@PathVariable UUID id, @Valid @RequestBody StatusRequest request,
                           @AuthenticationPrincipal UserPrincipal principal) {
        return adminUserService.changeStatus(id, request.status(), principal.getUser());
    }

    @PatchMapping("/users/{id}/role")
    public UserView role(@PathVariable UUID id, @Valid @RequestBody RoleRequest request,
                         @AuthenticationPrincipal UserPrincipal principal) {
        return adminUserService.changeRole(id, request.role(), principal.getUser());
    }

    @PostMapping("/users/{id}/reset-password")
    public Map<String, String> resetPassword(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        adminUserService.sendPasswordReset(id, principal.getUser());
        return Map.of("message", "Đã gửi link đặt lại mật khẩu tới email người dùng");
    }

    @GetMapping("/stats")
    public Stats stats() {
        return statsService.overview();
    }
}
