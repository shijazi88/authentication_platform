package com.middleware.platform.iam.dto;

import com.middleware.platform.iam.domain.IpPolicy;
import com.middleware.platform.iam.domain.Tenant;
import com.middleware.platform.iam.security.IpAllowlist;
import com.middleware.platform.iam.domain.TenantStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String code,
        String legalName,
        String contactEmail,
        TenantStatus status,
        boolean requireEncryptedPii,
        IpPolicy ipPolicy,
        List<String> ipAllowlist,
        Instant createdAt
) {
    public static TenantResponse from(Tenant t) {
        return new TenantResponse(
                t.getId(),
                t.getCode(),
                t.getLegalName(),
                t.getContactEmail(),
                t.getStatus(),
                t.isRequireEncryptedPii(),
                t.getIpPolicy() == null ? IpPolicy.ALL : t.getIpPolicy(),
                IpAllowlist.parse(t.getIpAllowlist()),
                t.getCreatedAt()
        );
    }
}
