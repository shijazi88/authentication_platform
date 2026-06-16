package com.middleware.platform.wallet.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Prepaid balance for a tenant. At most one per tenant. Balance is held in
 * minor currency units; all changes go through {@link WalletLedgerEntry}.
 */
@Entity
@Table(name = "tenant_wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantWallet {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "balance_minor", nullable = false)
    private long balanceMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "low_balance_threshold_minor")
    private Long lowBalanceThresholdMinor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
