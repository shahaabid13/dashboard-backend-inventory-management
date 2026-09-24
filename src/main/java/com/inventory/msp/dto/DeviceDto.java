package com.inventory.msp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.inventory.msp.model.DeviceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.ALWAYS)  // ← ADD THIS — force ALL fields in JSON
public class DeviceDto {

    private Long id;
    @JsonProperty("serialNumber")
    @JsonAlias({"serial","deviceSerial","serial_no","device_serial","serialNumber"})
    private String serialNumber;
    private DeviceType deviceType;
    private Boolean poles;
    private Boolean ecbPresent;
    private Boolean placeholder;

    @JsonProperty("notified")
    private Boolean notified;   // ← remove = true default, let toDto() control it

    private String latitude;
    private String longitude;
    private String status;
    private String locationName;
    private String approachRoad;
}