package com.middleware.platform.unit;

import com.middleware.platform.connector.spi.VerdictNormalizer;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VerdictNormalizerTest {

    private static Map<String, Object> body(Map<String, Object> verification) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("transaction", Map.of("id", "t1"));
        if (verification != null) m.put("verification", new LinkedHashMap<>(verification));
        m.put("person", new LinkedHashMap<>(Map.of("nationalNumber", "0101", "demographics", Map.of("names", Map.of()))));
        return m;
    }

    @Test
    void realProviderShapeIsKeptAndTyped() {
        Map<String, Object> c = body(Map.of("verification", "MATCH", "biometrics", Map.of("exists", true, "score", 92)));
        assertThat(VerdictNormalizer.normalize(c, false)).isEqualTo("MATCH");
        assertThat(c.get("verification")).isEqualTo(Map.of("verification", "MATCH", "biometrics", Map.of("exists", true, "score", 92)));
        assertThat(c).containsKey("person");
    }

    @Test
    void legacyStubShapeIsRewrittenToIcdShape() {
        Map<String, Object> c = body(Map.of("status", "match", "biometric", Map.of("status", true, "score", 88)));
        assertThat(VerdictNormalizer.normalize(c, false)).isEqualTo("MATCH");
        assertThat(c.get("verification")).isEqualTo(Map.of("verification", "MATCH", "biometrics", Map.of("exists", true, "score", 88)));
        assertThat(VerdictNormalizer.verdictOf(c)).isEqualTo("MATCH");
    }

    @Test
    void noMatchIsAVerdict_andPersonIsWithheld() {
        Map<String, Object> c = body(Map.of("verification", "NO_MATCH", "biometrics", Map.of("exists", true, "score", 12)));
        assertThat(VerdictNormalizer.normalize(c, false)).isEqualTo("NO_MATCH");
        VerdictNormalizer.withholdPerson(c);
        assertThat(c).doesNotContainKey("person").containsKey("transaction");
    }

    @Test
    void exemptRequestForcesExemptVerdictWithoutBiometrics() {
        Map<String, Object> c = body(Map.of("verification", "NO_VERIFICATION_POSSIBLE"));
        assertThat(VerdictNormalizer.normalize(c, true)).isEqualTo("EXEMPT");
        assertThat(c.get("verification")).isEqualTo(Map.of("verification", "EXEMPT", "biometrics", Map.of("exists", false)));
        Map<String, Object> none = body(null);
        assertThat(VerdictNormalizer.normalize(none, true)).isEqualTo("EXEMPT");
    }

    @Test
    void missingOrUnknownVerdictIsNull_soCallersFailClosed() {
        assertThat(VerdictNormalizer.normalize(body(null), false)).isNull();
        assertThat(VerdictNormalizer.normalize(body(Map.of("verification", "MAYBE")), false)).isNull();
        assertThat(VerdictNormalizer.normalize(null, false)).isNull();
    }
}
