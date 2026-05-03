package com.middleware.platform.connector.moi.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Removes secrets from headers/bodies before audit persistence.
 * <ul>
 *   <li>{@code Authorization} header → keeps scheme + first 12 and last 6 chars of token.</li>
 *   <li>JSON body {@code password} fields → masked as {@code ***}.</li>
 *   <li>JSON body {@code biometrics.image} / {@code image} fields → replaced with
 *       summary placeholder ({@code <base64 · N chars · prefix…>}).</li>
 * </ul>
 */
@Slf4j
public final class MoiRedactor {

    /** Keep a small, non-sensitive snippet of the JWT for call correlation. */
    public static String tokenSnippet(String token) {
        if (token == null) return null;
        int n = token.length();
        if (n <= 18) return token;
        return token.substring(0, 12) + "…" + token.substring(n - 6);
    }

    public static String maskAuthorization(String value) {
        if (value == null) return null;
        // "Bearer eyJhbG...xyz" → "Bearer eyJhbGc...xyz"
        String[] parts = value.split(" ", 2);
        if (parts.length != 2) return "***";
        return parts[0] + " " + tokenSnippet(parts[1]);
    }

    /** Redacts a JSON string body; returns a new JSON string. On parse failure returns input unchanged. */
    public static String redactBody(ObjectMapper mapper, String body) {
        if (body == null || body.isEmpty()) return body;
        try {
            JsonNode root = mapper.readTree(body);
            redactInPlace(root);
            return mapper.writeValueAsString(root);
        } catch (Exception ex) {
            // Non-JSON body (HTML error page, plain text). Fall back to regex
            // for "password" keys; leave everything else alone.
            return REGEX_PW.matcher(body).replaceAll("$1***$3");
        }
    }

    private static void redactInPlace(JsonNode node) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String k = e.getKey();
                JsonNode v = e.getValue();
                if ("password".equalsIgnoreCase(k)) {
                    obj.put(k, "***");
                } else if ("image".equalsIgnoreCase(k) && v.isTextual()) {
                    String img = v.asText();
                    int len = img.length();
                    String prefix = img.length() > 24 ? img.substring(0, 24) : img;
                    obj.put(k, "<base64 · " + len + " chars · " + prefix + "…>");
                } else {
                    redactInPlace(v);
                }
            }
        } else if (node.isArray()) {
            node.forEach(MoiRedactor::redactInPlace);
        }
    }

    /** Fallback for non-JSON bodies. Matches `"password"`:`"..."` with optional whitespace. */
    private static final Pattern REGEX_PW =
            Pattern.compile("(\"password\"\\s*:\\s*\")([^\"]*)(\")", Pattern.CASE_INSENSITIVE);

    /** Truncates any string over {@link #MAX_BODY_BYTES}, appending a marker. */
    public static String truncate(String s) {
        if (s == null) return null;
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length <= MAX_BODY_BYTES) return s;
        // Truncate on a UTF-8 safe boundary: go back up to 4 bytes.
        int cut = MAX_BODY_BYTES;
        while (cut > 0 && (bytes[cut] & 0xC0) == 0x80) cut--;
        String head = new String(bytes, 0, cut, java.nio.charset.StandardCharsets.UTF_8);
        return head + "\n…[truncated at " + MAX_BODY_BYTES + " bytes]";
    }

    public static final int MAX_BODY_BYTES = 64 * 1024;

    private MoiRedactor() {}
}
