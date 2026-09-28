package com.store.seasoft.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Don token het han / da dung moi dem de bang khong phinh mai
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupJob {

    private final JdbcTemplate jdbc;

    @Scheduled(cron = "${app.cleanup.cron:0 30 3 * * *}")
    @Transactional
    public int cleanup() {
        int refresh = jdbc.update("delete from refresh_tokens where expires_at < now() - interval '1 day' "
                + "or (revoked and created_at < now() - interval '30 days')");
        int reset = jdbc.update("delete from password_reset_tokens where expires_at < now() - interval '1 day' "
                + "or used_at < now() - interval '1 day'");
        if (refresh + reset > 0) {
            log.info("Da don {} refresh token va {} reset token cu", refresh, reset);
        }
        return refresh + reset;
    }
}
