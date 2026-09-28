package com.store.seasoft.Controller;

import com.store.seasoft.Dto.ConsultationDtos.CreateRequest;
import com.store.seasoft.Dto.ConsultationDtos.CreateResponse;
import com.store.seasoft.Dto.ConsultationDtos.CustomerView;
import com.store.seasoft.Model.ConsultationRequest;
import com.store.seasoft.Service.ConsultationService;
import com.store.seasoft.Service.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private static final String THANKS = "Cảm ơn bạn! SeaSoft sẽ liên hệ trong vòng 24 giờ làm việc.";

    private final ConsultationService consultationService;

    // Cong khai: ai cung gui duoc; neu dang dang nhap thi gan voi tai khoan
    @PostMapping
    public ResponseEntity<CreateResponse> create(@Valid @RequestBody CreateRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal,
                                                 HttpServletRequest http) {
        ConsultationRequest c = consultationService.create(request,
                principal == null ? null : principal.getUser(), http.getRemoteAddr());
        // Bot dien honeypot: van tra 201 nhu that de bot khong do ra
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateResponse(c == null ? null : c.getId(), THANKS));
    }

    @GetMapping("/mine")
    public List<CustomerView> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return consultationService.listMine(principal.getUser().getId());
    }
}
