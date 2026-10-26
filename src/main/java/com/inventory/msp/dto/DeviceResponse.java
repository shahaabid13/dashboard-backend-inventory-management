package com.inventory.msp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventory.msp.model.DeviceType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DeviceResponse {
    private Long id;
    @JsonProperty("serialNumber")
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
