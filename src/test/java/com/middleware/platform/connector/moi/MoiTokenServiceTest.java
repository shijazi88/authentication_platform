package com.middleware.platform.connector.moi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the pure helpers on {@link MoiTokenService} that don't need Spring.
 * Placed in the same package to exercise package-private helpers without
 * widening their visibility. Full HTTP round-trips are exercised by
 * integration tests.
 */
class MoiTokenServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final MoiTokenService svc = new MoiTokenService(null, null, mapper, null);

    @Test
    @DisplayName("extractToken finds 'token' at the root")
    void extract_topLevelToken() {
        String body = "{\"token\":\"abc.def.ghi\"}";
        assertThat(svc.extractToken(body)).isEqualTo("abc.def.ghi");
    }

    @Test
    @DisplayName("extractToken finds nested under 'data'")
    void extract_nestedUnderData() {
        String body = "{\"status\":\"ok\",\"data\":{\"accessToken\":\"nested.jwt.value\"}}";
        assertThat(svc.extractToken(body)).isEqualTo("nested.jwt.value");
    }

    @Test
    @DisplayName("extractToken returns null when no known key matches")
    void extract_missing() {
        String body = "{\"message\":\"welcome\"}";
        assertThat(svc.extractToken(body)).isNull();
    }

    @Test
    @DisplayName("extractToken tolerates malformed JSON")
    void extract_malformed() {
        assertThat(svc.extractToken("not json at all")).isNull();
        assertThat(svc.extractToken("")).isNull();
        assertThat(svc.extractToken(null)).isNull();
    }

    @Test
    @DisplayName("extractToken returns a bare JWT response body as-is")
    void extract_bareJwt() {
        // MOI staging returns the raw JWT with Content-Type: text/plain — no JSON envelope.
        String token = buildJwt("{\"sub\":\"NATVERIFIAPI\"}");
        assertThat(svc.extractToken(token)).isEqualTo(token);
    }

    @Test
    @DisplayName("extractToken trims whitespace around a bare JWT")
    void extract_bareJwt_trimsWhitespace() {
        String token = buildJwt("{\"sub\":\"NATVERIFIAPI\"}");
        assertThat(svc.extractToken("  " + token + "\n")).isEqualTo(token);
    }

    @Test
    @DisplayName("deriveExpiry reads JWT exp claim")
    void deriveExpiry_fromClaim() {
        long exp = Instant.now().plusSeconds(3600).getEpochSecond();
        String token = buildJwt("{\"exp\":" + exp + "}");
        Instant out = svc.deriveExpiry(token, 60);
        assertThat(out.getEpochSecond()).isEqualTo(exp - 60);
    }

    @Test
    @DisplayName("deriveExpiry falls back to default TTL when claim missing")
    void deriveExpiry_fallback() {
        String token = buildJwt("{\"sub\":\"NATVERIFIAPI\"}");
        Instant out = svc.deriveExpiry(token, 60);
        long minutes = (out.getEpochSecond() - Instant.now().getEpochSecond()) / 60;
        assertThat(minutes).isBetween(48L, 50L);
    }

    @Test
    @DisplayName("deriveExpiry handles malformed token gracefully")
    void deriveExpiry_malformed() {
        Instant out = svc.deriveExpiry("not-a-jwt", 0);
        assertThat(out).isAfter(Instant.now());
    }

    /** Build a minimally valid 3-part JWT (header.payload.signature) for parsing only. */
    private static String buildJwt(String payloadJson) {
        String header = base64Url("{\"alg\":\"none\"}");
        String payload = base64Url(payloadJson);
        return header + "." + payload + ".sig";
    }

    private static String base64Url(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes());
    }
}
