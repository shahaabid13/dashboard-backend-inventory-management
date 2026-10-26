package com.sscl.sdnetmonitor.dto;

import java.time.Instant;

public record StatusEventDto(
        Long id,
        String entityId,      // device id (as string) or fibre link id
        String entityLabel,   // device label or fibre link display name, for convenience
        String previousStatus,
        String newStatus,
        Instant changedAt,
        String source,
        String note
) {}
