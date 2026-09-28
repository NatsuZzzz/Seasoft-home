package com.store.seasoft.Controller;

import com.store.seasoft.Dto.UserDtos.ChangePasswordRequest;
import com.store.seasoft.Dto.UserDtos.UpdateProfileRequest;
import com.store.seasoft.Dto.UserDtos.UserResponse;
import com.store.seasoft.Service.UserPrincipal;
import com.store.seasoft.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getMe(principal.getUser().getId());
    }

    @PutMapping
    public UserResponse update(@AuthenticationPrincipal UserPrincipal principal,
                               @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateMe(principal.getUser().getId(), request);
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getUser().getId(), request);
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }
}
