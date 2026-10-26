package com.inventory.msp.incident.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TicketQueueRulesTest {

    @Test
    void newTicketGetsCreationAndUpdateTimesInReportZone() {
        Ticket ticket = new Ticket();

        ticket.onCreate();

        assertNotNull(ticket.getCreatedAt());
        assertEquals(ticket.getCreatedAt(), ticket.getUpdatedAt());
    }

    @Test
    void ticketStartTimeFallsBackToAssignmentTimeWhenCreationTimeIsNull() {
        LocalDateTime assignedAt = LocalDateTime.of(2026, 10, 1, 9, 30);
        Ticket ticket = Ticket.builder().assignedAt(assignedAt).build();

        assertEquals(assignedAt, TicketQueueRules.ticketStartTime(ticket));
    }
}
