package com.inventory.msp.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CharteredBikeStationHistoryDto {
    private String stationName;
    private int stationNumber;
    private String latitude;
    private String longitude;
    private boolean active;
    private int bikesAvailable;
    private int bikesTotal;
    private int bikesFree;
    private int reportActiveBikes;
    private int reportOnTripBikes;
    private int stolenBikes;
    private int missingBikes;
    private String cityName;
    private LocalDateTime capturedAt;
}
