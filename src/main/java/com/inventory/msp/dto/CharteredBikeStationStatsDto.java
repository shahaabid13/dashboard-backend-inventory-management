package com.inventory.msp.dto;

import lombok.Data;

@Data
public class CharteredBikeStationStatsDto {
    private String stationName;
    private long totalSnapshots;
    private double averageBikesAvailable;
    private int minBikesAvailable;
    private int maxBikesAvailable;
    private int latestBikesAvailable;
    private String lastUpdated;
}
