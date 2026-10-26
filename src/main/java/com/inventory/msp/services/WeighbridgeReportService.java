package com.inventory.msp.services;

import com.inventory.msp.dumpingdto.SummaryResponse;
import com.inventory.msp.dumpingdto.TrendResponse;
import com.inventory.msp.dumpingdto.VehicleTrendResponse;
import com.inventory.msp.model.WeighBridgeEntry;
import com.inventory.msp.repository.WeighBridgeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WeighbridgeReportService {

    @Autowired
    private WeighBridgeRepository repo;

    // split "2025-11-21T12:34:12" into date & time
    private String extractDate(String dt) {
        return dt.split("T")[0];
    }

    private String extractTime(String dt) {
        return dt.split("T")[1];
    }

    // 1. Net weight trend over time
    public List<TrendResponse> getNetWeightTrend(String wbId) {
        List<Object[]> rows = repo.getNetWeightTrend(wbId);

        return rows.stream()
                .map(r -> new TrendResponse(
                        extractDate(r[0].toString()),
                        extractTime(r[0].toString()),
                        Integer.parseInt(r[1].toString())
                ))
                .toList();
    }

    // 2. Gross weight trend over time
    public List<TrendResponse> getGrossWeightTrend(String wbId) {
        List<Object[]> rows = repo.getGrossWeightTrend(wbId);

        return rows.stream()
                .map(r -> new TrendResponse(
                        extractDate(r[0].toString()),
                        extractTime(r[0].toString()),
                        Integer.parseInt(r[1].toString())
                ))
                .toList();
    }

    // 3. Net weight by vehicle
    public List<VehicleTrendResponse> getNetWeightByVehicle(String wbId) {
        List<Object[]> rows = repo.getNetWeightByVehicle(wbId);

        return rows.stream()
                .map(r -> new VehicleTrendResponse(
                        r[0].toString(),
                        Integer.parseInt(r[1].toString())
                ))
                .toList();
    }

    // 4. Last 24 hours
    public List<TrendResponse> getLast24Hours(String wbId) {
        String start = LocalDateTime.now().minusHours(24).toString();

        List<WeighBridgeEntry> rows = repo.getLast24Hours(start, wbId);

        return rows.stream()
                .map(e ->
                        new TrendResponse(
                                extractDate(e.getEdate()),
                                extractTime(e.getEdate()),
                                e.getNweight()
                        ))
                .toList();
    }

    // Summary for any period
    public SummaryResponse getSummary(List<WeighBridgeEntry> entries) {

        long totalVehicles = entries.stream().map(e -> e.getVno()).distinct().count();

        long totalTrips = entries.size();

        long totalGrossWeight = entries.stream().mapToLong(e -> e.getGweight()).sum();

        long totalNetWeight = entries.stream().mapToLong(e -> e.getNweight()).sum();

        return new SummaryResponse(totalVehicles, totalTrips, totalGrossWeight, totalNetWeight);
    }

    // 5. Range-based report
    public SummaryResponse getRangeSummary(String start, String end, String wbId) {
        List<WeighBridgeEntry> rows = repo.getByDateRange(start, end, wbId);
        return getSummary(rows);
    }

    // Daily summary
    public SummaryResponse getDailySummary(String date, String wbId) {
        String start = date + "T00:00:00";
        String end = date + "T23:59:59";
        List<WeighBridgeEntry> entries = repo.getByDateRange(start, end, wbId);
        return getSummary(entries);
    }

    // Timeframe data
    public List<WeighBridgeEntry> getTimeframeData(String start, String end, String wbId) {
        return repo.getByDateRange(start, end, wbId);
    }

    public List<WeighBridgeEntry> getAllDataByWbId(String wbId) {
        return repo.findById_WbId(wbId);
    }
}
