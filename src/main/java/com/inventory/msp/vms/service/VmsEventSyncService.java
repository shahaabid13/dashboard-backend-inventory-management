package com.inventory.msp.vms.service;

import com.inventory.msp.vms.dto.request.EventSearchFilterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.inventory.msp.vms.config.VmsTmsProperties;

@Service
@Slf4j
@RequiredArgsConstructor
public class VmsEventSyncService {

    private final EventSearchService eventSearchService;
    private final VmsTmsProperties properties;
    private final VmsStatusService statusService;

    /**
     * Runs at a fixed rate defined by vms-tms.event-sync-interval-ms (default 60000ms).
     * The job polls events in the window [now - eventSyncWindowMs, now] and requests persistence.
     */
    @Scheduled(fixedRateString = "${vms-tms.event-sync-interval-ms:60000}")
    public void scheduledEventSync() {
        log.debug("Running scheduled VMS event sync");

        long now = System.currentTimeMillis();
        long windowMs = properties.getEventSyncWindowMs();
        long start = Math.max(0, now - windowMs);
        long end = now;

        EventSearchFilterRequest request = EventSearchFilterRequest.builder()
                .starttimestamp(start)
                .endtimestamp(end)
                .persist(true)
                .limit(100)
                .page(1)
                .build();

        int attempts = Math.max(1, properties.getEventSyncRetries());
        long baseDelay = Math.max(100L, properties.getEventSyncRetryBaseMs());

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                log.debug("Event sync attempt {}/{} for window {}-{}", attempt, attempts, start, end);
                eventSearchService.searchEvents(request);
                // record success for default server
                Integer sid = properties.getDefaultServerId();
                statusService.recordSuccess(sid);
                return;
            } catch (Exception e) {
                Integer sid = properties.getDefaultServerId();
                statusService.recordFailure(sid);
                log.warn("Event sync attempt {}/{} failed: {}", attempt, attempts, e.getMessage());
                if (attempt == attempts) {
                    log.error("Event sync ultimately failed after {} attempts", attempts, e);
                    return;
                }
                try {
                    long backoff = baseDelay * (1L << (attempt - 1));
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
