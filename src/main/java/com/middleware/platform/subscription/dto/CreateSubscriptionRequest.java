package com.middleware.platform.subscription.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateSubscriptionRequest(
        @NotNull UUID tenantId,
        @NotNull UUID planId,
        @NotNull LocalDate startDate,
        LocalDate endDate
) {}
