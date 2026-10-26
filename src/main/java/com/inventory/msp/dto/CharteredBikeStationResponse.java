package com.inventory.msp.dto;

import lombok.Data;
import java.util.List;

@Data
public class CharteredBikeStationResponse {
    private List<CharteredBikeStationCompany> data;
    private int status;
    private String message;
}
