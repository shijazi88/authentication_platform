package com.middleware.platform.subscription.api;

import com.middleware.platform.subscription.domain.Subscription;
import com.middleware.platform.subscription.domain.SubscriptionStatus;
import com.middleware.platform.subscription.dto.CreateSubscriptionRequest;
import com.middleware.platform.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Subscription create(@Valid @RequestBody CreateSubscriptionRequest req) {
        return subscriptionService.create(req);
    }

    @GetMapping
    public List<Subscription> listByTenant(@RequestParam UUID tenantId) {
        return subscriptionService.listByTenant(tenantId);
    }

    @PostMapping("/{id}/status")
    public Subscription setStatus(@PathVariable UUID id, @RequestParam SubscriptionStatus status) {
        return subscriptionService.setStatus(id, status);
    }
}
