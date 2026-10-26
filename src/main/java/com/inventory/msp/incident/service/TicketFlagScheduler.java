package com.inventory.msp.incident.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketFlagScheduler {

    private final TicketFlagService ticketFlagService;

    @Scheduled(cron = "${cims.flag.cron:0 0 * * * *}", zone = "Asia/Kolkata")
    public void scanForFlaggedTickets() {
        ticketFlagService.processFlagsAndReminders();
    }
}
