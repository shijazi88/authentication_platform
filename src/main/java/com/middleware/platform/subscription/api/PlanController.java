package com.middleware.platform.subscription.api;

import com.middleware.platform.subscription.domain.Plan;
import com.middleware.platform.subscription.domain.PlanEntitlement;
import com.middleware.platform.subscription.domain.PlanFieldEntitlement;
import com.middleware.platform.subscription.dto.AddPlanEntitlementRequest;
import com.middleware.platform.subscription.dto.CreatePlanRequest;
import com.middleware.platform.subscription.dto.PlanSubscriberCount;
import com.middleware.platform.subscription.dto.PlanSubscriberView;
import com.middleware.platform.subscription.dto.UpdateEntitlementLimitsRequest;
import com.middleware.platform.subscription.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Plan create(@Valid @RequestBody CreatePlanRequest req) {
        return planService.create(req);
    }

    @GetMapping
    public List<Plan> list() { return planService.list(); }

    @GetMapping("/subscriber-counts")
    public List<PlanSubscriberCount> subscriberCounts() {
        return planService.subscriberCounts();
    }

    @GetMapping("/{id}")
    public Plan get(@PathVariable UUID id) { return planService.get(id); }

    @GetMapping("/{id}/subscribers")
    public List<PlanSubscriberView> listSubscribers(@PathVariable UUID id) {
        return planService.listSubscribers(id);
    }

    @PostMapping("/{id}/entitlements")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanEntitlement addEntitlement(@PathVariable UUID id,
                                          @Valid @RequestBody AddPlanEntitlementRequest req) {
        return planService.addEntitlement(id, req);
    }

    @GetMapping("/{id}/entitlements")
    public List<PlanEntitlement> listEntitlements(@PathVariable UUID id) {
        return planService.listEntitlements(id);
    }

    @PutMapping("/{planId}/entitlements/{entitlementId}/limits")
    public PlanEntitlement updateLimits(@PathVariable UUID planId,
                                        @PathVariable UUID entitlementId,
                                        @RequestBody UpdateEntitlementLimitsRequest req) {
        return planService.updateLimits(planId, entitlementId,
                req.rateLimitPerMinute(), req.monthlyQuota());
    }

    @GetMapping("/{id}/field-entitlements")
    public List<PlanFieldEntitlement> listFieldEntitlements(@PathVariable UUID id) {
        return planService.listFieldEntitlements(id);
    }
}
