package com.middleware.platform.wallet.dto;

import jakarta.validation.constraints.Size;

/** Admin decision note when approving/rejecting a top-up request. */
public record TopUpDecisionRequest(
        @Size(max = 512) String note
) {}
