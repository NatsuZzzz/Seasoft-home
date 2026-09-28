package com.store.seasoft.Service;

import com.store.seasoft.Model.RefreshToken;
import com.store.seasoft.Model.User;
import com.store.seasoft.Repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    // Tra ve token goc cho client, DB chi luu hash
    @Transactional
    public String issue(User user) {
        String raw = TokenUtils.randomToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(TokenUtils.sha256(raw));
        token.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        refreshTokenRepository.save(token);
        return raw;
    }

    // Rotation: token cu bi thu hoi, tra ve user de cap cap token moi.
    // Neu token da bi thu hoi ma van duoc dung lai -> co the bi danh cap,
    // thu hoi toan bo phien cua user do.
    // noRollbackFor: viec thu hoi phai duoc commit du sau do nem loi 401.
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public User rotate(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(TokenUtils.sha256(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token không hợp lệ"));

        if (token.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(token.getUser().getId());
            throw new InvalidRefreshTokenException("Refresh token đã bị thu hồi");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token đã hết hạn");
        }

        token.setRevoked(true);
        // user la LAZY proxy -> unproxy de dung duoc sau khi transaction dong
        return (User) Hibernate.unproxy(token.getUser());
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(TokenUtils.sha256(rawToken))
                .ifPresent(t -> t.setRevoked(true));
    }

    @Transactional
    public void revokeAll(User user) {
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }
}
