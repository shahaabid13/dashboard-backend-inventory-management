package com.sscl.sdnetmonitor.service;

import com.sscl.sdnetmonitor.dto.DashboardSummaryDto;
import com.sscl.sdnetmonitor.entity.DeviceStatus;
import com.sscl.sdnetmonitor.entity.JunctionType;
import com.sscl.sdnetmonitor.repository.DeviceRepository;
import com.sscl.sdnetmonitor.repository.FibreLinkRepository;
import com.sscl.sdnetmonitor.repository.JunctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "sdnetMonitorTransactionManager", readOnly = true)
public class DashboardService {

    private final JunctionRepository junctionRepository;
    private final DeviceRepository deviceRepository;
    private final FibreLinkRepository fibreLinkRepository;
    private final MonitoringScheduler monitoringScheduler;

    public DashboardSummaryDto summary() {
        long junctionCount = junctionRepository.findAll().stream()
                .filter(j -> j.getType() == JunctionType.JUNCTION).count();
        long routerCount = junctionRepository.findAll().stream()
                .filter(j -> j.getType() == JunctionType.ROUTER).count();

        long deviceCount = deviceRepository.count();
        long devicesUp = deviceRepository.countByCurrentStatus(DeviceStatus.UP);
        long devicesDown = deviceRepository.countByCurrentStatus(DeviceStatus.DOWN);
        long devicesUnknown = deviceRepository.countByCurrentStatus(DeviceStatus.UNKNOWN);

        long linkCount = fibreLinkRepository.count();
        long linksUp = fibreLinkRepository.countByCurrentStatus(DeviceStatus.UP);
        long linksDown = fibreLinkRepository.countByCurrentStatus(DeviceStatus.DOWN);
        long linksUnknown = fibreLinkRepository.countByCurrentStatus(DeviceStatus.UNKNOWN);

        double totalLengthKm = fibreLinkRepository.findAll().stream()
                .mapToDouble(l -> l.getLengthMeters() != null ? l.getLengthMeters() : 0.0)
                .sum() / 1000.0;

        return new DashboardSummaryDto(
                junctionCount, routerCount,
                deviceCount, devicesUp, devicesDown, devicesUnknown,
                linkCount, linksUp, linksDown, linksUnknown,
                Math.round(totalLengthKm * 10.0) / 10.0,
                monitoringScheduler.getLastSweepAt()
        );
    }
}
