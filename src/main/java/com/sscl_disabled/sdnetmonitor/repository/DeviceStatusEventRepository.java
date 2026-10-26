package com.sscl.sdnetmonitor.repository;

import com.sscl.sdnetmonitor.entity.DeviceCategory;
import com.sscl.sdnetmonitor.entity.DeviceStatus;
import com.sscl.sdnetmonitor.entity.DeviceStatusEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeviceStatusEventRepository extends JpaRepository<DeviceStatusEvent, Long> {

    List<DeviceStatusEvent> findByDeviceIdAndChangedAtBetweenOrderByChangedAtAsc(
            Long deviceId, Instant from, Instant to);

    /** Every event for every device in one window, one query -- the bulk
     *  alternative to calling the single-device version once per device
     *  (which is what SlaReportService used to do: one query per device,
     *  1177 of them for a full-fleet report). Ordered by device then time
     *  so callers can group by device_id and replay each device's own
     *  sub-sequence directly, without a second query per device. */
    List<DeviceStatusEvent> findByChangedAtBetweenOrderByDeviceIdAscChangedAtAsc(Instant from, Instant to);

    List<DeviceStatusEvent> findTop50ByDeviceIdOrderByChangedAtDesc(Long deviceId);

    List<DeviceStatusEvent> findTop100ByOrderByChangedAtDesc();

    /** Every "went DOWN" transition in the window, across all devices -- the
     *  starting point for the downtime report (see DowntimeReportService).
     *  category/deviceId are optional (null = no filter on that field) and
     *  applied here in SQL rather than fetched-then-filtered in Java -- the
     *  previous version fetched every fleet-wide DOWN event even for a
     *  single device's own detail page, which got genuinely expensive once
     *  incident volume grew large. */
    @Query("""
            select e from DeviceStatusEvent e
            where e.newStatus = :newStatus
              and e.changedAt between :from and :to
              and (:category is null or e.device.category = :category)
              and (:deviceId is null or e.device.id = :deviceId)
            order by e.device.id asc, e.changedAt asc
            """)
    List<DeviceStatusEvent> findDownEvents(
            @Param("newStatus") DeviceStatus newStatus,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("category") DeviceCategory category,
            @Param("deviceId") Long deviceId);

    /** Whatever this device transitioned to next after `after` -- used to
     *  close out a DOWN incident with its recovery time, if one happened. */
    Optional<DeviceStatusEvent> findFirstByDeviceIdAndChangedAtAfterOrderByChangedAtAsc(
            Long deviceId, Instant after);
}
