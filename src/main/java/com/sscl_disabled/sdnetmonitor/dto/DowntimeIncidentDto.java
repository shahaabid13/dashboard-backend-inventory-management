package com.sscl.sdnetmonitor.dto;

import java.time.Instant;

/**
 * One outage, start to finish, for either a device or a fibre link.
 *
 * upAt is null when the incident is still open (no recovery event yet) as
 * of when the report was generated -- that's the signal to treat as "still
 * down", not durationSeconds. durationSeconds is always populated: for a
 * closed incident it's the actual outage length; for an open one it's the
 * elapsed time so far, which is genuinely useful on its own ("down for 3h
 * and counting") -- the report renderer just needs to also check upAt to
 * label it correctly.
 */
public record DowntimeIncidentDto(
        String entityType,     // "DEVICE" or "FIBRE_LINK"
        String entityId,
        String entityLabel,
        String category,       // DeviceCategory name for devices, null for fibre links (no category concept)
        Instant downAt,
        Instant upAt,          // null if still down
        Long durationSeconds,  // elapsed outage length, or elapsed-so-far if upAt is null
        String source
) {}
