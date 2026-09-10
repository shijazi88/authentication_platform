package com.middleware.platform.iam.security;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Parsing, validation and matching of IP allowlists. An allowlist is stored as
 * a comma-separated string of IPv4 addresses ({@code 203.0.113.4}) and/or CIDR
 * ranges ({@code 203.0.113.0/24}). IPv6 literals are accepted and compared
 * verbatim (no prefix matching).
 */
public final class IpAllowlist {

    private static final Pattern IPV4 =
            Pattern.compile("^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:]+(%[\\w]+)?$");

    private IpAllowlist() {}

    /** Splits a stored comma-separated allowlist into trimmed, non-empty entries. */
    public static List<String> parse(String stored) {
        List<String> out = new ArrayList<>();
        if (stored == null || stored.isBlank()) return out;
        for (String e : stored.split(",")) {
            String s = e.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    /**
     * Normalises user input (trim, de-duplicate, drop blanks) and validates
     * every entry. Returns the entries ready to be joined for storage.
     *
     * @throws IllegalArgumentException naming the first invalid entry
     */
    public static List<String> normalise(List<String> entries) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (entries == null) return new ArrayList<>();
        for (String raw : entries) {
            if (raw == null) continue;
            // Be forgiving about pasted lists: one field may hold several entries.
            for (String piece : raw.split("[,\\s]+")) {
                String s = piece.trim();
                if (s.isEmpty()) continue;
                validationError(s).ifPresent(msg -> { throw new IllegalArgumentException(msg); });
                out.add(s);
            }
        }
        return new ArrayList<>(out);
    }

    public static String join(List<String> entries) {
        return entries == null || entries.isEmpty() ? null : String.join(",", entries);
    }

    /** Empty when {@code entry} is a valid IPv4, IPv4/CIDR or IPv6 literal. */
    public static Optional<String> validationError(String entry) {
        if (entry == null || entry.isBlank()) return Optional.of("Empty IP entry");
        int slash = entry.indexOf('/');
        if (slash >= 0) {
            String ip = entry.substring(0, slash);
            String prefix = entry.substring(slash + 1);
            if (!IPV4.matcher(ip).matches()) {
                return Optional.of("Invalid CIDR '" + entry + "': not an IPv4 network");
            }
            try {
                int p = Integer.parseInt(prefix);
                if (p < 0 || p > 32) return Optional.of("Invalid CIDR '" + entry + "': prefix must be 0–32");
            } catch (NumberFormatException ex) {
                return Optional.of("Invalid CIDR '" + entry + "': bad prefix length");
            }
            return Optional.empty();
        }
        if (IPV4.matcher(entry).matches()) return Optional.empty();
        if (entry.contains(":") && IPV6.matcher(entry).matches()) return Optional.empty();
        return Optional.of("Invalid IP address '" + entry + "'");
    }

    /** True when {@code callerIp} matches any entry of the stored allowlist. */
    public static boolean matches(String callerIp, String storedAllowlist) {
        return matches(callerIp, parse(storedAllowlist));
    }

    public static boolean matches(String callerIp, List<String> entries) {
        if (callerIp == null || entries == null) return false;
        for (String entry : entries) {
            if (entry.contains("/")) {
                if (matchesCidr(callerIp, entry)) return true;
            } else if (entry.equals(callerIp)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            int prefixLen = Integer.parseInt(parts[1]);
            long cidrAddr = ipToLong(parts[0]);
            long ipAddr = ipToLong(ip);
            if (cidrAddr < 0 || ipAddr < 0) return false;
            long mask = prefixLen == 0 ? 0L : (-1L << (32 - prefixLen)) & 0xFFFFFFFFL;
            return (cidrAddr & mask) == (ipAddr & mask);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        if (octets.length != 4) return -1;
        long v = 0;
        for (String o : octets) {
            int n = Integer.parseInt(o);
            if (n < 0 || n > 255) return -1;
            v = (v << 8) | n;
        }
        return v;
    }
}
