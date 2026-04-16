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
import com.middleware.platform.subscription.dto.AddPlanEntitlementRequest;
import com.middleware.platform.subscription.dto.CreatePlanRequest;
import com.middleware.platform.subscription.repo.PlanEntitlementRepository;
import com.middleware.platform.subscription.repo.PlanFieldEntitlementRepository;
import com.middleware.platform.subscription.repo.PlanRepository;
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
}
