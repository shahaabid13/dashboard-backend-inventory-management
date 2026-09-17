package com.inventory.msp.dto;


import com.inventory.msp.model.DeviceType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateDeviceRequest {

    private String serialNumber;
    private DeviceType deviceType;

    private boolean poles;
    private boolean ecbPresent;
    private boolean notified;

    private String latitude;
    private String longitude;

    private Long locationId;
    private Long approachRoadId;
}
