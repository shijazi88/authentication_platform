package com.middleware.platform.connector.yemenid.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Wire-level representation of the Yemen ID /v1/id/verify response.
 * Loose typing on nested objects keeps us tolerant to future provider additions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record YemenIdVerifyResponse(
        Transaction transaction,
        Verification verification,
        Person person
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transaction(String timestamp, String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Verification(String status, BiometricResult biometric) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BiometricResult(Boolean status, Number score) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Person(
            String nationalNumber,
            List<Map<String, Object>> cards,
            Map<String, Object> demographics
    ) {}
}
