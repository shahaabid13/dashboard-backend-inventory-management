package com.sscl.sdnetmonitor.service;

import com.sscl.sdnetmonitor.dto.UptimeSummaryDto;
import com.sscl.sdnetmonitor.entity.Device;
import com.sscl.sdnetmonitor.entity.DeviceStatus;
import com.sscl.sdnetmonitor.entity.DeviceStatusEvent;
import com.sscl.sdnetmonitor.entity.FibreLink;
import com.sscl.sdnetmonitor.entity.FibreLinkStatusEvent;
import com.sscl.sdnetmonitor.repository.DeviceRepository;
import com.sscl.sdnetmonitor.repository.DeviceStatusEventRepository;
import com.sscl.sdnetmonitor.repository.FibreLinkRepository;
import com.sscl.sdnetmonitor.repository.FibreLinkStatusEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Uptime/downtime is always derived from the event log for the requested
 * window, never read from a stored running total. This is deliberate: it
 * means the figure is correct for *any* [from, to] window someone asks for
 * later, including windows that predate when this service was written, and
 * it matches exactly how Zabbix computes SLA/availability from
 * PROBLEM/RESOLVED event pairs -- the same approach recommended for the
 * fibre-link monitoring architecture this project extends.
 */
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "sdnetMonitorTransactionManager", readOnly = true)
public class UptimeCalculationService {

    private final DeviceRepository deviceRepository;
    private final DeviceStatusEventRepository deviceStatusEventRepository;
    private final FibreLinkRepository fibreLinkRepository;
    private final FibreLinkStatusEventRepository fibreLinkStatusEventRepository;

    public UptimeSummaryDto forDevice(Long deviceId, Instant from, Instant to) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NoSuchElementException("Device not found: " + deviceId));

        List<DeviceStatusEvent> events =
                deviceStatusEventRepository.findByDeviceIdAndChangedAtBetweenOrderByChangedAtAsc(deviceId, from, to);

        DeviceStatus anchorStatus = events.isEmpty() ? device.getCurrentStatus() : events.get(0).getPreviousStatus();
        if (anchorStatus == null) anchorStatus = DeviceStatus.UNKNOWN;

        long[] millisByStatus = computeMillisByStatus(
                anchorStatus,
                events.stream().map(DeviceStatusEvent::getChangedAt).toList(),
                events.stream().map(DeviceStatusEvent::getNewStatus).toList(),
                from, to);

        long up = millisByStatus[DeviceStatus.UP.ordinal()];
        long down = millisByStatus[DeviceStatus.DOWN.ordinal()];
        long unknown = millisByStatus[DeviceStatus.UNKNOWN.ordinal()];
        long total = up + down + unknown;
        double pct = total > 0 ? (100.0 * up / total) : 0.0;

        return new UptimeSummaryDto(
                String.valueOf(deviceId), device.getDeviceLabel(), from, to,
                up, down, unknown, pct, events.size());
    }

    public UptimeSummaryDto forFibreLink(String linkId, Instant from, Instant to) {
        FibreLink link = fibreLinkRepository.findById(linkId)
                .orElseThrow(() -> new NoSuchElementException("Fibre link not found: " + linkId));

        List<FibreLinkStatusEvent> events =
                fibreLinkStatusEventRepository.findByFibreLinkIdAndChangedAtBetweenOrderByChangedAtAsc(linkId, from, to);

        DeviceStatus anchorStatus = events.isEmpty() ? link.getCurrentStatus() : events.get(0).getPreviousStatus();
        if (anchorStatus == null) anchorStatus = DeviceStatus.UNKNOWN;

        long[] millisByStatus = computeMillisByStatus(
                anchorStatus,
                events.stream().map(FibreLinkStatusEvent::getChangedAt).toList(),
                events.stream().map(FibreLinkStatusEvent::getNewStatus).toList(),
                from, to);

        long up = millisByStatus[DeviceStatus.UP.ordinal()];
        long down = millisByStatus[DeviceStatus.DOWN.ordinal()];
        long unknown = millisByStatus[DeviceStatus.UNKNOWN.ordinal()];
        long total = up + down + unknown;
        double pct = total > 0 ? (100.0 * up / total) : 0.0;

        return new UptimeSummaryDto(
                link.getId(), link.getDisplayName() != null ? link.getDisplayName() : link.getId(),
                from, to, up, down, unknown, pct, events.size());
    }

    /**
     * The actual up/down/unknown replay, factored out so SlaReportService
     * can reuse this exact logic against events it bulk-fetched itself
     * (one query for every device in a category, instead of calling
     * forDevice() -- and paying for a separate query -- once per device).
     * Package-visible, not private: SlaReportService lives in this same
     * package specifically so it can call this directly.
     */
    long[] computeMillisByStatus(DeviceStatus anchorStatus, List<Instant> changedAts,
                                  List<DeviceStatus> newStatuses, Instant from, Instant to) {
        long[] millisByStatus = new long[DeviceStatus.values().length];
        Instant cursor = from;
        DeviceStatus current = anchorStatus;

        for (int i = 0; i < changedAts.size(); i++) {
            Instant changedAt = changedAts.get(i);
            long span = Math.max(0, changedAt.toEpochMilli() - cursor.toEpochMilli());
            millisByStatus[current.ordinal()] += span;
            current = newStatuses.get(i);
            cursor = changedAt;
        }
        millisByStatus[current.ordinal()] += Math.max(0, to.toEpochMilli() - cursor.toEpochMilli());
        return millisByStatus;
    }
}
