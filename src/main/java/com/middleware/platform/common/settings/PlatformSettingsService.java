package com.middleware.platform.common.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Typed access to admin-editable runtime settings. Values are JSON documents
 * keyed by name; each caller supplies its own defaults so a key that was never
 * saved still yields a complete, valid object.
 *
 * <p>Reads are served from a short-lived in-memory cache (one backend instance
 * per environment today) so the verify hot path costs no DB round-trip; a
 * write refreshes the cache immediately.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformSettingsService {

    private static final long CACHE_TTL_MS = 15_000;

    private final PlatformSettingRepository repository;
    private final ObjectMapper objectMapper;

    private record Cached(Object value, long loadedAt) {}
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public <T> T get(String key, Class<T> type, Supplier<T> defaults) {
        Cached c = cache.get(key);
        long now = System.currentTimeMillis();
        if (c != null && now - c.loadedAt() < CACHE_TTL_MS && type.isInstance(c.value())) {
            return type.cast(c.value());
        }
        T value = repository.findById(key)
                .map(s -> parse(key, s.getValueJson(), type, defaults))
                .orElseGet(defaults);
        cache.put(key, new Cached(value, now));
        return value;
    }

    @Transactional
    public <T> T put(String key, T value, String updatedBy) {
        String json;
        try {
            json = objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new ApplicationException(ErrorCode.INTERNAL_ERROR, "Cannot serialise setting " + key, ex);
        }
        PlatformSetting row = repository.findById(key).orElseGet(() -> PlatformSetting.builder().key(key).build());
        row.setValueJson(json);
        row.setUpdatedBy(updatedBy);
        row.setUpdatedAt(Instant.now());
        repository.save(row);
        cache.put(key, new Cached(value, System.currentTimeMillis()));
        log.info("Platform setting {} updated by {}", key, updatedBy);
        return value;
    }

    /** Last-updated metadata for the admin UI (null when the key was never saved). */
    @Transactional(readOnly = true)
    public PlatformSetting meta(String key) {
        return repository.findById(key).orElse(null);
    }

    private <T> T parse(String key, String json, Class<T> type, Supplier<T> defaults) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            // A stored document that no longer matches the type must not take the
            // verify path down — fall back to defaults and log loudly.
            log.error("Platform setting {} is unreadable, using defaults: {}", key, ex.getMessage());
            return defaults.get();
        }
    }
}
