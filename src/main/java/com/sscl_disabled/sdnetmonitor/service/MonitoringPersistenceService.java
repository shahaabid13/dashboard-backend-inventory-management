package com.sscl.sdnetmonitor.service;

import com.sscl.sdnetmonitor.entity.*;
import com.sscl.sdnetmonitor.repository.DeviceRepository;
import com.sscl.sdnetmonitor.repository.DeviceStatusEventRepository;
import com.sscl.sdnetmonitor.repository.FibreLinkRepository;
import com.sscl.sdnetmonitor.repository.FibreLinkStatusEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Split out from MonitoringScheduler deliberately: Spring's @Transactional
 * is proxy-based, and a method calling another @Transactional method on
 * *itself* (this.otherMethod()) bypasses the proxy entirely, so the
 * transaction boundary would silently be ignored. Putting the persistence
 * step on its own bean means MonitoringScheduler calls through the proxy
 * and each batch genuinely runs as one transaction.
 */
@Service
@RequiredArgsConstructor
public class MonitoringPersistenceService {

    private final DeviceRepository deviceRepository;
    private final DeviceStatusEventRepository deviceStatusEventRepository;
    private final FibreLinkRepository fibreLinkRepository;
    private final FibreLinkStatusEventRepository fibreLinkStatusEventRepository;

    @Transactional(transactionManager = "sdnetMonitorTransactionManager")
    public int applyDeviceResults(Map<Long, DeviceStatus> results) {
        Instant now = Instant.now();
        int changedCount = 0;
        List<Device> devices = deviceRepository.findAllById(results.keySet());

        for (Device device : devices) {
            DeviceStatus newStatus = results.get(device.getId());
            if (newStatus == null) continue;

            DeviceStatus previous = device.getCurrentStatus();
            device.setLastCheckedAt(now);
            if (newStatus == DeviceStatus.UP) {
                device.setLastUpAt(now);
            }

            if (previous != newStatus) {
                DeviceStatusEvent event = new DeviceStatusEvent();
                event.setDevice(device);
                event.setPreviousStatus(previous);
                event.setNewStatus(newStatus);
                event.setChangedAt(now);
                event.setSource(EventSource.PING);
                deviceStatusEventRepository.save(event);

                device.setCurrentStatus(newStatus);
                device.setLastStatusChange(now);
                changedCount++;
            }
            deviceRepository.save(device);
        }
        return changedCount;
    }

    @Transactional(transactionManager = "sdnetMonitorTransactionManager")
    public int recomputeLinkStatuses() {
        Instant now = Instant.now();
        int changedCount = 0;
        List<FibreLink> links = fibreLinkRepository.findAll();

        for (FibreLink link : links) {
            DeviceStatus derived = deriveLinkStatus(link);
            if (derived == DeviceStatus.UP) {
                link.setLastUpAt(now);
            }
            if (derived != link.getCurrentStatus()) {
                FibreLinkStatusEvent event = new FibreLinkStatusEvent();
                event.setFibreLink(link);
                event.setPreviousStatus(link.getCurrentStatus());
                event.setNewStatus(derived);
                event.setChangedAt(now);
                event.setSource(EventSource.DERIVED_PING);
                fibreLinkStatusEventRepository.save(event);

                link.setCurrentStatus(derived);
                link.setLastStatusChange(now);
                changedCount++;
            }
            fibreLinkRepository.save(link);
        }
        return changedCount;
    }

    /**
     * A link is UP only if both endpoint junctions have a reachable switch,
     * DOWN if either switch is confirmed unreachable, and UNKNOWN otherwise
     * (missing endpoint, no switch on record, or a switch not yet checked).
     * DOWN outranks UNKNOWN deliberately: a confirmed-unreachable switch is
     * a stronger signal than "no data yet" on the other end.
     */
    private DeviceStatus deriveLinkStatus(FibreLink link) {
        if (link.getFromJunction() == null || link.getToJunction() == null) {
            return DeviceStatus.UNKNOWN;
        }
        DeviceStatus fromStatus = switchStatus(link.getFromJunction().getId());
        DeviceStatus toStatus = switchStatus(link.getToJunction().getId());

        if (fromStatus == DeviceStatus.DOWN || toStatus == DeviceStatus.DOWN) {
            return DeviceStatus.DOWN;
        }
        if (fromStatus == DeviceStatus.UP && toStatus == DeviceStatus.UP) {
            return DeviceStatus.UP;
        }
        return DeviceStatus.UNKNOWN;
    }

    private DeviceStatus switchStatus(String junctionId) {
        return deviceRepository.findFirstByJunctionIdAndNetworkSwitchTrue(junctionId)
                .map(Device::getCurrentStatus)
                .orElse(DeviceStatus.UNKNOWN);
    }
}
