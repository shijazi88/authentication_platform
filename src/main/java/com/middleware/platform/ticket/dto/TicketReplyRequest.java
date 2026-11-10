package com.middleware.platform.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Client reply on its own ticket. */
public record TicketReplyRequest(
        @NotBlank @Size(max = 10_000) String message
) {}
