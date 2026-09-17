package com.inventory.msp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "chartered_bike_stations")
@Data
public class CharteredBikeStationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_name", nullable = false)
    private String stationName;

    @Column(name = "station_number", nullable = false)
    private int stationNumber;

    @Column(name = "latitude")
    private String latitude;

    @Column(name = "longitude")
    private String longitude;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "bikes_available")
    private int bikesAvailable;

    @Column(name = "bikes_total")
    private int bikesTotal;

    @Column(name = "bikes_rack")
    private int bikesRack;

    @Column(name = "bikes_free")
    private int bikesFree;

    @Column(name = "ebikes_available")
    private int ebikesAvailable;

    @Column(name = "report_active_bikes")
    private int reportActiveBikes;

    @Column(name = "report_inactive_bikes")
    private int reportInactiveBikes;

    @Column(name = "report_on_trip_bikes")
    private int reportOnTripBikes;

    @Column(name = "stolen_bikes")
    private int stolenBikes;

    @Column(name = "missing_bikes")
    private int missingBikes;

    @ElementCollection
    @CollectionTable(name = "chartered_bike_numbers", joinColumns = @JoinColumn(name = "station_snapshot_id"))
    @Column(name = "bike_number")
    private List<Integer> bikeNumberList;

    @ElementCollection
    @CollectionTable(name = "chartered_eco_bike_numbers", joinColumns = @JoinColumn(name = "station_snapshot_id"))
    @Column(name = "eco_bike_number")
    private List<Integer> ecoBikeNumberList;

    @ElementCollection
    @CollectionTable(name = "chartered_ebike_numbers", joinColumns = @JoinColumn(name = "station_snapshot_id"))
    @Column(name = "ebike_number")
    private List<Integer> ebikeNumberList;

    @Column(name = "city_name")
    private String cityName;

    @Column(name = "city_id")
    private int cityId;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @PrePersist
    protected void onCreate() {
        capturedAt = LocalDateTime.now();
    }
}
