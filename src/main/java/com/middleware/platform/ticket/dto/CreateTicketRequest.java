package com.middleware.platform.ticket.dto;

import com.middleware.platform.ticket.domain.TicketCategory;
import com.middleware.platform.ticket.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Client opens a ticket: subject + first message. Priority defaults to NORMAL. */
public record CreateTicketRequest(
        @NotBlank @Size(max = 200) String subject,
        @NotNull TicketCategory category,
        TicketPriority priority,
        @Size(max = 128) String reference,
        @NotBlank @Size(max = 10_000) String message
) {}
