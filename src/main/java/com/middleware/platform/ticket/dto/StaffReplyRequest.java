package com.middleware.platform.ticket.dto;

import com.middleware.platform.ticket.domain.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Support-team reply. {@code internal} = a staff-only note the client never
 * sees. {@code status} = where the ticket goes after a public reply; null means
 * WAITING_ON_CLIENT. Internal notes never change the status.
 */
public record StaffReplyRequest(
        @NotBlank @Size(max = 10_000) String message,
        boolean internal,
        TicketStatus status
) {}
