package com.middleware.platform.iam.security;

import com.middleware.platform.iam.domain.AdminUser;
import com.middleware.platform.iam.domain.TenantUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final SecurityProperties props;
    private final SecretKey signingKey;

    public JwtService(SecurityProperties props) {
        this.props = props;
        byte[] keyBytes = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "platform.security.jwt.secret must be at least 32 bytes (256 bits)");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String issueAccessToken(AdminUser user) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.jwt().accessTokenTtlMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .issuer(props.jwt().issuer())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return props.jwt().accessTokenTtlMinutes() * 60L;
    }

    /** Role claim carried by tenant-portal tokens (→ ROLE_TENANT_USER). */
    public static final String TENANT_USER_ROLE = "TENANT_USER";

    /**
     * Access token for a tenant-portal user. Carries the tenant id so requests
     * can be scoped to that tenant, and a TENANT_USER role distinct from admins.
     */
    public String issueTenantToken(TenantUser user) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.jwt().accessTokenTtlMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .issuer(props.jwt().issuer())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", TENANT_USER_ROLE)
                .claim("tenantId", user.getTenantId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    /** Scope claim marking a short-lived step-up unlock token. */
    public static final String UNLOCK_SCOPE = "txn-unlock";
    private static final long UNLOCK_TTL_MINUTES = 15;

    /**
     * Issues a short-lived token proving the user passed a PIN step-up. Subject
     * is the user's email so it can't be replayed for another account.
     */
    public String issueUnlockToken(String email) {
        Instant now = Instant.now();
        Instant exp = now.plus(UNLOCK_TTL_MINUTES, ChronoUnit.MINUTES);
        return Jwts.builder()
                .issuer(props.jwt().issuer())
                .subject(email)
                .claim("scope", UNLOCK_SCOPE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    public long unlockTtlSeconds() {
        return UNLOCK_TTL_MINUTES * 60L;
    }

    /** True if {@code token} is a valid, unexpired unlock token for {@code email}. */
    public boolean isValidUnlock(String token, String email) {
        if (token == null || token.isBlank() || email == null) return false;
        try {
            Claims c = parse(token);
            return UNLOCK_SCOPE.equals(c.get("scope", String.class))
                    && email.equalsIgnoreCase(c.getSubject());
        } catch (Exception ex) {
            return false;
        }
    }

    public Claims parse(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(props.jwt().issuer())
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }
}
