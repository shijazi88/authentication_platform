package com.middleware.platform.connector.moi.domain;

import com.middleware.platform.common.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Single-row configuration table for the MOI Yemen ID integration. The row
 * with id=1 always exists (seeded by Flyway V9); PUT /admin/moi-credentials
 * updates it in-place. The password column is encrypted by
 * {@link EncryptedStringConverter} when a column-encryption key is configured.
 */
@Entity
@Table(name = "moi_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoiCredentials {

    /** Always 1 — enforced by a CHECK constraint at the DB level. */
    public static final long SINGLETON_ID = 1L;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "auth_url", nullable = false, length = 512)
    private String authUrl;

    @Column(name = "verify_url", nullable = false, length = 512)
    private String verifyUrl;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    /**
     * Stored encrypted on disk; transparently decrypted via
     * {@link EncryptedStringConverter}. Plaintext seed rows are auto-upgraded
     * on the next save.
     */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "password_encrypted", nullable = false, length = 2048)
    private String password;

    @Column(name = "domain_name", nullable = false, length = 128)
    private String domain;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "connect_timeout_ms", nullable = false)
    private int connectTimeoutMs;

    @Column(name = "read_timeout_ms", nullable = false)
    private int readTimeoutMs;

    @Column(name = "token_refresh_skew_sec", nullable = false)
    private int tokenRefreshSkewSec;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
