package com.inventory.msp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LocationDto {
    private Long id;
    private String name;
    private String junctionBox;   // SMALL / BIG / NONE
}
