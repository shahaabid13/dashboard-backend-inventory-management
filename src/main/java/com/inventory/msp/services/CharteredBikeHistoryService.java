package com.inventory.msp.services;

import com.inventory.msp.dto.CharteredBikeStationHistoryDto;
import com.inventory.msp.dto.CharteredBikeStationStatsDto;
import com.inventory.msp.model.CharteredBikeStationSnapshot;
import com.inventory.msp.repository.CharteredBikeStationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CharteredBikeHistoryService {

    private final CharteredBikeStationRepository repository;

    /**
     * Get historical data for a specific station within date range
     */
    public List<CharteredBikeStationHistoryDto> getStationHistory(String stationName, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching history for station: {} from {} to {}", stationName, startDate, endDate);

        List<CharteredBikeStationSnapshot> snapshots = repository
            .findByStationNameAndCapturedAtBetweenOrderByCapturedAtDesc(stationName, startDate, endDate);

        return snapshots.stream()
            .map(this::convertToHistoryDto)
            .collect(Collectors.toList());
    }

    /**
     * Get historical data for all stations within date range
     */
    public List<CharteredBikeStationHistoryDto> getAllStationsHistory(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching history for all stations from {} to {}", startDate, endDate);

        List<CharteredBikeStationSnapshot> snapshots = repository
            .findByCapturedAtBetweenOrderByCapturedAtDesc(startDate, endDate);

        return snapshots.stream()
            .map(this::convertToHistoryDto)
            .collect(Collectors.toList());
    }

    /**
     * Get latest snapshots for all stations
     */
    public List<CharteredBikeStationHistoryDto> getLatestStationsData() {
        log.info("Fetching latest data for all stations");

        List<CharteredBikeStationSnapshot> snapshots = repository.findLatestSnapshotsForAllStations();

        return snapshots.stream()
            .map(this::convertToHistoryDto)
            .collect(Collectors.toList());
    }

    /**
     * Get station statistics for a date range
     */
    public CharteredBikeStationStatsDto getStationStats(String stationName, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Calculating stats for station: {} from {} to {}", stationName, startDate, endDate);

        List<CharteredBikeStationSnapshot> snapshots = repository
            .findByStationNameAndCapturedAtBetweenOrderByCapturedAtDesc(stationName, startDate, endDate);

        if (snapshots.isEmpty()) {
            CharteredBikeStationStatsDto emptyStats = new CharteredBikeStationStatsDto();
            emptyStats.setStationName(stationName);
            emptyStats.setTotalSnapshots(0);
            emptyStats.setAverageBikesAvailable(0.0);
            emptyStats.setMinBikesAvailable(0);
            emptyStats.setMaxBikesAvailable(0);
            emptyStats.setLatestBikesAvailable(0);
            emptyStats.setLastUpdated("No data");
            return emptyStats;
        }

        CharteredBikeStationStatsDto stats = new CharteredBikeStationStatsDto();
        stats.setStationName(stationName);
        stats.setTotalSnapshots(snapshots.size());

        int minBikes = snapshots.stream().mapToInt(CharteredBikeStationSnapshot::getBikesAvailable).min().orElse(0);
        int maxBikes = snapshots.stream().mapToInt(CharteredBikeStationSnapshot::getBikesAvailable).max().orElse(0);
        double avgBikes = snapshots.stream().mapToInt(CharteredBikeStationSnapshot::getBikesAvailable).average().orElse(0.0);

        stats.setMinBikesAvailable(minBikes);
        stats.setMaxBikesAvailable(maxBikes);
        stats.setAverageBikesAvailable(avgBikes);

        CharteredBikeStationSnapshot latest = snapshots.get(0); // Already ordered by date desc
        stats.setLatestBikesAvailable(latest.getBikesAvailable());
        stats.setLastUpdated(latest.getCapturedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return stats;
    }

    /**
     * Get list of all available station names
     */
    public List<String> getAvailableStations() {
        return repository.findDistinctStationNames();
    }

    /**
     * Get recent snapshots for a station (last N days)
     */
    public List<CharteredBikeStationHistoryDto> getRecentStationHistory(String stationName, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<CharteredBikeStationSnapshot> snapshots = repository.findRecentSnapshots(stationName, since);

        return snapshots.stream()
            .map(this::convertToHistoryDto)
            .collect(Collectors.toList());
    }

    private CharteredBikeStationHistoryDto convertToHistoryDto(CharteredBikeStationSnapshot snapshot) {
        CharteredBikeStationHistoryDto dto = new CharteredBikeStationHistoryDto();
        dto.setStationName(snapshot.getStationName());
        dto.setStationNumber(snapshot.getStationNumber());
        dto.setLatitude(snapshot.getLatitude());
        dto.setLongitude(snapshot.getLongitude());
        dto.setActive(snapshot.isActive());
        dto.setBikesAvailable(snapshot.getBikesAvailable());
        dto.setBikesTotal(snapshot.getBikesTotal());
        dto.setBikesFree(snapshot.getBikesFree());
        dto.setReportActiveBikes(snapshot.getReportActiveBikes());
        dto.setReportOnTripBikes(snapshot.getReportOnTripBikes());
        dto.setStolenBikes(snapshot.getStolenBikes());
        dto.setMissingBikes(snapshot.getMissingBikes());
        dto.setCityName(snapshot.getCityName());
        dto.setCapturedAt(snapshot.getCapturedAt());
        return dto;
    }
}
