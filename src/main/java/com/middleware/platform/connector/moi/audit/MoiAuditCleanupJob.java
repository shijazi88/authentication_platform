package com.middleware.platform.connector.moi.audit;

import com.middleware.platform.connector.moi.domain.MoiApiCallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Nightly job that deletes MOI audit rows older than the configured retention.
 * Runs at 03:00 server-local. Retention is in days, configurable via
 * {@code platform.moi.audit.retention-days} (default 30).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoiAuditCleanupJob {

    private final MoiApiCallRepository repo;

    @Value("${platform.moi.audit.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "${platform.moi.audit.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = repo.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("MOI audit cleanup · deleted {} rows older than {} days ({})",
                    deleted, retentionDays, cutoff);
        }
    }
}
