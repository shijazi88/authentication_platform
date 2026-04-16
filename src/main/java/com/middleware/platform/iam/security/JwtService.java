package com.middleware.platform.iam.security;

import com.middleware.platform.iam.domain.AdminUser;
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

    public Claims parse(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(props.jwt().issuer())
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }
}
