package com.middleware.platform.ticket.service;

import com.middleware.platform.common.error.ApplicationException;
import com.middleware.platform.common.error.ErrorCode;
import com.middleware.platform.iam.domain.AdminRole;
import com.middleware.platform.iam.domain.AdminUser;
import com.middleware.platform.iam.domain.Tenant;
import com.middleware.platform.iam.repo.AdminUserRepository;
import com.middleware.platform.iam.repo.TenantRepository;
import com.middleware.platform.ticket.domain.SupportTicket;
import com.middleware.platform.ticket.domain.TicketAuthorType;
import com.middleware.platform.ticket.domain.TicketClosedBy;
import com.middleware.platform.ticket.domain.TicketMessage;
import com.middleware.platform.ticket.domain.TicketPriority;
import com.middleware.platform.ticket.domain.TicketStatus;
import com.middleware.platform.ticket.dto.CreateTicketRequest;
import com.middleware.platform.ticket.dto.StaffReplyRequest;
import com.middleware.platform.ticket.dto.TicketAssigneeResponse;
import com.middleware.platform.ticket.dto.TicketCountsResponse;
import com.middleware.platform.ticket.dto.TicketMessageResponse;
import com.middleware.platform.ticket.dto.TicketResponse;
import com.middleware.platform.ticket.dto.UpdateTicketRequest;
import com.middleware.platform.ticket.repo.SupportTicketRepository;
import com.middleware.platform.ticket.repo.TicketMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Support tickets. Clients open tickets and reply from the tenant portal; the
 * support team replies, adds internal notes and moves the status from the
 * admin portal.
 *
 * <p>Status rules: a public staff reply sets WAITING_ON_CLIENT unless the agent
 * picks another status. A client reply puts the ticket back to OPEN (reopening
 * a RESOLVED one), except that IN_PROGRESS stays IN_PROGRESS. CLOSED is final
 * for public replies; staff can still add internal notes or reopen it.
 *
 * <p>Solving needs the client: RESOLVED means the team considers it solved and
 * waits for the client to confirm. The client confirms (→ CLOSED by CLIENT) or
 * replies (→ reopened). Unanswered, it closes itself after {@code autoCloseDays}
 * (→ CLOSED by AUTO).
 */
@Slf4j
@Service
public class TicketService {

    private static final Set<TicketStatus> DONE = EnumSet.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);
    /** Admin roles that answer tickets — the only valid assignees. */
    private static final Set<AdminRole> AGENT_ROLES =
            EnumSet.of(AdminRole.SUPER_ADMIN, AdminRole.PLATFORM_OPS, AdminRole.FINANCE, AdminRole.SUPPORT);

    private final SupportTicketRepository tickets;
    private final TicketMessageRepository messages;
    private final TenantRepository tenants;
    private final AdminUserRepository adminUsers;
    private final int autoCloseDays;

    public TicketService(SupportTicketRepository tickets, TicketMessageRepository messages,
                         TenantRepository tenants, AdminUserRepository adminUsers,
                         @Value("${platform.tickets.auto-close-days:7}") int autoCloseDays) {
        this.tickets = tickets;
        this.messages = messages;
        this.tenants = tenants;
        this.adminUsers = adminUsers;
        this.autoCloseDays = autoCloseDays;
    }

    // ── Client (tenant portal) ────────────────────────────────────────────────

    @Transactional
    public TicketResponse create(UUID tenantId, String email, CreateTicketRequest req) {
        SupportTicket t = SupportTicket.builder()
                .tenantId(tenantId)
                .subject(req.subject().trim())
                .category(req.category())
                .priority(req.priority() != null ? req.priority() : TicketPriority.NORMAL)
                .status(TicketStatus.OPEN)
                .reference(blankToNull(req.reference()))
                .createdBy(email)
                .lastMessageBy(TicketAuthorType.CLIENT)
                .build();
        // Flush so the database assigns ticket_no before we return it.
        tickets.saveAndFlush(t);
        TicketMessage m = messages.save(message(t.getId(), TicketAuthorType.CLIENT, email, req.message(), false));
        log.info("Ticket #{} opened: tenant={} category={}", t.getTicketNo(), tenantId, t.getCategory());
        return TicketResponse.forClient(t, autoCloseDays, List.of(TicketMessageResponse.forClient(m)));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listForTenant(UUID tenantId) {
        return tickets.findByTenantIdOrderByLastMessageAtDesc(tenantId).stream()
                .map(t -> TicketResponse.forClient(t, autoCloseDays, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getForTenant(UUID tenantId, UUID ticketId) {
        SupportTicket t = requireTenantTicket(tenantId, ticketId);
        return TicketResponse.forClient(t, autoCloseDays, publicThread(t.getId()));
    }

    @Transactional
    public TicketResponse clientReply(UUID tenantId, UUID ticketId, String email, String body) {
        SupportTicket t = requireTenantTicket(tenantId, ticketId);
        if (t.getStatus() == TicketStatus.CLOSED) {
            throw ApplicationException.conflict("This ticket is closed. Please open a new ticket.");
        }
        TicketMessage m = messages.save(message(t.getId(), TicketAuthorType.CLIENT, email, body, false));
        if (t.getStatus() != TicketStatus.IN_PROGRESS) {
            applyStatus(t, TicketStatus.OPEN);
        }
        t.setLastMessageAt(m.getCreatedAt());
        t.setLastMessageBy(TicketAuthorType.CLIENT);
        return TicketResponse.forClient(t, autoCloseDays, publicThread(t.getId()));
    }

    /**
     * The client confirms its issue is solved — answering the team's RESOLVED,
     * or on its own at any time. Either way the ticket closes.
     */
    @Transactional
    public TicketResponse clientConfirm(UUID tenantId, UUID ticketId) {
        SupportTicket t = requireTenantTicket(tenantId, ticketId);
        if (t.getStatus() == TicketStatus.CLOSED) {
            throw ApplicationException.conflict("This ticket is already closed.");
        }
        close(t, TicketClosedBy.CLIENT);
        return TicketResponse.forClient(t, autoCloseDays, publicThread(t.getId()));
    }

    // ── Support team (admin portal) ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TicketResponse> list(UUID tenantId, TicketStatus status, boolean activeOnly,
                                     String q, Pageable pageable) {
        Page<SupportTicket> page = tickets.filter(tenantId, status, activeOnly, DONE, blankToNull(q), pageable);
        Map<UUID, String> names = tenantNames(page.getContent().stream().map(SupportTicket::getTenantId).collect(Collectors.toSet()));
        return page.map(t -> TicketResponse.forStaff(t, autoCloseDays, names.get(t.getTenantId()), null));
    }

    @Transactional(readOnly = true)
    public TicketCountsResponse counts() {
        Map<TicketStatus, Long> byStatus = new EnumMap<>(TicketStatus.class);
        for (TicketStatus s : TicketStatus.values()) byStatus.put(s, 0L);
        tickets.countByStatus().forEach(c -> byStatus.put(c.getStatus(), c.getTotal()));
        long active = byStatus.entrySet().stream()
                .filter(e -> e.getKey().isActive()).mapToLong(Map.Entry::getValue).sum();
        return new TicketCountsResponse(byStatus.get(TicketStatus.OPEN), active, byStatus);
    }

    @Transactional(readOnly = true)
    public TicketResponse get(UUID ticketId) {
        SupportTicket t = requireTicket(ticketId);
        return staffView(t);
    }

    @Transactional
    public TicketResponse staffReply(UUID ticketId, String adminEmail, StaffReplyRequest req) {
        SupportTicket t = requireTicket(ticketId);
        if (!req.internal() && t.getStatus() == TicketStatus.CLOSED) {
            throw ApplicationException.conflict("Ticket is closed — reopen it before replying to the client");
        }
        TicketMessage m = messages.save(message(t.getId(), TicketAuthorType.STAFF, adminEmail, req.message(), req.internal()));
        if (!req.internal()) {
            applyStaffStatus(t, req.status() != null ? req.status() : TicketStatus.WAITING_ON_CLIENT);
            t.setLastMessageAt(m.getCreatedAt());
            t.setLastMessageBy(TicketAuthorType.STAFF);
        }
        // The first agent to answer takes the ticket unless someone already has it.
        if (t.getAssignedTo() == null) {
            t.setAssignedTo(adminEmail);
        }
        return staffView(t);
    }

    @Transactional
    public TicketResponse update(UUID ticketId, UpdateTicketRequest req) {
        SupportTicket t = requireTicket(ticketId);
        if (req.status() != null) applyStaffStatus(t, req.status());
        if (req.priority() != null) t.setPriority(req.priority());
        if (req.assignedTo() != null) t.setAssignedTo(requireAssignee(req.assignedTo()));
        return staffView(t);
    }

    /** Active team members who answer tickets, for the "Assigned to" picker. */
    @Transactional(readOnly = true)
    public List<TicketAssigneeResponse> assignees() {
        return adminUsers.findAllByOrderByCreatedAtAsc().stream()
                .filter(u -> u.isActive() && AGENT_ROLES.contains(u.getRole()))
                .map(u -> new TicketAssigneeResponse(u.getEmail(), u.getDisplayName(), u.getRole()))
                .toList();
    }

    /** Closes RESOLVED tickets the client has not answered within {@code autoCloseDays}. */
    @Scheduled(cron = "${platform.tickets.auto-close-cron:0 15 * * * *}")
    @Transactional
    public int autoCloseUnconfirmed() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(autoCloseDays));
        List<SupportTicket> stale = tickets.findByStatusAndResolvedAtBefore(TicketStatus.RESOLVED, cutoff);
        stale.forEach(t -> close(t, TicketClosedBy.AUTO));
        tickets.saveAll(stale);
        if (!stale.isEmpty()) {
            log.info("Auto-closed {} resolved ticket(s) unconfirmed for {} days", stale.size(), autoCloseDays);
        }
        return stale.size();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void applyStatus(SupportTicket t, TicketStatus next) {
        Instant now = Instant.now();
        // A fresh RESOLVED restarts the client's confirmation window.
        if (next == TicketStatus.RESOLVED && t.getStatus() != TicketStatus.RESOLVED) t.setResolvedAt(now);
        if (next == TicketStatus.CLOSED && t.getClosedAt() == null) t.setClosedAt(now);
        if (next != TicketStatus.CLOSED) {
            t.setClosedAt(null);
            t.setClosedBy(null);
        }
        if (next.isActive()) t.setResolvedAt(null);
        t.setStatus(next);
    }

    private void applyStaffStatus(SupportTicket t, TicketStatus next) {
        if (next == TicketStatus.CLOSED && t.getStatus() != TicketStatus.CLOSED) {
            close(t, TicketClosedBy.STAFF);
        } else {
            applyStatus(t, next);
        }
    }

    private void close(SupportTicket t, TicketClosedBy by) {
        applyStatus(t, TicketStatus.CLOSED);
        t.setClosedBy(by);
    }

    /** Blank unassigns; anything else must be an active team member who answers tickets. */
    private String requireAssignee(String email) {
        String e = blankToNull(email);
        if (e == null) return null;
        Optional<AdminUser> u = adminUsers.findByEmail(e);
        if (u.isEmpty() || !u.get().isActive() || !AGENT_ROLES.contains(u.get().getRole())) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST, "Tickets can only be assigned to an active support team member");
        }
        return u.get().getEmail();
    }

    private TicketResponse staffView(SupportTicket t) {
        List<TicketMessageResponse> thread = messages.findByTicketIdOrderByCreatedAtAsc(t.getId()).stream()
                .map(TicketMessageResponse::forStaff).toList();
        String name = tenants.findById(t.getTenantId()).map(Tenant::getLegalName).orElse(null);
        return TicketResponse.forStaff(t, autoCloseDays, name, thread);
    }

    private List<TicketMessageResponse> publicThread(UUID ticketId) {
        return messages.findByTicketIdAndInternalFalseOrderByCreatedAtAsc(ticketId).stream()
                .map(TicketMessageResponse::forClient).toList();
    }

    private Map<UUID, String> tenantNames(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return tenants.findAllById(ids).stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getLegalName));
    }

    private SupportTicket requireTicket(UUID ticketId) {
        return tickets.findById(ticketId).orElseThrow(() -> ApplicationException.notFound("Ticket"));
    }

    /** A tenant can only reach its own tickets; anything else reads as not found. */
    private SupportTicket requireTenantTicket(UUID tenantId, UUID ticketId) {
        SupportTicket t = requireTicket(ticketId);
        if (!t.getTenantId().equals(tenantId)) {
            throw ApplicationException.notFound("Ticket");
        }
        return t;
    }

    private static TicketMessage message(UUID ticketId, TicketAuthorType type, String email,
                                         String body, boolean internal) {
        return TicketMessage.builder()
                .ticketId(ticketId)
                .authorType(type)
                .authorEmail(email)
                .body(body.trim())
                .internal(internal)
                .createdAt(Instant.now())
                .build();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
