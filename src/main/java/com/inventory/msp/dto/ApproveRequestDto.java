package com.inventory.msp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ApproveRequestDto {
    private boolean approved;
    private String remarks;
}
