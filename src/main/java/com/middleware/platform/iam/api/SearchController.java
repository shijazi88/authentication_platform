package com.middleware.platform.iam.api;

import com.middleware.platform.iam.dto.SearchResponse;
import com.middleware.platform.iam.dto.TenantResponse;
import com.middleware.platform.iam.repo.TenantRepository;
import com.middleware.platform.transactions.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/search")
@RequiredArgsConstructor
public class SearchController {

    private static final int LIMIT = 5;
    private static final int MIN_LEN = 2;

    private final TenantRepository tenantRepository;
    private final TransactionRepository transactionRepository;

    @GetMapping
    public SearchResponse search(@RequestParam("q") String q) {
        String trimmed = q == null ? "" : q.trim();
        if (trimmed.length() < MIN_LEN) {
            return new SearchResponse(List.of(), List.of());
        }
        List<TenantResponse> tenants = tenantRepository.search(trimmed, PageRequest.of(0, LIMIT))
                .stream()
                .map(TenantResponse::from)
                .toList();
        List<SearchResponse.TransactionHit> txs = transactionRepository.search(trimmed, LIMIT)
                .stream()
                .map(t -> new SearchResponse.TransactionHit(
                        t.getId(),
                        t.getStatus(),
                        t.getProviderRequestId(),
                        t.getCreatedAt(),
                        t.getTenantId()))
                .toList();
        return new SearchResponse(tenants, txs);
    }
}
