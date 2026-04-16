package com.middleware.platform.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Local Caffeine-backed cache for the entitlement hot path.
 *
 * <p>Cache names registered here:
 * <ul>
 *   <li>{@code planFieldPaths} — set of visible field paths per plan id, read on
 *       every verify request and rarely changed (only on plan edits, which use
 *       {@code @CacheEvict}).</li>
 * </ul>
 *
 * <p>Single-node only — for multi-instance deployments swap the
 * {@link CaffeineCacheManager} for a Redis-backed manager.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_PLAN_FIELD_PATHS = "planFieldPaths";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager();
        mgr.setCacheNames(List.of(CACHE_PLAN_FIELD_PATHS));
        mgr.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1_000));
        return mgr;
    }
}
