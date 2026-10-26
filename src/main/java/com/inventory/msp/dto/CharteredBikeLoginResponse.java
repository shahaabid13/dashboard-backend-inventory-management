package com.inventory.msp.dto;

import lombok.Data;

@Data
public class CharteredBikeLoginResponse {
    private CharteredBikeLoginData data;
    private int status;
    private String message;
}
