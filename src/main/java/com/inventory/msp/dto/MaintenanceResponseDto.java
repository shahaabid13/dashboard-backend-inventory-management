package com.inventory.msp.dto;

import com.inventory.msp.model.MaintenanceRequestStatus;
import com.inventory.msp.model.MaintenanceRequestType;
import com.inventory.msp.model.DeviceType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class MaintenanceResponseDto {

    private Long id;

    private Long deviceId;
    private String oldSerial;
    private String newSerial;
    private String currentSerial;




    private MaintenanceRequestType requestType;
    private MaintenanceRequestStatus status;

    private String createdBy;
    private String approvedBy;
    private String referenceId;
    private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // -------------------------
    // EXTRA FIELDS (Your need)
    // -------------------------
    private DeviceType deviceType;

    private String newLocationName;      // Human-friendly location name
    private String newApproachRoadName;  // Human-friendly approach road name


    private String locationName;


    private String approachRoadName;
}
