package com.inventory.msp.controller;

import com.inventory.msp.dto.CharteredBikeStation;
import com.inventory.msp.dto.CharteredBikeStationCompany;
import com.inventory.msp.dto.CharteredBikeStationResponse;
import com.inventory.msp.dto.CharteredBikeStationHistoryDto;
import com.inventory.msp.dto.CharteredBikeStationStatsDto;
import com.inventory.msp.services.CharteredBikeApiService;
import com.inventory.msp.services.CharteredBikeHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/chartered-bike")
@RequiredArgsConstructor
public class CharteredBikeController {

    private final CharteredBikeApiService apiService;
    private final CharteredBikeHistoryService historyService;

    // ==================== REAL-TIME ENDPOINTS ====================

    @GetMapping("/stations")
    public CharteredBikeStationResponse getStations() {
        return apiService.getStations();
    }

    @GetMapping("/stations/filtered")
    public CharteredBikeStationResponse getStationsFiltered(@RequestParam(defaultValue = "0") int minBikes) {
        CharteredBikeStationResponse response = apiService.getStations();

        // Filter stations that have at least minBikes available
        if (response != null && response.getData() != null) {
            for (CharteredBikeStationCompany company : response.getData()) {
                if (company.getMapStationDTOs() != null) {
                    company.getMapStationDTOs().removeIf(station -> station.getBikesAvailable() < minBikes);
                }
            }
        }

        return response;
    }

    // ==================== HISTORICAL ENDPOINTS ====================

    @GetMapping("/history/stations")
    public List<String> getAvailableStations() {
        return historyService.getAvailableStations();
    }

    @GetMapping("/history/{stationName}")
    public List<CharteredBikeStationHistoryDto> getStationHistory(
            @PathVariable String stationName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return historyService.getStationHistory(stationName, start, end);
    }

    @GetMapping("/history/all")
    public List<CharteredBikeStationHistoryDto> getAllStationsHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return historyService.getAllStationsHistory(start, end);
    }

    @GetMapping("/history/{stationName}/stats")
    public CharteredBikeStationStatsDto getStationStats(
            @PathVariable String stationName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return historyService.getStationStats(stationName, start, end);
    }

    @GetMapping("/history/{stationName}/recent")
    public List<CharteredBikeStationHistoryDto> getRecentStationHistory(
            @PathVariable String stationName,
            @RequestParam(defaultValue = "7") int days) {
        return historyService.getRecentStationHistory(stationName, days);
    }

    @GetMapping("/latest")
    public List<CharteredBikeStationHistoryDto> getLatestStationsData() {
        return historyService.getLatestStationsData();
    }

    // ==================== REPORT ENDPOINTS ====================

    @GetMapping("/reports/last-week")
    public List<CharteredBikeStationHistoryDto> getLastWeekReport() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime now = LocalDateTime.now();
        return historyService.getAllStationsHistory(oneWeekAgo, now);
    }

    @GetMapping("/reports/last-month")
    public List<CharteredBikeStationHistoryDto> getLastMonthReport() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime now = LocalDateTime.now();
        return historyService.getAllStationsHistory(oneMonthAgo, now);
    }

    // ==================== UTILITY ENDPOINTS ====================

    // Manual trigger to fetch and save current data
    @PostMapping("/sync")
    public String syncData() {
        apiService.getStations();
        return "Data synchronization completed";
    }

    // Usage example in code (not exposed as endpoint)
    public void printStations() {
        CharteredBikeStationResponse response = apiService.getStations();
        for (CharteredBikeStationCompany company : response.getData()) {
            for (CharteredBikeStation station : company.getMapStationDTOs()) {
                System.out.println(station.getStationName() + " - Available: " + station.getBikesAvailable() + ", On Trip: " + station.getReportOnTripBikes());
            }
        }
    }
}
