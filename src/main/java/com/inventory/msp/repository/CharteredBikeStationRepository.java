package com.inventory.msp.repository;

import com.inventory.msp.model.CharteredBikeStationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CharteredBikeStationRepository extends JpaRepository<CharteredBikeStationSnapshot, Long> {

    // Find all snapshots for a specific station within date range
    List<CharteredBikeStationSnapshot> findByStationNameAndCapturedAtBetweenOrderByCapturedAtDesc(
        String stationName, LocalDateTime startDate, LocalDateTime endDate);

    // Find latest snapshot for each station
    @Query("SELECT s FROM CharteredBikeStationSnapshot s WHERE s.capturedAt = " +
           "(SELECT MAX(s2.capturedAt) FROM CharteredBikeStationSnapshot s2 WHERE s2.stationName = s.stationName)")
    List<CharteredBikeStationSnapshot> findLatestSnapshotsForAllStations();

    // Find snapshots for a specific station (latest first)
    List<CharteredBikeStationSnapshot> findByStationNameOrderByCapturedAtDesc(String stationName);

    // Find all snapshots within date range
    List<CharteredBikeStationSnapshot> findByCapturedAtBetweenOrderByCapturedAtDesc(
        LocalDateTime startDate, LocalDateTime endDate);

    // Get average bikes available for a station over time period
    @Query("SELECT AVG(s.bikesAvailable) FROM CharteredBikeStationSnapshot s " +
           "WHERE s.stationName = :stationName AND s.capturedAt BETWEEN :startDate AND :endDate")
    Double getAverageBikesAvailable(@Param("stationName") String stationName,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    // Get station names that have data
    @Query("SELECT DISTINCT s.stationName FROM CharteredBikeStationSnapshot s ORDER BY s.stationName")
    List<String> findDistinctStationNames();

    // Count total snapshots for a station
    long countByStationName(String stationName);

    // Find snapshots from last N days for a station
    @Query("SELECT s FROM CharteredBikeStationSnapshot s WHERE s.stationName = :stationName " +
           "AND s.capturedAt >= :since ORDER BY s.capturedAt DESC")
    List<CharteredBikeStationSnapshot> findRecentSnapshots(@Param("stationName") String stationName,
                                                          @Param("since") LocalDateTime since);
}
