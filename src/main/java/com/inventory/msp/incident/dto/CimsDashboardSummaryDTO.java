package com.inventory.msp.incident.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CimsDashboardSummaryDTO {
    long totalInQueue;
    long flaggedCount;
    double avgAgeDays;
    long oldestAgeDays;
    long tasksPending;
    long tasksOverdue;
    long tasksCompletedThisWeek;
}
