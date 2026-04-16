package com.middleware.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the application context boots cleanly using the test profile.
 *
 * <p>Requires a MySQL instance reachable at the URL in {@code application-test.yml}
 * (default: {@code jdbc:mysql://localhost:3306/yemen_test}). Override via the
 * {@code TEST_DB_URL}, {@code TEST_DB_USERNAME}, {@code TEST_DB_PASSWORD} env vars.
 *
 * <p>For CI without a local MySQL, switch to Testcontainers
 * (jdbc:tc:mysql:8.0:///yemen_test).
 */
@SpringBootTest
@ActiveProfiles("test")
class PlatformApplicationTests {

    @Test
    void contextLoads() {
        // intentionally empty
    }
}
