package com.middleware.platform.connector.moi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.connector.moi.audit.MoiApiCallLogger;
import com.middleware.platform.connector.moi.audit.MoiRedactor;
import com.middleware.platform.connector.moi.domain.MoiApiCall;
import com.middleware.platform.connector.moi.domain.MoiCredentials;
import com.middleware.platform.connector.moi.domain.MoiCredentialsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * Fetches and caches JWTs from the MOI authentication API.
 *
 * <p>Single in-memory cache per process. Concurrent verify calls share one
 * login round-trip via a {@link ReentrantLock}. Token TTL is read from the
 * JWT's {@code exp} claim (minus a refresh-skew safety margin); if the
 * claim is absent, falls back to 50 minutes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MoiTokenService {

    private final MoiCredentialsRepository credentialsRepo;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper mapper;
    private final MoiApiCallLogger audit;

    private final AtomicReference<CachedToken> cache = new AtomicReference<>(null);
    private final ReentrantLock refreshLock = new ReentrantLock();

    /** Returns a valid bearer token, fetching a fresh one if needed. */
    public String getToken() {
        CachedToken current = cache.get();
        if (current != null && Instant.now().isBefore(current.expiresAt())) {
            return current.token();
        }
        return fetchFreshToken();
    }

    /** Clear cached token — forces next call to re-login. Called on 401 from verify. */
    public void invalidate() {
        cache.set(null);
    }

    /** Force-refresh. Exposed for the admin "Test connection" endpoint. */
    public TokenResult fetchForTest() {
        try {
            String token = fetchFreshToken();
            CachedToken ct = cache.get();
            return new TokenResult(true, token, ct != null ? ct.expiresAt() : null, null);
        } catch (ApplicationException ex) {
            return new TokenResult(false, null, null, ex.getMessage());
        } catch (Exception ex) {
            return new TokenResult(false, null, null, ex.getMessage());
        }
    }

    private String fetchFreshToken() {
        refreshLock.lock();
        try {
            // Double-check: another thread may have refreshed while we waited.
            CachedToken current = cache.get();
            if (current != null && Instant.now().isBefore(current.expiresAt())) {
                return current.token();
            }

            MoiCredentials creds = credentialsRepo.getSingleton();
            CachedToken fresh = doLogin(creds);
            cache.set(fresh);
            return fresh.token();
        } finally {
            refreshLock.unlock();
        }
    }

    private CachedToken doLogin(MoiCredentials creds) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", creds.getUsername());
        body.put("password", creds.getPassword());
        body.put("domain", creds.getDomain());

        String requestBodyJson;
        try {
            requestBodyJson = mapper.writeValueAsString(body);
        } catch (Exception ex) {
            throw new ApplicationException(ErrorCode.INTERNAL_ERROR, "Failed to serialize MOI login body", ex);
        }

        MoiApiCall.MoiApiCallBuilder auditBuilder = audit.build(MoiApiCall.Kind.AUTH, creds.getAuthUrl())
                .requestHeadersJson(requestHeadersJson())
                .requestBodyJson(audit.redactAndTruncate(requestBodyJson));

        long started = System.currentTimeMillis();
        WebClient client = webClientBuilder.build();
        try {
            var response = client.post()
                    .uri(URI.create(creds.getAuthUrl()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toEntity(String.class)
                    .block(Duration.ofMillis(creds.getReadTimeoutMs()));

            long latency = System.currentTimeMillis() - started;
            int status = response != null ? response.getStatusCode().value() : 0;
            String respBody = response != null ? response.getBody() : null;
            HttpHeaders respHeaders = response != null ? response.getHeaders() : null;

            if (status != 200 || respBody == null) {
                String errMsg = "MOI login failed · HTTP " + status;
                audit.record(auditBuilder
                        .httpStatus(status)
                        .responseHeadersJson(audit.headersJson(respHeaders))
                        .responseBodyJson(audit.redactAndTruncate(respBody))
                        .durationMs(latency)
                        .errorMessage(errMsg)
                        .build());
                throw new ApplicationException(ErrorCode.CONNECTOR_ERROR, errMsg);
            }

            String token = extractToken(respBody);
            if (token == null || token.isBlank()) {
                audit.record(auditBuilder
                        .httpStatus(status)
                        .responseHeadersJson(audit.headersJson(respHeaders))
                        .responseBodyJson(audit.redactAndTruncate(respBody))
                        .durationMs(latency)
                        .errorMessage("MOI login response did not contain a token field")
                        .build());
                throw new ApplicationException(ErrorCode.CONNECTOR_ERROR,
                        "MOI login response did not contain a token field");
            }

            Instant expiresAt = deriveExpiry(token, creds.getTokenRefreshSkewSec());

            audit.record(auditBuilder
                    .httpStatus(status)
                    .responseHeadersJson(audit.headersJson(respHeaders))
                    .responseBodyJson(audit.redactAndTruncate(respBody))
                    .durationMs(latency)
                    .tokenSnippet(MoiRedactor.tokenSnippet(token))
                    .build());

            log.info("MOI JWT acquired · expires={} · {} ms", expiresAt, latency);
            return new CachedToken(token, expiresAt);

        } catch (ApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - started;
            audit.record(auditBuilder
                    .durationMs(latency)
                    .errorMessage(ex.getMessage())
                    .build());
            throw new ApplicationException(ErrorCode.CONNECTOR_ERROR,
                    "MOI login call failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Extract the JWT from the auth response body.
     *
     * <p>The MOI staging endpoint returns the raw JWT as {@code text/plain},
     * so we first check whether the trimmed body itself matches the
     * three-segment JWT shape. If not, fall back to scanning JSON for the
     * common token field names — this keeps us compatible with providers
     * that wrap the JWT in {@code {"token": "..."}} envelopes.
     */
    String extractToken(String body) {
        if (body == null) return null;
        String trimmed = body.trim();
        if (trimmed.isEmpty()) return null;
        if (JWT_PATTERN.matcher(trimmed).matches()) return trimmed;
        try {
            JsonNode root = mapper.readTree(trimmed);
            for (String k : TOKEN_KEYS) {
                JsonNode v = root.get(k);
                if (v != null && v.isTextual() && !v.asText().isBlank()) return v.asText();
            }
            JsonNode data = root.get("data");
            if (data != null && data.isObject()) {
                for (String k : TOKEN_KEYS) {
                    JsonNode v = data.get(k);
                    if (v != null && v.isTextual() && !v.asText().isBlank()) return v.asText();
                }
            }
            JsonNode result = root.get("result");
            if (result != null && result.isObject()) {
                for (String k : TOKEN_KEYS) {
                    JsonNode v = result.get(k);
                    if (v != null && v.isTextual() && !v.asText().isBlank()) return v.asText();
                }
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Compute expiry from JWT {@code exp} claim if present; otherwise fall
     * back to {@link #DEFAULT_TTL_SEC} seconds. Always subtracts {@code skewSec}
     * to keep callers safely inside the valid window.
     */
    Instant deriveExpiry(String token, int skewSec) {
        long expEpochSec = parseExpClaim(token);
        Instant exp = expEpochSec > 0
                ? Instant.ofEpochSecond(expEpochSec)
                : Instant.now().plusSeconds(DEFAULT_TTL_SEC);
        return exp.minusSeconds(Math.max(0, skewSec));
    }

    private long parseExpClaim(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return 0;
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = mapper.readTree(new String(payloadBytes, StandardCharsets.UTF_8));
            JsonNode exp = payload.get("exp");
            return exp != null && exp.canConvertToLong() ? exp.asLong() : 0;
        } catch (Exception ex) {
            return 0;
        }
    }

    private String requestHeadersJson() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return audit.headersJson(h);
    }

    private static final String[] TOKEN_KEYS = {
            "token", "accessToken", "access_token", "jwt", "bearer", "Authorization"
    };

    /** Three base64url segments separated by dots — matches a bare JWT response body. */
    private static final Pattern JWT_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");

    private static final long DEFAULT_TTL_SEC = 50 * 60; // 50 minutes

    public record CachedToken(String token, Instant expiresAt) {}

    public record TokenResult(boolean ok, String token, Instant expiresAt, String errorMessage) {}
}
