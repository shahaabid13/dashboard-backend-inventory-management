package com.sscl.sdnetmonitor.dto;

public record JunctionDto(
        String id,
        String name,
        String type,
        Double latitude,
        Double longitude,
        boolean hasCoordinates,
        String source,
        long deviceCount,
        long linkCount
) {}
