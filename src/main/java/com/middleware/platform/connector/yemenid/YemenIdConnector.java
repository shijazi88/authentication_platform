package com.middleware.platform.connector.yemenid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.connector.spi.ConnectorRequest;
import com.middleware.platform.connector.spi.ConnectorResponse;
import com.middleware.platform.connector.spi.VerificationConnector;
import com.middleware.platform.connector.yemenid.dto.YemenIdVerifyRequest;
import com.middleware.platform.connector.yemenid.dto.YemenIdVerifyResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Concrete connector implementation for Yemen ID v0.1.0 — wraps POST /v1/id/verify.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YemenIdConnector implements VerificationConnector {

    public static final String KEY = "YEMEN_ID";
    public static final String OP_VERIFY = "verify";

    private final YemenIdProperties properties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private WebClient client;

    @PostConstruct
    void init() {
        // Build the WebClient eagerly so the first inbound request doesn't hit a
        // racy lazy-init path. Properties are bound by the time @PostConstruct runs.
        this.client = webClientBuilder.baseUrl(properties.baseUrl()).build();
        log.info("Yemen ID connector ready · base-url={} · timeout={}ms",
                properties.baseUrl(), properties.timeoutMs());
    }

    @Override
    public String key() { return KEY; }

    @Override
    @CircuitBreaker(name = "yemen-id")
    @Retry(name = "yemen-id")
    public ConnectorResponse invoke(ConnectorRequest request) {
        if (!OP_VERIFY.equals(request.operationCode())) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST,
                    "Unsupported Yemen ID operation: " + request.operationCode());
        }

        YemenIdVerifyRequest providerReq = toProviderRequest(request.payload());
        long start = System.currentTimeMillis();
        try {
            YemenIdVerifyResponse providerRes = client
                    .post()
                    .uri("/v1/id/verify")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.bearerToken())
                    .header("X-Request-Id", request.requestId().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(providerReq)
                    .retrieve()
                    .onStatus(s -> s.value() == 401,
                            r -> Mono.error(new ApplicationException(ErrorCode.CONNECTOR_ERROR,
                                    "Yemen ID rejected our credentials (401)")))
                    .onStatus(s -> s.value() == 404,
                            r -> Mono.error(new ApplicationException(ErrorCode.NOT_FOUND,
                                    "National number not found in Yemen ID")))
                    .bodyToMono(YemenIdVerifyResponse.class)
                    .block(Duration.ofMillis(properties.timeoutMs()));

            long latency = System.currentTimeMillis() - start;
            Map<String, Object> canonical = toCanonical(providerRes);
            String providerRequestId = providerRes != null && providerRes.transaction() != null
                    ? providerRes.transaction().id()
                    : null;
            return new ConnectorResponse(200, providerRequestId, canonical, latency);

        } catch (ApplicationException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            log.warn("Yemen ID returned {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApplicationException(ErrorCode.CONNECTOR_ERROR,
                    "Yemen ID error " + ex.getStatusCode().value(), ex);
        } catch (Exception ex) {
            log.error("Yemen ID call failed", ex);
            throw new ApplicationException(ErrorCode.CONNECTOR_ERROR,
                    "Yemen ID call failed: " + ex.getMessage(), ex);
        }
    }

    private YemenIdVerifyRequest toProviderRequest(Map<String, Object> payload) {
        String nationalNumber = (String) payload.get("nationalNumber");
        if (nationalNumber == null || nationalNumber.isBlank()) {
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED, "nationalNumber is required");
        }
        YemenIdVerifyRequest.Biometrics biometrics = null;
        Object bio = payload.get("biometrics");
        if (bio instanceof Map<?, ?> bioMap) {
            Object pos = bioMap.get("fingerPosition");
            String image = (String) bioMap.get("image");
            biometrics = new YemenIdVerifyRequest.Biometrics(
                    pos instanceof Number n ? n.intValue() : null,
                    image
            );
        }
        return new YemenIdVerifyRequest(nationalNumber, biometrics);
    }

    /** Convert the typed provider response into the canonical Map representation. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> toCanonical(YemenIdVerifyResponse providerRes) {
        if (providerRes == null) return new HashMap<>();
        // Round-trip through Jackson to get a stable Map<String,Object> tree.
        return objectMapper.convertValue(providerRes, Map.class);
    }
}
