package com.middleware.platform.iam.dto;

import com.middleware.platform.iam.domain.IpPolicy;
import com.middleware.platform.iam.domain.Tenant;
import com.middleware.platform.iam.security.IpAllowlist;

import java.util.List;

/** Tenant-facing view of the source-IP policy applied to its API calls. */
public record IpPolicyResponse(IpPolicy mode, List<String> allowlist) {
    public static IpPolicyResponse from(Tenant t) {
        return new IpPolicyResponse(
                t.getIpPolicy() == null ? IpPolicy.ALL : t.getIpPolicy(),
                IpAllowlist.parse(t.getIpAllowlist()));
    }
}
