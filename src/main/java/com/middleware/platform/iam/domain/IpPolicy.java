package com.middleware.platform.iam.domain;

/** Source-IP policy applied to a tenant's bank-facing API calls. */
public enum IpPolicy {
    /** Accept calls from any source IP. */
    ALL,
    /** Accept calls only from the tenant's approved IP allowlist. */
    RESTRICTED
}
