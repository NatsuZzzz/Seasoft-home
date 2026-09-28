package com.store.seasoft.Service;

import org.springframework.security.authentication.BadCredentialsException;

// Refresh token sai/het han/da thu hoi -> 401, client phai dang nhap lai
public class InvalidRefreshTokenException extends BadCredentialsException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
