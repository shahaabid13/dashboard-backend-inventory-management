package com.sscl.sdnetmonitor.dto;

import java.time.Instant;

/**
 * Uptime/downtime for one entity (device or fibre link) over [from, to].
 * Always computed on request from the event log -- see UptimeCalculationService.
 */
public record UptimeSummaryDto(
        String entityId,
        String entityLabel,
        Instant from,
        Instant to,
        long upMillis,
        long downMillis,
        long unknownMillis,
        double uptimePercent,
        int stateChangeCount
) {}
