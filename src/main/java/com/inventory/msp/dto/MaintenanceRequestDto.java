package com.inventory.msp.dto;


import com.inventory.msp.model.MaintenanceRequestStatus;
import com.inventory.msp.model.MaintenanceRequestType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class MaintenanceRequestDto {

    private Long id;

    private Long deviceId;
    private String oldSerial;
    private String newSerial;

    private Long newLocationId;
    private Long newApproachRoadId;

    private String newLocationName;      // Human-friendly location name
    private String newApproachRoadName;  // Human-friendly approach road name

    private MaintenanceRequestType requestType;
    private MaintenanceRequestStatus status;

    private String createdBy;
    private String approvedBy;
    private String referenceId;
    private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
