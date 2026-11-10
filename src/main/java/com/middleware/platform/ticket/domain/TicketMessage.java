package com.middleware.platform.ticket.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/** One message in a ticket thread. {@code internal} notes are staff-only. */
@Entity
@Table(name = "support_ticket_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketMessage {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ticket_id", nullable = false, updatable = false)
    private UUID ticketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false, length = 16)
    private TicketAuthorType authorType;

    @Column(name = "author_email", nullable = false, length = 255)
    private String authorEmail;

    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "internal", nullable = false)
    private boolean internal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
