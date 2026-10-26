package com.sscl.sdnetmonitor.dto;

/**
 * Aggregate SLA figures for one device category over a [from, to] window --
 * built by summing UptimeCalculationService.forDevice(...) across every
 * device in the category. See SlaReportService for the from/to context.
 */
public record CategorySlaDto(
        String category,
        int deviceCount,
        long totalUpSeconds,
        long totalDownSeconds,
        long totalUnknownSeconds,
        double uptimePercent,
        int currentlyDownCount
) {}
