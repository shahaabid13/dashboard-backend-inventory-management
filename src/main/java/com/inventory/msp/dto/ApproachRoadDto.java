package com.inventory.msp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ApproachRoadDto {
    private Long id;
    private String name;
    private Long locationId;
    private String locationName;
}
