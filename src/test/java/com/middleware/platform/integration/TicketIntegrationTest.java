package com.middleware.platform.integration;

import com.middleware.platform.iam.dto.CreateTenantRequest;
import com.middleware.platform.iam.dto.TenantResponse;
import com.middleware.platform.iam.service.TenantService;
import com.middleware.platform.ticket.domain.TicketCategory;
import com.middleware.platform.ticket.domain.TicketClosedBy;
import com.middleware.platform.ticket.domain.TicketStatus;
import com.middleware.platform.ticket.dto.CreateTicketRequest;
import com.middleware.platform.ticket.dto.StaffReplyRequest;
import com.middleware.platform.ticket.dto.TicketResponse;
import com.middleware.platform.ticket.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tickets against a real MySQL: the V26 migration, the database-assigned
 * ticket number, the admin filter query and the client/staff views.
 */
@SpringBootTest
@Import(TestContainersConfig.class)
@Testcontainers
class TicketIntegrationTest {

    @Autowired
    TenantService tenantService;

    @Autowired
    TicketService ticketService;

    @Test
    @DisplayName("ticket numbers come from the database, and internal notes stay out of the client view")
    void ticketLifecycle() {
        TenantResponse tenant = tenantService.create(
                new CreateTenantRequest("TICKET_BANK_" + System.nanoTime(), "Ticket Bank Ltd", null));

        TicketResponse first = ticketService.create(tenant.id(), "ops@bank.example",
                new CreateTicketRequest("Top-up not showing", TicketCategory.BILLING, null, null, "We transferred yesterday."));
        TicketResponse second = ticketService.create(tenant.id(), "ops@bank.example",
                new CreateTicketRequest("Scanner question", TicketCategory.VERIFICATION, null, "REQ-42", "Which scanner?"));

        assertThat(first.ticketNo()).isGreaterThanOrEqualTo(1001L);
        assertThat(second.ticketNo()).isGreaterThan(first.ticketNo());

        ticketService.staffReply(first.id(), "agent@motabiq.example",
                new StaffReplyRequest("Checked with finance", true, null));
        TicketResponse replied = ticketService.staffReply(first.id(), "agent@motabiq.example",
                new StaffReplyRequest("The top-up is now on your wallet.", false, TicketStatus.RESOLVED));

        assertThat(replied.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(replied.messages()).hasSize(3);

        TicketResponse clientView = ticketService.getForTenant(tenant.id(), first.id());
        assertThat(clientView.messages()).hasSize(2).noneMatch(m -> m.internal());
        assertThat(clientView.assignedTo()).isNull();

        PageRequest page = PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        assertThat(ticketService.list(tenant.id(), null, true, null, page).getContent())
                .extracting(TicketResponse::id).containsExactly(second.id());
        assertThat(ticketService.list(tenant.id(), null, false, String.valueOf(first.ticketNo()), page).getContent())
                .extracting(TicketResponse::id).containsExactly(first.id());
        assertThat(ticketService.list(tenant.id(), null, false, "scanner", page).getContent())
                .extracting(TicketResponse::tenantName).containsExactly("Ticket Bank Ltd");

        assertThat(ticketService.counts().needsReply()).isGreaterThanOrEqualTo(1L);

        TicketResponse confirmed = ticketService.clientConfirm(tenant.id(), first.id());
        assertThat(confirmed.status()).isEqualTo(TicketStatus.CLOSED);
        assertThat(confirmed.closedBy()).isEqualTo(TicketClosedBy.CLIENT);
        assertThat(ticketService.autoCloseUnconfirmed()).isZero();
    }
}
