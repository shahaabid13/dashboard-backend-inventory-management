package com.inventory.msp.incident.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentDashboardStatsResponse {

    private Long totalTickets;
    private Long openTickets;
    private Long coordinatorReviewTickets;
    private Long assignedToReviewerTickets;
    private Long pendingTickets;
    private Long resolvedTickets;
    private Long reopenedTickets;
    private Long rejectedTickets;

    // Priority breakdown
    private Map<String, Long> priorityBreakdown;
}

