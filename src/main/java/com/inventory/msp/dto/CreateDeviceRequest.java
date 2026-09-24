package com.inventory.msp.dto;


import com.inventory.msp.model.DeviceType;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateDeviceRequest {

    @JsonAlias({"serial","deviceSerial","serial_no","device_serial","serialNumber"})
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
