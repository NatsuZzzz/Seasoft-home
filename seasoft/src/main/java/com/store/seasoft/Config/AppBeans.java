package com.store.seasoft.Config;

import com.store.seasoft.Service.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class AppBeans {

    // Form tu van cong khai: mac dinh toi da 5 lan / 10 phut / IP
    @Bean
    public RateLimiter consultationRateLimiter(
            @Value("${app.rate-limit.consultation.max:5}") int max,
            @Value("${app.rate-limit.consultation.window-minutes:10}") long windowMinutes) {
        return new RateLimiter(max, Duration.ofMinutes(windowMinutes), Clock.systemUTC());
    }
}
