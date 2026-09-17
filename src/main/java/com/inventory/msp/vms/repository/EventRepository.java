package com.inventory.msp.vms.repository;

import com.inventory.msp.vms.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e WHERE " +
           "(:serverId IS NULL OR e.serverId = :serverId) AND " +
           "(:channelId IS NULL OR e.channelId = :channelId) AND " +
           "(:startTimestamp IS NULL OR e.eventTimestamp >= :startTimestamp) AND " +
           "(:endTimestamp IS NULL OR e.eventTimestamp <= :endTimestamp) AND " +
           "(:lpNumber IS NULL OR LOWER(e.lpNumber) LIKE LOWER(CONCAT('%', :lpNumber, '%')))")
    Page<Event> searchEvents(
            @Param("serverId") Integer serverId,
            @Param("channelId") String channelId,
            @Param("startTimestamp") Long startTimestamp,
            @Param("endTimestamp") Long endTimestamp,
            @Param("lpNumber") String lpNumber,
            Pageable pageable
    );
}
