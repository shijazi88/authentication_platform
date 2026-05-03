package com.middleware.platform.connector.moi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.connector.moi.audit.MoiApiCallLogger;
import com.middleware.platform.connector.moi.audit.MoiRedactor;
import com.middleware.platform.connector.moi.domain.MoiApiCall;
import com.middleware.platform.connector.moi.domain.MoiCredentials;
import com.middleware.platform.connector.moi.domain.MoiCredentialsRepository;
import com.middleware.platform.connector.spi.ConnectorRequest;
import com.middleware.platform.connector.spi.ConnectorResponse;
import com.middleware.platform.connector.spi.VerificationConnector;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Live MOI Yemen ID connector.
 *
 * <ul>
 *   <li>Fetches a JWT from {@link MoiTokenService} (cached, auto-refreshed).</li>
 *   <li>POSTs to the configured verify URL with {@code Authorization: Bearer}.</li>
 *   <li>Logs every call (request+response, redacted) via {@link MoiApiCallLogger}.</li>
 *   <li>On HTTP 401, invalidates the cached token and retries once.</li>
 * </ul>
 *
 * Activated when {@code platform.connectors.yemen-id.mode=moi}. When mode is
 * {@code wiremock} (the default), the existing {@code YemenIdConnector} is used.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "platform.connectors.yemen-id", name = "mode", havingValue = "moi")
public class MoiYemenIdConnector implements VerificationConnector {

    public static final String KEY = "YEMEN_ID";
    public static final String OP_VERIFY = "verify";

    private final MoiCredentialsRepository credentialsRepo;
    private final MoiTokenService tokenService;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper mapper;
    private final MoiApiCallLogger audit;

    @Override
    public String key() { return KEY; }

    @Override
    @CircuitBreaker(name = "moi-yemen-id")
    @Retry(name = "moi-yemen-id")
    public ConnectorResponse invoke(ConnectorRequest request) {
        if (!OP_VERIFY.equals(request.operationCode())) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST,
                    "Unsupported Yemen ID operation: " + request.operationCode());
        }

        Map<String, Object> providerReq = toProviderRequest(request.payload());
        MoiCredentials creds = credentialsRepo.getSingleton();

        // First attempt with cached/fresh token; on 401, re-login once.
        try {
            return doVerify(request, providerReq, creds, tokenService.getToken());
        } catch (ApplicationException ex) {
            if (ex.getErrorCode() == ErrorCode.INVALID_CREDENTIALS) {
                log.info("MOI returned 401 — invalidating cached token and retrying once");
                tokenService.invalidate();
                return doVerify(request, providerReq, creds, tokenService.getToken());
            }
            throw ex;
        }
    }

    private ConnectorResponse doVerify(ConnectorRequest request,
                                       Map<String, Object> providerReq,
                                       MoiCredentials creds,
                                       String token) {
        String url = creds.getVerifyUrl();
        String requestBodyJson = serialize(providerReq);

        HttpHeaders outHeaders = new HttpHeaders();
        outHeaders.setContentType(MediaType.APPLICATION_JSON);
        outHeaders.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        outHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        outHeaders.add("X-Request-Id", request.requestId().toString());

        MoiApiCall.MoiApiCallBuilder auditBuilder = audit.build(MoiApiCall.Kind.VERIFY, url)
                .tenantId(request.tenantId() != null ? request.tenantId().toString() : null)
                .transactionId(request.requestId().toString())
                .tokenSnippet(MoiRedactor.tokenSnippet(token))
                .requestHeadersJson(audit.headersJson(outHeaders))
                .requestBodyJson(audit.redactAndTruncate(requestBodyJson));

        long started = System.currentTimeMillis();
        WebClient client = webClientBuilder.build();

        try {
            var response = client.post()
                    .uri(URI.create(url))
                    .headers(h -> h.addAll(outHeaders))
                    .bodyValue(providerReq)
                    .retrieve()
                    .onStatus(s -> true, r -> reactor.core.publisher.Mono.empty())  // never throw — handle status ourselves
                    .toEntity(String.class)
                    .block(Duration.ofMillis(creds.getReadTimeoutMs()));

            long latency = System.currentTimeMillis() - started;
            int status = response != null ? response.getStatusCode().value() : 0;
            String respBody = response != null ? response.getBody() : null;
            HttpHeaders respHeaders = response != null ? response.getHeaders() : null;
            String providerReqId = respHeaders != null ? respHeaders.getFirst("x-request-id") : null;

            auditBuilder
                    .httpStatus(status)
                    .responseHeadersJson(audit.headersJson(respHeaders))
                    .responseBodyJson(audit.redactAndTruncate(respBody))
                    .durationMs(latency);

            if (status == 401) {
                audit.record(auditBuilder.errorMessage("Unauthorized — token rejected").build());
                throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS,
                        "MOI rejected the bearer token (401)");
            }
            if (status == 403) {
                audit.record(auditBuilder.errorMessage("Forbidden — IP or permission").build());
                throw new ApplicationException(ErrorCode.FORBIDDEN,
                        "MOI denied the request (403)");
            }
            if (status == 404) {
                audit.record(auditBuilder.errorMessage("Not found in Yemen ID").build());
                throw new ApplicationException(ErrorCode.NOT_FOUND,
                        "National number not found in Yemen ID");
            }
            // Other 4xx — typically MOI's input validation (e.g. 400 / X-Error-Code 220
            // "Invalid biometrics", 203 "Bad image format"). Surface MOI's own error
            // code + message so the bank's UI shows the actual cause instead of a
            // generic "upstream is broken". 5xx still maps to CONNECTOR_ERROR.
            if (status >= 400 && status < 500) {
                String moiErrCode = respHeaders != null ? respHeaders.getFirst("X-Error-Code") : null;
                String moiErrMsg  = respHeaders != null ? respHeaders.getFirst("X-Error-Message") : null;
                String upstream = (moiErrCode != null ? "MOI " + moiErrCode : "MOI HTTP " + status)
                        + (moiErrMsg != null ? " · " + moiErrMsg : "");
                audit.record(auditBuilder.errorMessage(upstream).build());
                throw new ApplicationException(ErrorCode.VALIDATION_FAILED, upstream);
            }
            if (status < 200 || status >= 300 || respBody == null) {
                audit.record(auditBuilder.errorMessage("MOI HTTP " + status).build());
                throw new ApplicationException(ErrorCode.CONNECTOR_ERROR,
                        "MOI verify failed · HTTP " + status);
            }

            Map<String, Object> canonical = parseBody(respBody);
            audit.record(auditBuilder.build());

            return new ConnectorResponse(status, providerReqId, canonical, latency);

        } catch (ApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - started;
            audit.record(auditBuilder.durationMs(latency).errorMessage(ex.getMessage()).build());
            log.error("MOI verify call failed", ex);
            throw new ApplicationException(ErrorCode.CONNECTOR_ERROR,
                    "MOI verify call failed: " + ex.getMessage(), ex);
        }
    }

    /** Build the request body MOI expects. Accepts our internal canonical fields. */
    private Map<String, Object> toProviderRequest(Map<String, Object> payload) {
        Object nn = payload.get("nationalNumber");
        if (nn == null || (nn instanceof String s && s.isBlank())) {
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED, "nationalNumber is required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nationalNumber", normalizeNationalNumber(nn.toString()));
        Object bio = payload.get("biometrics");
        if (bio instanceof Map<?, ?> bioMap) {
            Map<String, Object> b = new LinkedHashMap<>();
            Object pos = bioMap.get("fingerPosition");
            Object img = bioMap.get("image");
            if (pos != null) b.put("fingerPosition", pos);
            if (img != null) b.put("image", img);
            if (!b.isEmpty()) body.put("biometrics", b);
        }
        return body;
    }

    /**
     * MOI's verify endpoint requires the national number in dashed
     * {@code NNNN-NNNN-NNNN} form. Banks may send either {@code 213274040424}
     * or {@code 2132-7404-0424}; if it's exactly 12 digits, insert dashes.
     * Anything else (already-dashed, scrambled, longer) is passed through so
     * MOI's own validation surfaces the real error.
     */
    static String normalizeNationalNumber(String raw) {
        String trimmed = raw.trim();
        if (trimmed.length() == 12 && trimmed.chars().allMatch(Character::isDigit)) {
            return trimmed.substring(0, 4) + "-"
                    + trimmed.substring(4, 8) + "-"
                    + trimmed.substring(8, 12);
        }
        return trimmed;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseBody(String json) {
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception ex) {
            throw new ApplicationException(ErrorCode.CONNECTOR_ERROR,
                    "MOI response was not JSON: " + ex.getMessage(), ex);
        }
    }

    private String serialize(Object o) {
        try { return mapper.writeValueAsString(o); }
        catch (Exception ex) {
            throw new ApplicationException(ErrorCode.INTERNAL_ERROR, "Serialize failed", ex);
        }
    }
}
