package com.inventory.msp.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CharteredBikeDataScheduler {

    private final CharteredBikeApiService charteredBikeApiService;

    /**
     * Fetch and save Chartered Bike station data every 30 minutes
     * This ensures we have regular snapshots of station status
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30 minutes in milliseconds
    public void fetchAndSaveStationData() {
        try {
            log.info("Starting scheduled Chartered Bike data fetch...");
            charteredBikeApiService.getStations(); // This will fetch and save data
            log.info("✅ Successfully completed scheduled Chartered Bike data fetch");
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("unreachable")) {
                log.warn("⚠️ Chartered Bike API is currently unreachable. Will retry in 30 minutes. Error: {}", e.getMessage());
            } else if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                log.warn("⚠️ Chartered Bike API connection timed out. Will retry in 30 minutes. Error: {}", e.getMessage());
            } else {
                log.error("❌ Failed to fetch Chartered Bike data: {}", e.getMessage());
            }
            log.debug("Full error details:", e);
            // Don't rethrow - we don't want the scheduler to stop
        } catch (Exception e) {
            log.error("❌ Unexpected error during scheduled Chartered Bike data fetch", e);
            // Don't rethrow - we don't want the scheduler to stop
        }
    }

    /**
     * Alternative: Fetch data every hour at the top of the hour
     * Uncomment this and comment the fixedRate method above if preferred
     */
    // @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
    // public void fetchHourlyStationData() {
    //     fetchAndSaveStationData();
    // }

    /**
     * Alternative: Fetch data daily at 6 AM
     * Uncomment this for daily snapshots instead of frequent updates
     */
    // @Scheduled(cron = "0 0 6 * * *") // Daily at 6:00 AM
    // public void fetchDailyStationData() {
    //     fetchAndSaveStationData();
    // }
}
