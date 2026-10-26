package com.sscl.sdnetmonitor.repository;

import com.sscl.sdnetmonitor.entity.DeviceStatus;
import com.sscl.sdnetmonitor.entity.FibreLinkStatusEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FibreLinkStatusEventRepository extends JpaRepository<FibreLinkStatusEvent, Long> {

    List<FibreLinkStatusEvent> findByFibreLinkIdAndChangedAtBetweenOrderByChangedAtAsc(
            String fibreLinkId, Instant from, Instant to);

    List<FibreLinkStatusEvent> findTop50ByFibreLinkIdOrderByChangedAtDesc(String fibreLinkId);

    List<FibreLinkStatusEvent> findTop100ByOrderByChangedAtDesc();

    /** Every "went DOWN" transition in the window, across all fibre links. */
    List<FibreLinkStatusEvent> findByNewStatusAndChangedAtBetweenOrderByFibreLinkIdAscChangedAtAsc(
            DeviceStatus newStatus, Instant from, Instant to);

    /** Whatever this link transitioned to next after `after`. */
    Optional<FibreLinkStatusEvent> findFirstByFibreLinkIdAndChangedAtAfterOrderByChangedAtAsc(
            String fibreLinkId, Instant after);
}
