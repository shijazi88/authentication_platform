package com.middleware.platform.gateway.projection;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Filters a canonical response tree (nested Maps and Lists) so that only fields
 * whitelisted by the tenant's plan are returned.
 *
 * <h3>Rules</h3>
 * <ul>
 *   <li>{@code allowedPaths} contains dot-separated paths, e.g. {@code "verification.status"}.</li>
 *   <li>If a path P is in {@code allowedPaths}, P and its entire subtree are visible.</li>
 *   <li>A leaf at path P is visible iff some allowed E equals or is an ancestor of P.</li>
 *   <li>Lists do not introduce indices into paths — the rule is applied to each element.</li>
 *   <li>Empty {@code allowedPaths} produces an empty result (hit-only mode).</li>
 * </ul>
 */
@Component
public class FieldProjector {

    @SuppressWarnings("unchecked")
    public Map<String, Object> project(Map<String, Object> source, Set<String> allowedPaths) {
        if (source == null || allowedPaths == null || allowedPaths.isEmpty()) {
            return Map.of();
        }
        Object result = projectValue(source, "", allowedPaths);
        if (result instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Map.of();
    }

    private Object projectValue(Object value, String path, Set<String> allowedPaths) {
        if (!path.isEmpty() && isPathOrAncestorAllowed(path, allowedPaths)) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String childPath = path.isEmpty()
                        ? String.valueOf(entry.getKey())
                        : path + "." + entry.getKey();
                Object childResult = projectValue(entry.getValue(), childPath, allowedPaths);
                if (!isEmpty(childResult)) {
                    result.put(String.valueOf(entry.getKey()), childResult);
                }
            }
            return result.isEmpty() ? null : result;
        }
        if (value instanceof Collection<?> coll) {
            List<Object> result = new ArrayList<>();
            for (Object item : coll) {
                Object itemResult = projectValue(item, path, allowedPaths);
                if (!isEmpty(itemResult)) {
                    result.add(itemResult);
                }
            }
            return result.isEmpty() ? null : result;
        }
        // Leaf value — included only if its path was allowed (handled above).
        return null;
    }

    private boolean isPathOrAncestorAllowed(String path, Set<String> allowedPaths) {
        for (String allowed : allowedPaths) {
            if (path.equals(allowed) || path.startsWith(allowed + ".")) {
                return true;
            }
        }
        return false;
    }

    private boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof Map<?, ?> m) return m.isEmpty();
        if (value instanceof Collection<?> c) return c.isEmpty();
        return false;
    }
}
