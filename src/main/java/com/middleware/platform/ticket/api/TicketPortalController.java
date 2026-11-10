package com.middleware.platform.ticket.api;

import com.middleware.platform.iam.security.CurrentTenant;
import com.middleware.platform.ticket.dto.CreateTicketRequest;
import com.middleware.platform.ticket.dto.TicketReplyRequest;
import com.middleware.platform.ticket.dto.TicketResponse;
import com.middleware.platform.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Client side of support tickets (tenant portal). Every call is scoped to the
 * authenticated tenant via {@link CurrentTenant}; internal staff notes and the
 * assigned agent are never returned here.
 */
@RestController
@RequestMapping("/portal-api/tickets")
@RequiredArgsConstructor
public class TicketPortalController {

    private final TicketService ticketService;

    @GetMapping
    public List<TicketResponse> list() {
        return ticketService.listForTenant(CurrentTenant.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest req) {
        return ticketService.create(CurrentTenant.id(), CurrentTenant.email(), req);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable UUID id) {
        return ticketService.getForTenant(CurrentTenant.id(), id);
    }

    @PostMapping("/{id}/messages")
    public TicketResponse reply(@PathVariable UUID id, @Valid @RequestBody TicketReplyRequest req) {
        return ticketService.clientReply(CurrentTenant.id(), id, CurrentTenant.email(), req.message());
    }

    @PostMapping("/{id}/resolve")
    public TicketResponse resolve(@PathVariable UUID id) {
        return ticketService.clientResolve(CurrentTenant.id(), id);
    }
}
