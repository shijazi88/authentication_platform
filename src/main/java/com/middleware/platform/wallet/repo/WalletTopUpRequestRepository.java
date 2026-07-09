package com.middleware.platform.wallet.repo;

import com.middleware.platform.wallet.domain.TopUpRequestStatus;
import com.middleware.platform.wallet.domain.WalletTopUpRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WalletTopUpRequestRepository extends JpaRepository<WalletTopUpRequest, UUID> {
    List<WalletTopUpRequest> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<WalletTopUpRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, TopUpRequestStatus status);
    long countByStatus(TopUpRequestStatus status);
}
