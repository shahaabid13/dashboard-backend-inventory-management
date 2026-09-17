package com.sscl.sdnetmonitor.dto;

import java.time.Instant;

public record DeviceDto(
        Long id,
        String junctionId,
        String junctionName,
        Double junctionLatitude,
        Double junctionLongitude,
        boolean junctionHasCoordinates,
        String deviceLabel,
        String ipAddress,
        String category,
        boolean networkSwitch,
        String currentStatus,
        Instant lastStatusChange,
        Instant lastCheckedAt,
        Instant lastUpAt,
        boolean snmpEnabled,
        Boolean lldpEnabled
) {}
