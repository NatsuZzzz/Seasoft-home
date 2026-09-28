package com.store.seasoft.Service;

import com.store.seasoft.Config.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

// Chong do mat khau / spam mail / tao tai khoan hang loat.
// Moi hanh dong gioi han theo IP (chong quet nhieu tai khoan) va theo email (chong do 1 tai khoan tu nhieu IP).
@Component
public class AuthRateLimits {

    private static final String MSG = "Bạn thao tác quá nhiều lần, vui lòng thử lại sau ít phút";

    private final RateLimiter loginByIp;
    private final RateLimiter loginByEmail;
    private final RateLimiter forgotByIp;
    private final RateLimiter forgotByEmail;
    private final RateLimiter registerByIp;

    public AuthRateLimits(
            @Value("${app.rate-limit.login.ip-max:30}") int loginIp,
            @Value("${app.rate-limit.login.email-max:10}") int loginEmail,
            @Value("${app.rate-limit.login.window-minutes:15}") long loginWindow,
            @Value("${app.rate-limit.forgot.ip-max:10}") int forgotIp,
            @Value("${app.rate-limit.forgot.email-max:3}") int forgotEmail,
            @Value("${app.rate-limit.forgot.window-minutes:15}") long forgotWindow,
            @Value("${app.rate-limit.register.ip-max:10}") int registerIp,
            @Value("${app.rate-limit.register.window-minutes:60}") long registerWindow) {
        Clock clock = Clock.systemUTC();
        loginByIp = new RateLimiter(loginIp, Duration.ofMinutes(loginWindow), clock);
        loginByEmail = new RateLimiter(loginEmail, Duration.ofMinutes(loginWindow), clock);
        forgotByIp = new RateLimiter(forgotIp, Duration.ofMinutes(forgotWindow), clock);
        forgotByEmail = new RateLimiter(forgotEmail, Duration.ofMinutes(forgotWindow), clock);
        registerByIp = new RateLimiter(registerIp, Duration.ofMinutes(registerWindow), clock);
    }

    public void checkLogin(String ip, String email) {
        require(loginByIp.tryAcquire(key(ip)) && loginByEmail.tryAcquire(key(email)));
    }

    public void checkForgot(String ip, String email) {
        require(forgotByIp.tryAcquire(key(ip)) && forgotByEmail.tryAcquire(key(email)));
    }

    public void checkRegister(String ip) {
        require(registerByIp.tryAcquire(key(ip)));
    }

    private static String key(String s) {
        return s == null ? "unknown" : s;
    }

    private static void require(boolean allowed) {
        if (!allowed) {
            throw ApiException.tooManyRequests(MSG);
        }
    }
}
