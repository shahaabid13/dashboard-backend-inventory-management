package com.inventory.msp.incident.service;

import com.inventory.msp.incident.model.TicketStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketFlagServiceTest {

    @Test
    void onlyUnactedOpenTicketsOlderThanTwentyFourHoursAreFlagged() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 12, 10, 0);

        assertFalse(TicketFlagService.shouldFlag(TicketStatus.OPEN, now.minusHours(23), false, now, 24));
        assertFalse(TicketFlagService.shouldFlag(TicketStatus.OPEN, now.minusHours(24), false, now, 24));
        assertTrue(TicketFlagService.shouldFlag(TicketStatus.OPEN, now.minusHours(25), false, now, 24));
        assertFalse(TicketFlagService.shouldFlag(TicketStatus.OPEN, now.minusHours(25), true, now, 24));
        assertFalse(TicketFlagService.shouldFlag(TicketStatus.RESOLVED, now.minusHours(25), false, now, 24));
        assertFalse(TicketFlagService.shouldFlag(TicketStatus.COORDINATOR_REVIEW, now.minusHours(25), false, now, 24));
    }
}
