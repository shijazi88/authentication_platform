package com.middleware.platform.wallet.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TopUpRequest(
        @Positive long amountMinor,
        @Size(max = 512) String note
) {}
