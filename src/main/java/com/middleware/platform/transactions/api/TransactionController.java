package com.middleware.platform.transactions.api;

import com.middleware.platform.transactions.domain.Transaction;
import com.middleware.platform.transactions.domain.TransactionPayload;
import com.middleware.platform.transactions.dto.TransactionDetail;
import com.middleware.platform.transactions.repo.TransactionPayloadRepository;
import com.middleware.platform.transactions.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionPayloadRepository payloadRepository;

    @GetMapping
    public Page<Transaction> listByTenant(@RequestParam UUID tenantId,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "50") int size) {
        return transactionService.listByTenant(tenantId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /**
     * Returns the transaction row joined with its bank-facing and provider-
     * facing JSON payloads (from {@code transaction_payloads}). Used by the
     * portal's transaction detail page to render the full request/response
     * trail.
     */
    @GetMapping("/{id}")
    public TransactionDetail get(@PathVariable UUID id) {
        Transaction tx = transactionService.get(id);
        TransactionPayload payload = payloadRepository.findById(id).orElse(null);
        return TransactionDetail.from(tx, payload);
    }
}
