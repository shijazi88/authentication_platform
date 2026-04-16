package com.middleware.platform.integration;

import com.middleware.platform.iam.domain.TenantStatus;
import com.middleware.platform.iam.dto.CreateCredentialRequest;
import com.middleware.platform.iam.dto.CreateTenantRequest;
import com.middleware.platform.iam.dto.CredentialResponse;
import com.middleware.platform.iam.dto.TenantResponse;
import com.middleware.platform.iam.service.TenantService;
import com.middleware.platform.subscription.dto.CreateSubscriptionRequest;
import com.middleware.platform.subscription.service.SubscriptionService;
import java.util.UUID;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration test that boots the full Spring context against a Testcontainers
 * MySQL and exercises the verify-identity happy path + entitlement denied path.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfig.class)
@Testcontainers
class VerifyIdentityIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TenantService tenantService;

    @Autowired
    SubscriptionService subscriptionService;

    private String clientId;
    private String clientSecret;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        // Create tenant + activate + subscribe to PREMIUM + issue credential
        TenantResponse tenant = tenantService.create(
                new CreateTenantRequest("TEST_BANK_" + System.nanoTime(), "Test Bank Ltd", null));
        tenantService.setStatus(tenant.id(), TenantStatus.ACTIVE);

        subscriptionService.create(new CreateSubscriptionRequest(
                tenant.id(),
                UUID.fromString("40000000-0000-0000-0000-000000000002"), // PREMIUM plan (from V6 seed)
                LocalDate.now(),
                null));

        CredentialResponse cred = tenantService.issueCredential(
                tenant.id(), new CreateCredentialRequest(null, null));
        clientId = cred.clientId();
        clientSecret = cred.clientSecret();
    }

    @Test
    @DisplayName("POST /api/v1/verify/identity → 200 with transaction + result")
    void verifyIdentity_success() {
        given()
                .auth().preemptive().basic(clientId, clientSecret)
                .contentType(ContentType.JSON)
                .body("""
                        { "nationalNumber": "B123TEST" }
                        """)
        .when()
                .post("/api/v1/verify/identity")
        .then()
                .statusCode(200)
                .header("X-Request-Id", notNullValue())
                .body("transaction.id", notNullValue())
                .body("transaction.status", equalTo("OK"))
                .body("result", notNullValue())
                .body("result.verification.status", notNullValue());
    }

    @Test
    @DisplayName("POST /api/v1/verify/identity without credentials → 401")
    void verifyIdentity_noCredentials_401() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "nationalNumber": "B123TEST" }
                        """)
        .when()
                .post("/api/v1/verify/identity")
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("POST /api/v1/verify/identity with wrong secret → 401")
    void verifyIdentity_wrongSecret_401() {
        given()
                .auth().preemptive().basic(clientId, "wrong-secret")
                .contentType(ContentType.JSON)
                .body("""
                        { "nationalNumber": "B123TEST" }
                        """)
        .when()
                .post("/api/v1/verify/identity")
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("POST /api/v1/verify/identity with empty body → 400")
    void verifyIdentity_emptyBody_400() {
        given()
                .auth().preemptive().basic(clientId, clientSecret)
                .contentType(ContentType.JSON)
                .body("""
                        { "nationalNumber": "" }
                        """)
        .when()
                .post("/api/v1/verify/identity")
        .then()
                .statusCode(400);
    }
}
