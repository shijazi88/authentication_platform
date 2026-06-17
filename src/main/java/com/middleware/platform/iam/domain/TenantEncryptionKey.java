package com.middleware.platform.iam.domain;

import com.middleware.platform.common.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * A per-tenant encryption keypair. The public side is published as an X.509
 * cert (PEM) for clients to encrypt to; the private key PEM is stored encrypted
 * at rest via {@link EncryptedStringConverter} and used by the gateway to
 * decrypt the JWE payload.
 */
@Entity
@Table(name = "tenant_encryption_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantEncryptionKey {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "kid", nullable = false, unique = true, length = 64)
    private String kid;

    @Column(name = "algorithm", nullable = false, length = 32)
    private String algorithm;

    @Column(name = "public_cert_pem", nullable = false, columnDefinition = "mediumtext")
    private String publicCertPem;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "private_key_pem_encrypted", nullable = false, columnDefinition = "mediumtext")
    private String privateKeyPem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private EncryptionKeyStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @PrePersist
    void onCreate() { if (createdAt == null) this.createdAt = Instant.now(); }
}
