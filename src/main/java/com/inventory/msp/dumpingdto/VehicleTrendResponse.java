package com.inventory.msp.dumpingdto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VehicleTrendResponse {
    private String vno;
    private Integer totalNetWeight;
}
