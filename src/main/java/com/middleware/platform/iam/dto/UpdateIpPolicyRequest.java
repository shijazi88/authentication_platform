package com.middleware.platform.iam.dto;

import com.middleware.platform.iam.domain.IpPolicy;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Admin update of a tenant's source-IP policy. {@code allowlist} entries are
 * IPv4 addresses or CIDR ranges; required (non-empty) when mode is RESTRICTED
 * and ignored when mode is ALL.
 */
public record UpdateIpPolicyRequest(
        @NotNull IpPolicy mode,
        @Size(max = 64) List<@Size(max = 64) String> allowlist
) {}
