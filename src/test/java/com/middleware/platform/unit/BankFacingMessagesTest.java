package com.middleware.platform.unit;

import com.middleware.platform.common.error.BankFacingMessages;
import com.middleware.platform.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BankFacingMessagesTest {

    @Test
    void connectorFailuresBecomeServiceMessagesWithoutProviderNames() {
        assertThat(BankFacingMessages.forBank(ErrorCode.CONNECTOR_ERROR, "MOI verify failed · HTTP 500"))
                .doesNotContain("MOI").doesNotContain("500").contains("verification service").contains("request ID");
        assertThat(BankFacingMessages.forBank(ErrorCode.CONNECTOR_UNAVAILABLE,
                "Circuit breaker open for connector moi-yemen-id — CircuitBreaker 'moi-yemen-id' is OPEN and does not permit further calls"))
                .doesNotContain("Circuit").doesNotContain("MOI").contains("temporarily unavailable");
        assertThat(BankFacingMessages.forBank(ErrorCode.CONNECTOR_TIMEOUT, "MOI verify call failed: timeout"))
                .contains("did not respond in time");
        assertThat(BankFacingMessages.forBank(ErrorCode.INTERNAL_ERROR, "NullPointerException at ..."))
                .doesNotContain("Exception").contains("unexpected error");
    }

    @Test
    void providerValidationKeepsTheCodeButDropsTheName() {
        assertThat(BankFacingMessages.forBank(ErrorCode.VALIDATION_FAILED, "MOI 220 · Invalid biometrics"))
                .isEqualTo("The verification service rejected the request (code 220): Invalid biometrics");
        assertThat(BankFacingMessages.forBank(ErrorCode.VALIDATION_FAILED, "MOI HTTP 400"))
                .isEqualTo("The verification service rejected the request (code 400)");
        assertThat(BankFacingMessages.forBank(ErrorCode.INVALID_CREDENTIALS, "MOI rejected the bearer token (401)"))
                .doesNotContain("MOI").doesNotContain("token").contains("contact support");
    }

    @Test
    void bankOwnErrorsAreUntouched() {
        assertThat(BankFacingMessages.forBank(ErrorCode.VALIDATION_FAILED, "Fingerprint image rejected: PNG must be 8-bit greyscale"))
                .isEqualTo("Fingerprint image rejected: PNG must be 8-bit greyscale");
        assertThat(BankFacingMessages.forBank(ErrorCode.NOT_FOUND, "National number not found in Yemen ID"))
                .isEqualTo("National number not found in Yemen ID");
        assertThat(BankFacingMessages.forBank(ErrorCode.BIOMETRIC_NO_MATCH, "Fingerprint did not match the national ID"))
                .isEqualTo("Fingerprint did not match the national ID");
        assertThat(BankFacingMessages.forBank(ErrorCode.INSUFFICIENT_FUNDS, null)).isEqualTo("Insufficient wallet balance");
    }
}
