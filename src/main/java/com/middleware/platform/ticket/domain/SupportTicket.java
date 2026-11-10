package com.middleware.platform.ticket.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.util.UUID;

/** A client's support ticket. The conversation lives in {@link TicketMessage}. */
@Entity
@Table(name = "support_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Short number shown to both sides; assigned by the database on insert. */
    @Generated(event = EventType.INSERT)
    @Column(name = "ticket_no", insertable = false, updatable = false)
    private Long ticketNo;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private TicketStatus status;

    @Column(name = "reference", length = 128)
    private String reference;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "assigned_to", length = 255)
    private String assignedTo;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_message_by", nullable = false, length = 16)
    private TicketAuthorType lastMessageBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "closed_by", length = 16)
    private TicketClosedBy closedBy;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (lastMessageAt == null) lastMessageAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
