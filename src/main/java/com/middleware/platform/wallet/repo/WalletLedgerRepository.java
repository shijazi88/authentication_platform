package com.middleware.platform.wallet.repo;

import com.middleware.platform.wallet.domain.WalletEntryType;
import com.middleware.platform.wallet.domain.WalletLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletLedgerRepository extends JpaRepository<WalletLedgerEntry, UUID> {

    Page<WalletLedgerEntry> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    /** Guards against double-applying the same transaction debit. */
    boolean existsByReferenceAndEntryType(String reference, WalletEntryType entryType);
}
