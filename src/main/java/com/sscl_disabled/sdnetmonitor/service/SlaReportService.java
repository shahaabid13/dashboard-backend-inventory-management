package com.sscl.sdnetmonitor.service;

import com.sscl.sdnetmonitor.dto.CategorySlaDto;
import com.sscl.sdnetmonitor.entity.Device;
import com.sscl.sdnetmonitor.entity.DeviceCategory;
import com.sscl.sdnetmonitor.entity.DeviceStatus;
import com.sscl.sdnetmonitor.entity.DeviceStatusEvent;
import com.sscl.sdnetmonitor.repository.DeviceRepository;
import com.sscl.sdnetmonitor.repository.DeviceStatusEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Per-device-category SLA rollup: for each category, sums up/down/unknown
 * time across every device in that category over [from, to].
 *
 * Bulk-fetches every device_status_event in the window ONCE
 * (findByChangedAtBetweenOrderByDeviceIdAscChangedAtAsc), groups by device
 * in memory, then replays each device's own sub-sequence using
 * UptimeCalculationService's shared computeMillisByStatus(). This used to
 * call uptimeCalculationService.forDevice(id, from, to) once per device --
 * fine at the device counts this was first built and tested against, but
 * measured against a real 4-day production dump (1177 devices, 57k+
 * events, sustained flapping from several marginal devices) that was 1177
 * individual queries per report. Grouping .getDevice().getId() on the
 * lazy-loaded association is safe here without triggering per-event loads --
 * Hibernate resolves a @ManyToOne(LAZY) proxy's id from the already-known
 * FK column without initializing the rest of the entity.
 */
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "sdnetMonitorTransactionManager", readOnly = true)
public class SlaReportService {

    private final DeviceRepository deviceRepository;
    private final DeviceStatusEventRepository deviceStatusEventRepository;
    private final UptimeCalculationService uptimeCalculationService;

    /** @param category null means "every category, one row each" */
    public List<CategorySlaDto> generateSummary(Instant from, Instant to, DeviceCategory category) {
        List<Device> devices = deviceRepository.findAll().stream()
                .filter(d -> category == null || d.getCategory() == category)
                .toList();

        List<DeviceStatusEvent> allEvents =
                deviceStatusEventRepository.findByChangedAtBetweenOrderByDeviceIdAscChangedAtAsc(from, to);
        Map<Long, List<DeviceStatusEvent>> eventsByDeviceId = allEvents.stream()
                .collect(Collectors.groupingBy(e -> e.getDevice().getId()));

        Map<DeviceCategory, List<Device>> byCategory = devices.stream()
                .collect(Collectors.groupingBy(Device::getCategory));

        List<CategorySlaDto> result = new ArrayList<>();
        for (Map.Entry<DeviceCategory, List<Device>> entry : byCategory.entrySet()) {
            result.add(summarizeCategory(entry.getKey(), entry.getValue(), eventsByDeviceId, from, to));
        }
        result.sort(Comparator.comparing(CategorySlaDto::category));
        return result;
    }

    private CategorySlaDto summarizeCategory(DeviceCategory category, List<Device> devicesInCategory,
                                              Map<Long, List<DeviceStatusEvent>> eventsByDeviceId,
                                              Instant from, Instant to) {
        long totalUpMs = 0, totalDownMs = 0, totalUnknownMs = 0;
        int currentlyDown = 0;

        for (Device device : devicesInCategory) {
            List<DeviceStatusEvent> events = eventsByDeviceId.getOrDefault(device.getId(), List.of());

            DeviceStatus anchorStatus = events.isEmpty() ? device.getCurrentStatus() : events.get(0).getPreviousStatus();
            if (anchorStatus == null) anchorStatus = DeviceStatus.UNKNOWN;

            long[] millis = uptimeCalculationService.computeMillisByStatus(
                    anchorStatus,
                    events.stream().map(DeviceStatusEvent::getChangedAt).toList(),
                    events.stream().map(DeviceStatusEvent::getNewStatus).toList(),
                    from, to);

            totalUpMs += millis[DeviceStatus.UP.ordinal()];
            totalDownMs += millis[DeviceStatus.DOWN.ordinal()];
            totalUnknownMs += millis[DeviceStatus.UNKNOWN.ordinal()];
            if (device.getCurrentStatus() == DeviceStatus.DOWN) {
                currentlyDown++;
            }
        }

        // Unknown time excluded from the % on purpose -- it's neither uptime
        // nor an SLA-counting outage, just an absence of data, so folding it
        // into either side would misrepresent the actual SLA figure.
        long totalKnownMs = totalUpMs + totalDownMs;
        double uptimePercent = totalKnownMs > 0 ? (100.0 * totalUpMs / totalKnownMs) : 0.0;

        return new CategorySlaDto(
                category.name(),
                devicesInCategory.size(),
                totalUpMs / 1000,
                totalDownMs / 1000,
                totalUnknownMs / 1000,
                Math.round(uptimePercent * 100) / 100.0,
                currentlyDown
        );
    }
}
