package com.middleware.platform.gateway.orchestrator;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.transactions.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory per-minute rate limiter + monthly quota checker.
 *
 * <p><b>Rate limit</b> — a sliding-minute counter keyed by
 * {@code (credentialId, operationId)}. Resets every 60 seconds.
 * No external dependency (no Redis). Suitable for single-instance.
 *
 * <p><b>Monthly quota</b> — queries the {@code transactions} table once per
 * tenant-per-period and caches the result for 60 seconds so a burst doesn't
 * hammer the DB with COUNT(*) queries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final TransactionRepository transactionRepository;

    // Rate limit: per-minute buckets.
    // Key: "credentialId:operationId", Value: counter + minute boundary.
    private final ConcurrentHashMap<String, MinuteBucket> rateBuckets = new ConcurrentHashMap<>();

    // Quota: cached monthly count per tenant.
    // Key: "tenantId:YYYY-MM", Value: last known count + timestamp of last check.
    private final ConcurrentHashMap<String, QuotaCache> quotaCache = new ConcurrentHashMap<>();

    /**
     * @throws ApplicationException with {@link ErrorCode#QUOTA_EXCEEDED} if
     *         the per-minute rate limit or the monthly quota is breached.
     */
    public void check(UUID credentialId, UUID tenantId, UUID operationId,
                      Integer rateLimitPerMinute, Long monthlyQuota) {
        // Per-minute rate limit
        if (rateLimitPerMinute != null && rateLimitPerMinute > 0) {
            String rateKey = credentialId + ":" + operationId;
            long nowMinute = System.currentTimeMillis() / 60_000;
            MinuteBucket bucket = rateBuckets.compute(rateKey, (k, existing) -> {
                if (existing == null || existing.minute != nowMinute) {
                    return new MinuteBucket(nowMinute, new AtomicInteger(1));
                }
                existing.count.incrementAndGet();
                return existing;
            });
            if (bucket.count.get() > rateLimitPerMinute) {
                log.warn("Rate limit exceeded: credential={} operation={} count={} limit={}",
                        credentialId, operationId, bucket.count.get(), rateLimitPerMinute);
                throw new ApplicationException(ErrorCode.QUOTA_EXCEEDED,
                        "Rate limit exceeded (" + rateLimitPerMinute + " calls/minute)");
            }
        }

        // Monthly quota
        if (monthlyQuota != null && monthlyQuota > 0) {
            YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
            String quotaKey = tenantId + ":" + currentMonth;
            long nowMs = System.currentTimeMillis();
            QuotaCache cached = quotaCache.get(quotaKey);

            long currentCount;
            if (cached != null && (nowMs - cached.checkedAtMs) < 60_000) {
                // Use the cache for 60 seconds to avoid hammering DB.
                currentCount = cached.count.incrementAndGet();
            } else {
                // Refresh from DB.
                Instant monthStart = currentMonth.atDay(1)
                        .atStartOfDay(ZoneOffset.UTC).toInstant();
                Instant monthEnd = currentMonth.plusMonths(1).atDay(1)
                        .atStartOfDay(ZoneOffset.UTC).toInstant();
                long dbCount = transactionRepository.countByTenantIdAndCreatedAtBetween(
                        tenantId, monthStart, monthEnd);
                QuotaCache newCache = new QuotaCache(new AtomicLong(dbCount + 1), nowMs);
                quotaCache.put(quotaKey, newCache);
                currentCount = newCache.count.get();
            }

            if (currentCount > monthlyQuota) {
                log.warn("Monthly quota exceeded: tenant={} count={} quota={}",
                        tenantId, currentCount, monthlyQuota);
                throw new ApplicationException(ErrorCode.QUOTA_EXCEEDED,
                        "Monthly quota exceeded (" + monthlyQuota + " calls/month)");
            }
        }
    }

    private record MinuteBucket(long minute, AtomicInteger count) {}
    private record QuotaCache(AtomicLong count, long checkedAtMs) {}
}
