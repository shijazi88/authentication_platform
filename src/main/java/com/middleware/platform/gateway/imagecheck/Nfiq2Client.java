package com.middleware.platform.gateway.imagecheck;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client for the NFIQ 2 quality-score sidecar (see {@code nfiq2/} in the repo).
 * POSTs the raw image bytes and returns the 0–100 score, or the sidecar's
 * reason why the image could not be scored. Never throws: an unreachable
 * sidecar is reported as {@code available=false} so the caller can fail open.
 *
 * <p>Availability transitions are logged at ERROR (down) / INFO (recovered) so
 * the log stream doubles as the alert channel; {@link #health()} backs the
 * status shown in the admin portal.
 */
@Component
@Slf4j
public class Nfiq2Client {

    public record Result(Integer score, String error, boolean available, long ms) {
        public static Result unavailable(String why) { return new Result(null, why, false, 0); }
    }

    /** Snapshot for the admin portal. */
    public record Health(boolean available, String version, Long latencyMs, String error,
                         Instant checkedAt, Instant lastFailureAt) {}

    private final String baseUrl;
    private final int defaultTimeoutMs;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final AtomicReference<Boolean> lastAvailable = new AtomicReference<>(null);
    private volatile Instant lastFailureAt;

    public Nfiq2Client(@Value("${platform.nfiq2.url:http://nfiq2:8090}") String baseUrl,
                       @Value("${platform.nfiq2.timeout-ms:5000}") int timeoutMs,
                       ObjectMapper mapper) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.defaultTimeoutMs = timeoutMs;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    public Result score(byte[] image, ImageFormat format) {
        return score(image, format, defaultTimeoutMs);
    }

    public Result score(byte[] image, ImageFormat format, int timeoutMs) {
        long t0 = System.currentTimeMillis();
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/score"))
                    .timeout(Duration.ofMillis(timeoutMs <= 0 ? defaultTimeoutMs : timeoutMs))
                    .header("Content-Type", format == ImageFormat.WSQ ? "image/x-wsq" : "image/png")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(image))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            long ms = System.currentTimeMillis() - t0;
            if (resp.statusCode() != 200) {
                log.warn("NFIQ2 sidecar answered HTTP {} in {} ms: {}", resp.statusCode(), ms, resp.body());
                return markDown("quality service answered HTTP " + resp.statusCode());
            }
            JsonNode n = mapper.readTree(resp.body());
            Integer score = n.hasNonNull("score") ? n.get("score").asInt() : null;
            String error = n.hasNonNull("error") ? n.get("error").asText() : null;
            markUp();
            return new Result(score, error, true, ms);
        } catch (Exception ex) {
            log.warn("NFIQ2 sidecar unreachable ({}): {}", baseUrl, ex.toString());
            return markDown("quality service unreachable");
        }
    }

    /** Live probe of the sidecar's {@code /health}; used by the admin portal status badge. */
    public Health health() {
        long t0 = System.currentTimeMillis();
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/health"))
                    .timeout(Duration.ofSeconds(3)).GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            long ms = System.currentTimeMillis() - t0;
            if (resp.statusCode() != 200) {
                markDown("HTTP " + resp.statusCode());
                return new Health(false, null, ms, "quality service answered HTTP " + resp.statusCode(),
                        Instant.now(), lastFailureAt);
            }
            JsonNode n = mapper.readTree(resp.body());
            markUp();
            return new Health(true, n.path("nfiq2").asText(null), ms, null, Instant.now(), lastFailureAt);
        } catch (Exception ex) {
            markDown(ex.toString());
            return new Health(false, null, null, "quality service unreachable", Instant.now(), lastFailureAt);
        }
    }

    private Result markDown(String why) {
        lastFailureAt = Instant.now();
        if (!Boolean.FALSE.equals(lastAvailable.getAndSet(false))) {
            log.error("ALERT: NFIQ 2 quality service is DOWN ({}): {}", baseUrl, why);
        }
        return Result.unavailable(why);
    }

    private void markUp() {
        if (Boolean.FALSE.equals(lastAvailable.getAndSet(true))) {
            log.info("NFIQ 2 quality service recovered ({})", baseUrl);
        }
    }
}
