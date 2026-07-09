package com.middleware.platform.device.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * A fingerprint capture device registered to a tenant (client). Serial numbers
 * are unique across active (non-deleted) devices; deletion is soft.
 */
@Entity
@Table(name = "fingerprint_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FingerprintDevice {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "model", length = 255)
    private String model;

    @Column(name = "type", length = 64)
    private String type;

    @Column(name = "serial_number", nullable = false, length = 128)
    private String serialNumber;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

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
