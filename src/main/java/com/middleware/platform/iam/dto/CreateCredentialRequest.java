package com.middleware.platform.iam.dto;

import jakarta.validation.constraints.Size;

public record CreateCredentialRequest(
        @Size(max = 128) String label,
        @Size(max = 1024) String ipAllowlist
) {}
