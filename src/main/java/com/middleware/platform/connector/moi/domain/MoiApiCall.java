package com.middleware.platform.connector.moi.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit row for one outbound HTTP call to the MOI server (auth or verify).
 * Written async after each call (both success and failure paths). Bodies are
 * redacted + truncated to 64 KiB before persistence.
 */
@Entity
@Table(name = "moi_api_calls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoiApiCall {

    public enum Kind { AUTH, VERIFY }

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private Kind kind;

    @Column(name = "transaction_id", length = 36)
    private String transactionId;

    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    @Column(name = "method", nullable = false, length = 8)
    private String method;

    @Column(name = "request_headers_json", columnDefinition = "mediumtext")
    private String requestHeadersJson;

    @Column(name = "request_body_json", columnDefinition = "mediumtext")
    private String requestBodyJson;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_headers_json", columnDefinition = "mediumtext")
    private String responseHeadersJson;

    @Column(name = "response_body_json", columnDefinition = "mediumtext")
    private String responseBodyJson;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    @Column(name = "token_snippet", length = 128)
    private String tokenSnippet;

    public static String newId() {
        return UUID.randomUUID().toString();
    }
}
