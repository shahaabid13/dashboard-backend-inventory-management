package com.sscl.sdnetmonitor.service;

import com.sscl.sdnetmonitor.entity.Device;
import com.sscl.sdnetmonitor.entity.DeviceStatus;
import com.sscl.sdnetmonitor.repository.DeviceRepository;
import com.sscl.sdnetmonitor.service.probe.StatusProbe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Runs the full device sweep on a fixed schedule: ping every device
 * concurrently (I/O only, no DB access from inside the probe), then hand the
 * results to MonitoringPersistenceService to persist and to derive fibre-link
 * statuses. Kept as three phases (probe -> persist -> derive) so the
 * parallel I/O never overlaps with a database transaction, and so the
 * transactional work runs through a separate bean (see
 * MonitoringPersistenceService's class comment for why that split matters).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringScheduler {

    private final DeviceRepository deviceRepository;
    private final StatusProbe statusProbe;
    private final ExecutorService pingExecutorService;
    private final MonitoringPersistenceService persistenceService;

    @Value("${sdnet.monitoring.enabled:true}")
    private boolean monitoringEnabled;

    private final AtomicReference<Instant> lastSweepAt = new AtomicReference<>(null);
    private final AtomicReference<Boolean> sweepInProgress = new AtomicReference<>(false);

    public Instant getLastSweepAt() {
        return lastSweepAt.get();
    }

    @Scheduled(fixedRateString = "${sdnet.monitoring.sweep-interval-ms:60000}")
    public void sweep() {
        if (!monitoringEnabled) {
            return;
        }
        if (!sweepInProgress.compareAndSet(false, true)) {
            log.warn("Previous sweep still running -- skipping this tick");
            return;
        }
        try {
            long startedAt = System.currentTimeMillis();
            List<Device> devices = deviceRepository.findAll();

            // Phase 1: ping everything concurrently. Pure I/O, no DB writes.
            List<CompletableFuture<DeviceResult>> futures = devices.stream()
                    .map(d -> CompletableFuture.supplyAsync(
                            () -> new DeviceResult(d.getId(), statusProbe.check(d)), pingExecutorService))
                    .toList();

            // Safety net for a genuinely hung ping subprocess, not a normal-
            // operation budget: real worst case with the current pool size
            // is well under this. Kept short (rather than the old 120s) so
            // a hang doesn't silently swallow a dozen 10s ticks in a row.
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .orTimeout(120, TimeUnit.SECONDS)
                    .join();

            Map<Long, DeviceStatus> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toMap(DeviceResult::deviceId, DeviceResult::status));

            // Phase 2 & 3: persist + derive, each its own real transaction (see MonitoringPersistenceService).
            int changed = persistenceService.applyDeviceResults(results);
            int linksChanged = persistenceService.recomputeLinkStatuses();

            lastSweepAt.set(Instant.now());
            long tookMs = System.currentTimeMillis() - startedAt;
            log.info("Sweep complete: {} devices checked, {} status changes, {} link changes, {} ms",
                    devices.size(), changed, linksChanged, tookMs);

        } catch (Exception e) {
            log.error("Sweep failed: {}", e.getMessage(), e);
        } finally {
            sweepInProgress.set(false);
        }
    }

    private record DeviceResult(Long deviceId, DeviceStatus status) {}
}
