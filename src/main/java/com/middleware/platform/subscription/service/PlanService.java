package com.middleware.platform.subscription.service;

import com.middleware.platform.catalog.domain.FieldDefinition;
import com.middleware.platform.catalog.domain.ServiceOperation;
import com.middleware.platform.catalog.repo.FieldDefinitionRepository;
import com.middleware.platform.catalog.service.CatalogService;
import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.config.CacheConfig;
import com.middleware.platform.subscription.domain.Plan;
import com.middleware.platform.subscription.domain.PlanEntitlement;
import com.middleware.platform.subscription.domain.PlanFieldEntitlement;
import com.middleware.platform.iam.domain.Tenant;
import com.middleware.platform.iam.repo.TenantRepository;
import com.middleware.platform.subscription.domain.Subscription;
import com.middleware.platform.subscription.dto.AddPlanEntitlementRequest;
import com.middleware.platform.subscription.dto.CreatePlanRequest;
import com.middleware.platform.subscription.dto.PlanSubscriberCount;
import com.middleware.platform.subscription.dto.PlanSubscriberView;
import com.middleware.platform.subscription.repo.PlanEntitlementRepository;
import com.middleware.platform.subscription.repo.PlanFieldEntitlementRepository;
import com.middleware.platform.subscription.repo.PlanRepository;
import com.middleware.platform.subscription.repo.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanEntitlementRepository entitlementRepository;
    private final PlanFieldEntitlementRepository fieldEntitlementRepository;
    private final FieldDefinitionRepository fieldRepository;
    private final CatalogService catalogService;
    private final SubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public Plan create(CreatePlanRequest req) {
        if (planRepository.existsByCode(req.code())) {
            throw new ApplicationException(ErrorCode.CONFLICT, "Plan code already exists: " + req.code());
        }
        Plan plan = Plan.builder()
                .code(req.code())
                .name(req.name())
                .description(req.description())
                .baseFeeMinor(req.baseFeeMinor())
                .currency(req.currency())
                .active(true)
                .build();
        return planRepository.save(plan);
    }

    @Transactional(readOnly = true)
    public List<Plan> list() { return planRepository.findAll(); }

    @Transactional(readOnly = true)
    public Plan get(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> ApplicationException.notFound("Plan"));
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_PLAN_FIELD_PATHS, key = "#planId")
    public PlanEntitlement addEntitlement(UUID planId, AddPlanEntitlementRequest req) {
        Plan plan = get(planId);
        ServiceOperation op = catalogService.getOperation(req.serviceCode(), req.operationCode());

        entitlementRepository.findByPlanIdAndOperationId(plan.getId(), op.getId())
                .ifPresent(e -> { throw new ApplicationException(ErrorCode.CONFLICT,
                        "Entitlement already exists for this operation"); });

        PlanEntitlement entitlement = PlanEntitlement.builder()
                .planId(plan.getId())
                .operationId(op.getId())
                .unitPriceOverrideMinor(req.unitPriceOverrideMinor())
                .monthlyQuota(req.monthlyQuota())
                .rateLimitPerMinute(req.rateLimitPerMinute())
                .build();
        entitlementRepository.save(entitlement);

        // Map field paths -> field definitions and persist field entitlements.
        if (req.visibleFieldPaths() != null && !req.visibleFieldPaths().isEmpty()) {
            List<FieldDefinition> allFields = fieldRepository.findByOperationId(op.getId());
            Map<String, FieldDefinition> byPath = allFields.stream()
                    .collect(Collectors.toMap(FieldDefinition::getPath, f -> f));
            for (String path : req.visibleFieldPaths()) {
                FieldDefinition def = byPath.get(path);
                if (def == null) {
                    throw new ApplicationException(ErrorCode.BAD_REQUEST,
                            "Unknown field path '" + path + "' for operation "
                                    + req.serviceCode() + "/" + req.operationCode());
                }
                fieldEntitlementRepository.save(PlanFieldEntitlement.builder()
                        .planId(plan.getId())
                        .fieldId(def.getId())
                        .fieldPath(def.getPath())
                        .build());
            }
        }
        return entitlement;
    }

    @Transactional(readOnly = true)
    public List<PlanEntitlement> listEntitlements(UUID planId) {
        return entitlementRepository.findByPlanId(planId);
    }

    @Transactional(readOnly = true)
    public List<PlanFieldEntitlement> listFieldEntitlements(UUID planId) {
        return fieldEntitlementRepository.findByPlanId(planId);
    }

    /**
     * Lists all subscriptions on a given plan, joined with the tenant's
     * legal name + code so the UI can render the "who joined" table
     * without firing one /admin/tenants/{id} request per row.
     */
    @Transactional(readOnly = true)
    public List<PlanSubscriberView> listSubscribers(UUID planId) {
        planRepository.findById(planId)
                .orElseThrow(() -> ApplicationException.notFound("Plan"));
        List<Subscription> subs = subscriptionRepository.findByPlanId(planId);
        if (subs.isEmpty()) return List.of();
        List<UUID> tenantIds = subs.stream().map(Subscription::getTenantId).distinct().toList();
        Map<UUID, Tenant> byId = tenantRepository.findAllById(tenantIds).stream()
                .collect(Collectors.toMap(Tenant::getId, t -> t));
        return subs.stream()
                .map(s -> {
                    Tenant t = byId.get(s.getTenantId());
                    return new PlanSubscriberView(
                            s.getId(),
                            s.getTenantId(),
                            t != null ? t.getCode() : null,
                            t != null ? t.getLegalName() : null,
                            s.getStatus(),
                            s.getStartDate(),
                            s.getEndDate(),
                            s.getCreatedAt()
                    );
                })
                .toList();
    }

    /**
     * Aggregate subscription counts (total + active) per plan. Single
     * grouped query; the plans list page calls this once.
     */
    @Transactional(readOnly = true)
    public List<PlanSubscriberCount> subscriberCounts() {
        return subscriptionRepository.countByPlanRaw().stream()
                .map(row -> new PlanSubscriberCount(
                        (UUID) row[0],
                        ((Number) row[1]).longValue(),
                        row[2] != null ? ((Number) row[2]).longValue() : 0L))
                .toList();
    }

    @Transactional
    public PlanEntitlement updateLimits(UUID planId, UUID entitlementId,
                                        Integer rateLimitPerMinute, Long monthlyQuota) {
        PlanEntitlement e = entitlementRepository.findById(entitlementId)
                .orElseThrow(() -> ApplicationException.notFound("Entitlement"));
        if (!e.getPlanId().equals(planId)) {
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED,
                    "Entitlement does not belong to this plan");
        }
        if (rateLimitPerMinute != null && rateLimitPerMinute <= 0) {
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED,
                    "rateLimitPerMinute must be positive or null for unlimited");
        }
        if (monthlyQuota != null && monthlyQuota <= 0) {
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED,
                    "monthlyQuota must be positive or null for unlimited");
        }
        e.setRateLimitPerMinute(rateLimitPerMinute);
        e.setMonthlyQuota(monthlyQuota);
        return e;
    }
}
