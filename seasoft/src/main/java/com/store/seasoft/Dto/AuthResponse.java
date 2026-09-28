package com.store.seasoft.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {

    // Access token (JWT, song ngan) - gui kem header Authorization: Bearer <token>
    private String token;
    // Refresh token (song dai) - chi dung de goi /api/auth/refresh
    private String refreshToken;
    private String email;
    private String fullName;
    private String role;
}
