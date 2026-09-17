package com.inventory.msp.dto;

import com.inventory.msp.model.DeviceType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceResponse {
    private Long id;
    private String serialNumber;
    private DeviceType deviceType;
    private Boolean ecbPresent;
    private Boolean poles;
    private String latitude;
    private String longitude;
    private String status;
    private Long locationId;
    private String locationName;
    private Long approachRoadId;
    private String approachRoadName;
    private Boolean placeholder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
