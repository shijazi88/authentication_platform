package com.middleware.platform.unit;

import com.middleware.platform.iam.domain.AdminRole;
import com.middleware.platform.iam.domain.AdminUser;
import com.middleware.platform.iam.security.JwtService;
import com.middleware.platform.iam.security.SecurityProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            new SecurityProperties(
                    new SecurityProperties.Jwt(
                            "test-secret-test-secret-test-secret-test-secret-256b",
                            "test-issuer",
                            60),
                    new SecurityProperties.BootstrapAdmin("", "", false)));

    @Test
    @DisplayName("issueAccessToken → parsable JWT with correct claims")
    void issueAndParse() {
        AdminUser user = AdminUser.builder()
                .id(UUID.randomUUID())
                .email("admin@test.local")
                .role(AdminRole.SUPER_ADMIN)
                .build();

        String token = jwtService.issueAccessToken(user);
        assertThat(token).isNotBlank();

        Claims claims = jwtService.parse(token);
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo("admin@test.local");
        assertThat(claims.get("role", String.class)).isEqualTo("SUPER_ADMIN");
        assertThat(claims.getIssuer()).isEqualTo("test-issuer");
    }

    @Test
    @DisplayName("parse → throws on tampered token")
    void parse_tampered() {
        AdminUser user = AdminUser.builder()
                .id(UUID.randomUUID())
                .email("admin@test.local")
                .role(AdminRole.SUPER_ADMIN)
                .build();

        String token = jwtService.issueAccessToken(user);
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        assertThatThrownBy(() -> jwtService.parse(tampered))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("accessTokenTtlSeconds → matches config")
    void ttl() {
        assertThat(jwtService.accessTokenTtlSeconds()).isEqualTo(3600L);
    }
}
