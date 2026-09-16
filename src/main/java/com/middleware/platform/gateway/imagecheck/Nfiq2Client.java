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

/**
 * Client for the NFIQ 2 quality-score sidecar (see {@code nfiq2/} in the repo).
 * POSTs the raw image bytes and returns the 0–100 score, or the sidecar's
 * reason why the image could not be scored. Never throws: an unreachable
 * sidecar is reported as {@code available=false} so the caller can fail open.
 */
@Component
@Slf4j
public class Nfiq2Client {

    public record Result(Integer score, String error, boolean available, long ms) {
        public static Result unavailable(String why) { return new Result(null, why, false, 0); }
    }

    private final String baseUrl;
    private final int timeoutMs;
    private final ObjectMapper mapper;
    private final HttpClient http;

    public Nfiq2Client(@Value("${platform.nfiq2.url:http://nfiq2:8090}") String baseUrl,
                       @Value("${platform.nfiq2.timeout-ms:5000}") int timeoutMs,
                       ObjectMapper mapper) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.timeoutMs = timeoutMs;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    public Result score(byte[] image, ImageFormat format) {
        long t0 = System.currentTimeMillis();
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/score"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", format == ImageFormat.WSQ ? "image/x-wsq" : "image/png")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(image))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            long ms = System.currentTimeMillis() - t0;
            if (resp.statusCode() != 200) {
                log.warn("NFIQ2 sidecar answered HTTP {} in {} ms: {}", resp.statusCode(), ms, resp.body());
                return Result.unavailable("quality service answered HTTP " + resp.statusCode());
            }
            JsonNode n = mapper.readTree(resp.body());
            Integer score = n.hasNonNull("score") ? n.get("score").asInt() : null;
            String error = n.hasNonNull("error") ? n.get("error").asText() : null;
            return new Result(score, error, true, ms);
        } catch (Exception ex) {
            log.warn("NFIQ2 sidecar unreachable ({}): {}", baseUrl, ex.toString());
            return Result.unavailable("quality service unreachable");
        }
    }
}
