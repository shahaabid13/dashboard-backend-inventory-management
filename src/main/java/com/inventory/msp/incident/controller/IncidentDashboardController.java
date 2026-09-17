package com.inventory.msp.incident.controller;

import com.inventory.msp.incident.dto.IncidentDashboardStatsResponse;
import com.inventory.msp.incident.dto.DailyTicketCountResponse;
import com.inventory.msp.incident.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents/dashboard")
@RequiredArgsConstructor
public class IncidentDashboardController {

    private final TicketService ticketService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IncidentDashboardStatsResponse> getDashboardStats() {
        Map<String, Long> stats = ticketService.getTicketStatistics();
        Map<String, Long> priorityBreakdown = ticketService.getPriorityBreakdown();

        IncidentDashboardStatsResponse response = IncidentDashboardStatsResponse.builder()
                .totalTickets(stats.get("TOTAL"))
                .openTickets(stats.get("OPEN"))
                .coordinatorReviewTickets(stats.get("COORDINATOR_REVIEW"))
                .assignedToReviewerTickets(stats.get("ASSIGNED_TO_REVIEWER"))
                .pendingTickets(stats.get("PENDING"))
                .resolvedTickets(stats.get("RESOLVED"))
                .reopenedTickets(stats.get("REOPENED"))
                .rejectedTickets(stats.get("REJECTED"))
                .priorityBreakdown(priorityBreakdown)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/daily-ticket-counts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DailyTicketCountResponse>> getDailyTicketCounts() {
        List<DailyTicketCountResponse> counts = ticketService.getDailyTicketCounts();
        return ResponseEntity.ok(counts);
    }
}

