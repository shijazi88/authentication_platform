package com.middleware.platform.ticket.api;

import com.middleware.platform.ticket.domain.TicketStatus;
import com.middleware.platform.ticket.dto.StaffReplyRequest;
import com.middleware.platform.ticket.dto.TicketCountsResponse;
import com.middleware.platform.ticket.dto.TicketResponse;
import com.middleware.platform.ticket.dto.UpdateTicketRequest;
import com.middleware.platform.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Support-team side of tickets. Internal staff may read (the legacy AUDITOR
 * role included); Super Admin, Operation, Finance and Support may reply and
 * change status. The central-bank regulator has no access — tickets are
 * private client conversations. Mirrored in SecurityConfig for /admin/tickets/**.
 */
@RestController
@RequestMapping("/admin/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPS','FINANCE','SUPPORT','AUDITOR')")
public class TicketAdminController {

    private static final String REPLY_ROLES = "hasAnyRole('SUPER_ADMIN','PLATFORM_OPS','FINANCE','SUPPORT')";

    private final TicketService ticketService;

    /** {@code active=true} (default) hides resolved and closed tickets unless a status is given. */
    @GetMapping
    public Page<TicketResponse> list(@RequestParam(required = false) UUID tenantId,
                                     @RequestParam(required = false) TicketStatus status,
                                     @RequestParam(defaultValue = "true") boolean active,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "25") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        return ticketService.list(tenantId, status, status == null && active, q, pageable);
    }

    @GetMapping("/counts")
    public TicketCountsResponse counts() {
        return ticketService.counts();
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable UUID id) {
        return ticketService.get(id);
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize(REPLY_ROLES)
    public TicketResponse reply(@PathVariable UUID id, @Valid @RequestBody StaffReplyRequest req,
                                Authentication authentication) {
        return ticketService.staffReply(id, authentication.getName(), req);
    }

    @PutMapping("/{id}")
    @PreAuthorize(REPLY_ROLES)
    public TicketResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTicketRequest req) {
        return ticketService.update(id, req);
    }
}
