package com.middleware.platform.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.platform.connector.moi.audit.MoiRedactor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MoiRedactorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("tokenSnippet keeps a short prefix and suffix for long JWTs")
    void tokenSnippet_longToken() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.abcdefghijklmnopqrstuvwxyz.signaturepart";
        String snippet = MoiRedactor.tokenSnippet(jwt);
        assertThat(snippet).startsWith("eyJhbGciOiJI");
        assertThat(snippet).endsWith("repart");
        assertThat(snippet).contains("…");
        assertThat(snippet.length()).isLessThan(jwt.length());
    }

    @Test
    @DisplayName("tokenSnippet returns short tokens as-is (no leak risk)")
    void tokenSnippet_shortToken() {
        assertThat(MoiRedactor.tokenSnippet("short")).isEqualTo("short");
        assertThat(MoiRedactor.tokenSnippet(null)).isNull();
    }

    @Test
    @DisplayName("maskAuthorization preserves Bearer scheme + masks the token")
    void maskAuthorization() {
        String masked = MoiRedactor.maskAuthorization("Bearer eyJhbGciOiJIUzI1NiJ9.something.signed");
        assertThat(masked).startsWith("Bearer ");
        assertThat(masked).doesNotContain("something");
    }

    @Test
    @DisplayName("redactBody masks password field in JSON")
    void redactBody_password() {
        String body = "{\"username\":\"NATVERIFIAPI\",\"password\":\"T9KSa-e8876H!\",\"domain\":\"UM_MASTER\"}";
        String out = MoiRedactor.redactBody(mapper, body);
        assertThat(out).contains("\"password\":\"***\"");
        assertThat(out).doesNotContain("T9KSa-e8876H");
        assertThat(out).contains("NATVERIFIAPI");
        assertThat(out).contains("UM_MASTER");
    }

    @Test
    @DisplayName("redactBody replaces base64 image with summary")
    void redactBody_imageSummary() {
        String body = "{\"nationalNumber\":\"123456789012\",\"biometrics\":{\"fingerPosition\":2,\"image\":\"/6D/qAB6TklTVF9DT00gOQpQSVhfV0lEVEggMzU3Cg==\"}}";
        String out = MoiRedactor.redactBody(mapper, body);
        assertThat(out).contains("<base64 · ");
        assertThat(out).doesNotContain("qAB6TklTVF9DT00gOQpQSVhfV0lEVEggMzU3Cg");
        assertThat(out).contains("123456789012");
    }

    @Test
    @DisplayName("redactBody falls back to regex for non-JSON bodies with password")
    void redactBody_nonJsonWithPassword() {
        String body = "something=went_wrong, \"password\":\"leakme\", trace=...";
        String out = MoiRedactor.redactBody(mapper, body);
        assertThat(out).contains("\"password\":\"***\"");
        assertThat(out).doesNotContain("leakme");
    }

    @Test
    @DisplayName("truncate respects byte limit and appends marker")
    void truncate_longBody() {
        String big = "x".repeat(MoiRedactor.MAX_BODY_BYTES + 100);
        String out = MoiRedactor.truncate(big);
        assertThat(out).endsWith("bytes]");
        assertThat(out.getBytes().length).isLessThan(big.getBytes().length);
    }

    @Test
    @DisplayName("truncate leaves short bodies unchanged")
    void truncate_short() {
        String s = "{\"ok\":true}";
        assertThat(MoiRedactor.truncate(s)).isEqualTo(s);
    }
}
