package com.middleware.platform.wallet.repo;

import com.middleware.platform.wallet.domain.TenantWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TenantWalletRepository extends JpaRepository<TenantWallet, UUID> {

    Optional<TenantWallet> findByTenantId(UUID tenantId);

    /**
     * Loads the wallet with a row-level write lock (SELECT … FOR UPDATE) so
     * concurrent debits/credits serialize and the balance never races.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from TenantWallet w where w.tenantId = :tenantId")
    Optional<TenantWallet> findByTenantIdForUpdate(UUID tenantId);
}
