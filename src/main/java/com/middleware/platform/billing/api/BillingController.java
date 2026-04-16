package com.middleware.platform.billing.api;

import com.middleware.platform.billing.domain.BillingEvent;
import com.middleware.platform.billing.repo.PeriodSummary;
import com.middleware.platform.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/events")
    public Page<BillingEvent> events(@RequestParam UUID tenantId,
                                     @RequestParam String period,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "50") int size) {
        return billingService.listByTenantAndPeriod(tenantId, period,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")));
    }

    @GetMapping("/summary")
    public List<PeriodSummary> summary(@RequestParam UUID tenantId,
                                       @RequestParam String period) {
        return billingService.summarize(tenantId, period);
    }
}
