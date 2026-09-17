package com.inventory.msp.dto;

import lombok.Data;

@Data
public class SerialUpdateRequest {
    private String newSerial;
    private String referenceId;
}
