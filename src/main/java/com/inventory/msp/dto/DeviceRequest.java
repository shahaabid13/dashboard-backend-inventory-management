package com.inventory.msp.dto;

import com.inventory.msp.model.DeviceType;
import com.inventory.msp.model.JunctionBoxType;
import lombok.Data;

@Data

public class DeviceRequest {
    private String serialNumber;
    private DeviceType deviceType;
    private Boolean ecbPresent;
    private Boolean poles;
    private String latitude;
    private String longitude;
    private String locationName;    //location name where device is to be installed
    private String approachRoadName;  //particular approach road where device is to be installed
    private String status;
    private JunctionBoxType junctionBoxType;
    private boolean placeholder;
    private Boolean notified;



}
