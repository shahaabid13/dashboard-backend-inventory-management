package com.inventory.msp.dto;

import com.inventory.msp.model.MaintenanceRequestType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateMaintenanceRequestRequest {

    private String deviceSerial;          // device being acted upon

    private String oldSerial;       // auto-filled by controller
    private String newSerial;       // required for SERIAL_UPDATE or REPLACE

    private String newLocationName;      // ✅ new
    private String newApproachRoadName; // for MOVE

    private MaintenanceRequestType requestType;

    private String createdBy;       // auto-filled from JWT
    private String referenceId;     // external or EMS id
    private String remarks;
}
