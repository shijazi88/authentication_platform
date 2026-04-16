package com.middleware.platform.connector.yemenid.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record YemenIdVerifyRequest(
        String nationalNumber,
        Biometrics biometrics
) {
    public record Biometrics(Integer fingerPosition, String image) {}
}
