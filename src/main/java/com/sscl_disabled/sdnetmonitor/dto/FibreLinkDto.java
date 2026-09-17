package com.sscl.sdnetmonitor.dto;

import java.time.Instant;
import java.util.List;

public record FibreLinkDto(
        String id,
        String displayName,
        String fromJunctionId,
        String fromJunctionName,
        boolean fromConfident,
        String toJunctionId,
        String toJunctionName,
        boolean toConfident,
        Double lengthMeters,
        boolean confirmed,
        boolean diagramConfirmed,
        String currentStatus,
        Instant lastStatusChange,
        List<List<Double>> path
) {}
