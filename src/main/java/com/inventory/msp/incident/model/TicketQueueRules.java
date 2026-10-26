package com.inventory.msp.incident.model;

import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.List;

public final class TicketQueueRules {

    public static final TicketStatus DEFAULTER_STATUS = TicketStatus.OPEN;
    public static final List<TicketStatus> DEFAULTER_STATUSES = List.of(DEFAULTER_STATUS);
    public static final List<TicketStatus> QUEUE_STATUSES = List.of(TicketStatus.OPEN, TicketStatus.PENDING);
    public static final List<TicketStatus> DASHBOARD_STATUSES = List.of(
            TicketStatus.OPEN,
            TicketStatus.PENDING,
            TicketStatus.ASSIGNED_TO_REVIEWER,
            TicketStatus.REVALIDATION,
            TicketStatus.RESOLVED);
    public static final List<String> QUEUE_STATUS_NAMES = QUEUE_STATUSES.stream()
            .map(Enum::name)
            .toList();
    public static final String TICKET_START_TIME_JPQL = "COALESCE(t.createdAt, t.assignedAt)";
    public static final String TICKET_START_TIME_SQL = "COALESCE(t.createdAt, t.assignedAt)";
    public static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Kolkata");

    private TicketQueueRules() {
    }

    public static LocalDateTime ticketStartTime(Ticket ticket) {
        return ticket.getCreatedAt() != null ? ticket.getCreatedAt() : ticket.getAssignedAt();
    }
}
