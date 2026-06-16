package com.middleware.platform.iam.service;

import com.middleware.platform.catalog.domain.ServiceOperation;
import com.middleware.platform.catalog.repo.ServiceOperationRepository;
import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.iam.domain.ApiCredential;
import com.middleware.platform.iam.dto.*;
import com.middleware.platform.iam.repo.ApiCredentialRepository;
import com.middleware.platform.subscription.domain.Plan;
import com.middleware.platform.subscription.domain.Subscription;
import com.middleware.platform.subscription.repo.PlanEntitlementRepository;
import com.middleware.platform.subscription.repo.PlanFieldEntitlementRepository;
import com.middleware.platform.subscription.repo.PlanRepository;
import com.middleware.platform.subscription.repo.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read/light-write operations a tenant performs on its own resources: API
 * credentials (list / issue / revoke) and the details of a subscribed plan.
 * Everything is passed the caller's tenantId and verified to belong to it.
 */
@Service
@RequiredArgsConstructor
public class TenantPortalService {

    private final ApiCredentialRepository credentials;
    private final TenantService tenantService;
    private final SubscriptionRepository subscriptions;
    private final PlanRepository plans;
    private final PlanEntitlementRepository planEntitlements;
    private final PlanFieldEntitlementRepository planFieldEntitlements;
    private final ServiceOperationRepository operations;

    @Transactional(readOnly = true)
    public List<CredentialView> listCredentials(UUID tenantId) {
        return credentials.findByTenantId(tenantId).stream().map(CredentialView::from).toList();
    }

    /** Issues a new key for the tenant — the plaintext secret is returned once. */
    @Transactional
    public CredentialResponse issueCredential(UUID tenantId, CreateCredentialRequest req) {
        return tenantService.issueCredential(tenantId, req);
    }

    /** Deactivates one of the tenant's own credentials (reversible by an admin). */
    @Transactional
    public void revokeCredential(UUID tenantId, UUID credentialId) {
        ApiCredential c = credentials.findById(credentialId)
                .orElseThrow(() -> ApplicationException.notFound("Credential"));
        if (!c.getTenantId().equals(tenantId)) {
            throw ApplicationException.notFound("Credential");
        }
        c.setActive(false);
    }

    @Transactional(readOnly = true)
    public SubscriptionDetailResponse subscriptionDetails(UUID tenantId, UUID subscriptionId) {
        Subscription sub = subscriptions.findById(subscriptionId)
                .orElseThrow(() -> ApplicationException.notFound("Subscription"));
        if (!sub.getTenantId().equals(tenantId)) {
            throw ApplicationException.notFound("Subscription");
        }
        Plan plan = plans.findById(sub.getPlanId())
                .orElseThrow(() -> ApplicationException.notFound("Plan"));

        var ents = planEntitlements.findByPlanId(plan.getId());
        Map<UUID, ServiceOperation> opsById = operations
                .findAllById(ents.stream().map(e -> e.getOperationId()).toList())
                .stream()
                .collect(Collectors.toMap(ServiceOperation::getId, Function.identity()));

        List<PlanOperationView> opViews = ents.stream().map(e -> {
            ServiceOperation op = opsById.get(e.getOperationId());
            long price = e.getUnitPriceOverrideMinor() != null
                    ? e.getUnitPriceOverrideMinor()
                    : (op != null ? op.getDefaultUnitPriceMinor() : 0L);
            return new PlanOperationView(
                    op != null ? op.getCode() : e.getOperationId().toString(),
                    op != null ? op.getName() : null,
                    e.getRateLimitPerMinute(),
                    e.getMonthlyQuota(),
                    price,
                    op != null ? op.getCurrency() : plan.getCurrency());
        }).toList();

        List<String> fields = planFieldEntitlements.findByPlanId(plan.getId()).stream()
                .map(f -> f.getFieldPath())
                .filter(p -> p != null)
                .sorted()
                .toList();

        return new SubscriptionDetailResponse(
                sub.getId(),
                sub.getStatus().name(),
                sub.getStartDate() != null ? sub.getStartDate().toString() : null,
                sub.getEndDate() != null ? sub.getEndDate().toString() : null,
                plan.getCode(), plan.getName(), plan.getDescription(),
                plan.getBaseFeeMinor(), plan.getCurrency(),
                opViews, fields);
    }
}
