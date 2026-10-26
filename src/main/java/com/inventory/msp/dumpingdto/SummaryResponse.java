package com.inventory.msp.dumpingdto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SummaryResponse {
    private long totalVehicles;
    private long totalTrips;
    private long totalGrossWeight;
    private long totalNetWeight;
}
