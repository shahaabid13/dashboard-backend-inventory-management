package com.inventory.msp.vms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.msp.vms.dto.external.ExternalEventItemDto;
import com.inventory.msp.vms.entity.Event;
import com.inventory.msp.vms.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventPersistenceService {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Saves a single event in its own independent transaction. A failure here
     * (bad data, constraint violation, etc.) can never roll back the caller's
     * transaction — it's isolated to just this one row.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveEvent(Integer serverId, ExternalEventItemDto item, String savedImagePath) {
        Long timestamp = resolveEventTimestamp(item);
        if (timestamp == null) {
            log.warn("Skipping event with unresolvable timestamp: eventId={}, channelId={}, eventtime={}",
                    item.getEventId(), item.getChannelId(), item.getEventTime());
            return;
        }
        try {
            String rawJson = objectMapper.writeValueAsString(item);
            Event event = Event.builder()
                    .serverId(serverId)
                    .channelId(item.getChannelId())
                    .applicationId(item.getApplicationId())
                    .lpNumber(item.getLpNumber())
                    .eventTimestamp(timestamp)
                    .imagePath(savedImagePath)
                    .fileName(item.getName())
                    .rawResponse(rawJson)
                    .build();

            eventRepository.save(event);
        } catch (Exception e) {
            log.error("Could not persist event locally: {}", e.getMessage());
        }
    }

    private Long resolveEventTimestamp(ExternalEventItemDto item) {
        if (item.getEventTimestamp() != null) {
            return item.getEventTimestamp();
        }
        if (item.getEventTime() != null && !item.getEventTime().isBlank()) {
            try {
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(item.getEventTime(), formatter);
                return ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception ex) {
                log.warn("Failed to parse eventtime '{}' as fallback timestamp: {}", item.getEventTime(), ex.getMessage());
            }
        }
        return null;
    }
}
