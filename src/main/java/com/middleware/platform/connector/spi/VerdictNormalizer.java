package com.middleware.platform.connector.spi;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rewrites a provider's {@code verification} block into the ICD shape
 * (§4.3.2 / §5) regardless of which variant the provider or a stub produced:
 *
 * <pre>
 * "verification": {
 *   "verification": "MATCH" | "NO_MATCH" | "NO_VERIFICATION_POSSIBLE" | "EXEMPT",
 *   "biometrics": { "exists": true, "score": 92 }
 * }
 * </pre>
 *
 * Accepted inputs: {@code verification.verification} (real provider) or
 * {@code verification.status} (legacy stub) for the verdict;
 * {@code verification.biometrics.{exists,score}} or
 * {@code verification.biometric.{status,score}} for the biometric result.
 */
public final class VerdictNormalizer {

    public static final String MATCH = "MATCH";
    public static final String NO_MATCH = "NO_MATCH";
    public static final String NO_VERIFICATION_POSSIBLE = "NO_VERIFICATION_POSSIBLE";
    public static final String EXEMPT = "EXEMPT";
    private static final Set<String> KNOWN = Set.of(MATCH, NO_MATCH, NO_VERIFICATION_POSSIBLE, EXEMPT);

    private VerdictNormalizer() {}

    /**
     * Normalises {@code canonical.verification} in place and returns the verdict,
     * or {@code null} when the provider body carries no recognisable verdict
     * (callers fail closed on null). For a fingerprint-exception request the
     * verdict is forced to {@link #EXEMPT}.
     */
    @SuppressWarnings("unchecked")
    public static String normalize(Map<String, Object> canonical, boolean exemptRequest) {
        if (canonical == null) return null;
        Map<String, Object> v = canonical.get("verification") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();

        String verdict = str(first(v.get("verification"), v.get("status"), v.get("result")));
        Map<String, Object> bio = v.get("biometrics") instanceof Map<?, ?> b1 ? (Map<String, Object>) b1
                : v.get("biometric") instanceof Map<?, ?> b2 ? (Map<String, Object>) b2 : Map.of();
        Object score = first(bio.get("score"), v.get("score"));
        Object existsRaw = first(bio.get("exists"), bio.get("status"));
        boolean exists = existsRaw instanceof Boolean b ? b : score != null;

        if (exemptRequest) {
            verdict = EXEMPT;
            exists = false;
            score = null;
        } else if (verdict != null) {
            verdict = verdict.trim().toUpperCase(Locale.ROOT);
            if (!KNOWN.contains(verdict)) return null;
        } else {
            return null;
        }

        Map<String, Object> biometrics = new LinkedHashMap<>();
        biometrics.put("exists", exists);
        if (score != null) biometrics.put("score", score);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("verification", verdict);
        out.put("biometrics", biometrics);
        canonical.put("verification", out);
        return verdict;
    }

    /** Reads the verdict from an already-normalised canonical payload (null if absent). */
    public static String verdictOf(Map<String, Object> canonical) {
        if (canonical == null || !(canonical.get("verification") instanceof Map<?, ?> v)) return null;
        return str(v.get("verification"));
    }

    /**
     * A non-match must never disclose who the record belongs to: drop the
     * {@code person} block (demographics, cards) and keep only the verdict.
     */
    public static void withholdPerson(Map<String, Object> canonical) {
        if (canonical != null) canonical.remove("person");
    }

    private static Object first(Object... candidates) {
        for (Object c : candidates) if (c != null) return c;
        return null;
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }
}
