package com.middleware.platform.connector.moi.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.platform.connector.moi.domain.MoiApiCall;
import com.middleware.platform.connector.moi.domain.MoiApiCallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists one row per outbound MOI call. Writes are async so the verify
 * hot-path never blocks on audit I/O. Uses REQUIRES_NEW so audit writes
 * don't get rolled back if the caller's transaction aborts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MoiApiCallLogger {

    private final MoiApiCallRepository repo;
    private final ObjectMapper mapper;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(MoiApiCall call) {
        try {
            repo.save(call);
        } catch (Exception ex) {
            // Never let audit failures propagate.
            log.warn("Failed to persist MOI API call audit row", ex);
        }
    }

    public MoiApiCall.MoiApiCallBuilder build(MoiApiCall.Kind kind, String url) {
        return MoiApiCall.builder()
                .id(MoiApiCall.newId())
                .createdAt(Instant.now())
                .kind(kind)
                .url(url)
                .method("POST");
    }

    public String headersJson(HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        headers.forEach((k, values) -> {
            if ("authorization".equalsIgnoreCase(k)) {
                m.put(k, List.of(MoiRedactor.maskAuthorization(String.join(", ", values))));
            } else {
                m.put(k, values);
            }
        });
        try {
            return mapper.writeValueAsString(m);
        } catch (Exception ex) {
            return "{}";
        }
    }

    public String redactAndTruncate(String body) {
        if (body == null) return null;
        return MoiRedactor.truncate(MoiRedactor.redactBody(mapper, body));
    }
}
