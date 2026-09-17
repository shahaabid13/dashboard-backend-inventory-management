package com.inventory.msp.dto;

import lombok.Data;
import java.util.List;

@Data
public class CharteredBikeStation {
    private String stationName;
    private int stationNumber;
    private String latitude;
    private String longitude;
    private boolean active;
    private int bikesAvailable;
    private int bikesTotal;
    private int bikesRack;
    private int bikesFree;
    private int ebikesAvailable;
    private int reportActiveBikes;
    private int reportInactiveBikes;
    private int reportOnTripBikes;
    private int stolenBikes;
    private int missingBikes;
    private List<Integer> bikeNumberList;
    private List<Integer> ecoBikeNumberList;
    private List<Integer> ebikeNumberList;
    private String cityName;
    private int cityId;
}
