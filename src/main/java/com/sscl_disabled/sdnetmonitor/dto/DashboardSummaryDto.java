package com.sscl.sdnetmonitor.dto;

import java.time.Instant;

public record DashboardSummaryDto(
        long junctionCount,
        long routerCount,
        long deviceCount,
        long devicesUp,
        long devicesDown,
        long devicesUnknown,
        long linkCount,
        long linksUp,
        long linksDown,
        long linksUnknown,
        double totalLengthKm,
        Instant lastSweepAt
) {}
