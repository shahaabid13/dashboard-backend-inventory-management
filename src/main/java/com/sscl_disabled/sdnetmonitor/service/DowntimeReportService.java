package com.sscl.sdnetmonitor.service;

import com.sscl.sdnetmonitor.dto.DowntimeIncidentDto;
import com.sscl.sdnetmonitor.entity.DeviceCategory;
import com.sscl.sdnetmonitor.entity.DeviceStatus;
import com.sscl.sdnetmonitor.entity.DeviceStatusEvent;
import com.sscl.sdnetmonitor.entity.FibreLinkStatusEvent;
import com.sscl.sdnetmonitor.repository.DeviceStatusEventRepository;
import com.sscl.sdnetmonitor.repository.FibreLinkStatusEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds a fleet-wide list of downtime incidents (device or fibre link,
 * start time, recovery time, duration) for a [from, to] window -- the data
 * source behind GET /api/reports/downtime and the PDF export.
 *
 * Scope, deliberately: this reports outages whose DOWN transition itself
 * falls inside [from, to]. An outage that started before `from` and was
 * still ongoing when the window opened is not picked up as a "new" incident
 * here (UptimeCalculationService already covers aggregate up/down time for
 * an arbitrary window per-entity, including that edge; this service answers
 * a different question -- "list the individual outages that began in this
 * period" -- which is what an ops/SLA report typically wants). If you need
 * "still-down-from-before" entries included too, that's a straightforward
 * follow-up: seed the walk with each entity's last known event before
 * `from`, same as UptimeCalculationService's anchorStatus does.
 */
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "sdnetMonitorTransactionManager", readOnly = true)
public class DowntimeReportService {

    private final DeviceStatusEventRepository deviceStatusEventRepository;
    private final FibreLinkStatusEventRepository fibreLinkStatusEventRepository;

    /**
     * @param category optional filter. Only applies to device incidents --
     *                  fibre links have no category concept, so a non-null
     *                  category excludes fibre-link incidents from the
     *                  result entirely rather than pretending one applies.
     * @param deviceId optional filter for a single specific device (e.g.
     *                  from that device's own detail page). Combines with
     *                  category if both are given; also excludes fibre-link
     *                  incidents, same reasoning as category.
     */
    public List<DowntimeIncidentDto> generateIncidents(Instant from, Instant to, DeviceCategory category, Long deviceId) {
        List<DowntimeIncidentDto> incidents = new ArrayList<>();
        Instant now = Instant.now();

        List<DeviceStatusEvent> deviceDownEvents = deviceStatusEventRepository
                .findDownEvents(DeviceStatus.DOWN, from, to, category, deviceId);

        for (DeviceStatusEvent downEvent : deviceDownEvents) {
            Instant downAt = downEvent.getChangedAt();
            Instant upAt = deviceStatusEventRepository
                    .findFirstByDeviceIdAndChangedAtAfterOrderByChangedAtAsc(downEvent.getDevice().getId(), downAt)
                    .filter(next -> next.getNewStatus() != DeviceStatus.DOWN)
                    .map(DeviceStatusEvent::getChangedAt)
                    .orElse(null);

            incidents.add(new DowntimeIncidentDto(
                    "DEVICE",
                    String.valueOf(downEvent.getDevice().getId()),
                    downEvent.getDevice().getDeviceLabel(),
                    downEvent.getDevice().getCategory().name(),
                    downAt,
                    upAt,
                    durationSeconds(downAt, upAt, now),
                    downEvent.getSource().name()));
        }

        if (category == null && deviceId == null) {
            List<FibreLinkStatusEvent> linkDownEvents = fibreLinkStatusEventRepository
                    .findByNewStatusAndChangedAtBetweenOrderByFibreLinkIdAscChangedAtAsc(DeviceStatus.DOWN, from, to);

            for (FibreLinkStatusEvent downEvent : linkDownEvents) {
                Instant downAt = downEvent.getChangedAt();
                Instant upAt = fibreLinkStatusEventRepository
                        .findFirstByFibreLinkIdAndChangedAtAfterOrderByChangedAtAsc(downEvent.getFibreLink().getId(), downAt)
                        .filter(next -> next.getNewStatus() != DeviceStatus.DOWN)
                        .map(FibreLinkStatusEvent::getChangedAt)
                        .orElse(null);

                String label = downEvent.getFibreLink().getDisplayName() != null
                        ? downEvent.getFibreLink().getDisplayName()
                        : downEvent.getFibreLink().getId();

                incidents.add(new DowntimeIncidentDto(
                        "FIBRE_LINK",
                        downEvent.getFibreLink().getId(),
                        label,
                        null,
                        downAt,
                        upAt,
                        durationSeconds(downAt, upAt, now),
                        downEvent.getSource().name()));
            }
        }

        incidents.sort(Comparator.comparing(DowntimeIncidentDto::downAt));
        return incidents;
    }

    private Long durationSeconds(Instant downAt, Instant upAt, Instant now) {
        if (upAt != null) {
            return Duration.between(downAt, upAt).getSeconds();
        }
        // Still open: report elapsed-so-far rather than leaving it null,
        // since callers besides the PDF (e.g. a live "currently down" view)
        // may want a number here. The PDF renderer separately checks upAt
        // == null to print "still down" instead of a duration.
        return Duration.between(downAt, now).getSeconds();
    }
}
