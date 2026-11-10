package com.middleware.platform.ticket.dto;

import com.middleware.platform.ticket.domain.TicketAuthorType;
import com.middleware.platform.ticket.domain.TicketMessage;

import java.time.Instant;
import java.util.UUID;

/**
 * One message. {@code authorEmail} is null in the client view for staff
 * messages — the client sees "MOTABIQ Support", not who on the team answered.
 */
public record TicketMessageResponse(
        UUID id,
        TicketAuthorType authorType,
        String authorEmail,
        String body,
        boolean internal,
        Instant createdAt
) {
    public static TicketMessageResponse forStaff(TicketMessage m) {
        return new TicketMessageResponse(m.getId(), m.getAuthorType(), m.getAuthorEmail(),
                m.getBody(), m.isInternal(), m.getCreatedAt());
    }

    public static TicketMessageResponse forClient(TicketMessage m) {
        String email = m.getAuthorType() == TicketAuthorType.CLIENT ? m.getAuthorEmail() : null;
        return new TicketMessageResponse(m.getId(), m.getAuthorType(), email,
                m.getBody(), false, m.getCreatedAt());
    }
}
