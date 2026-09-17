package com.inventory.msp.vms.controller;

import com.inventory.msp.vms.dto.external.ExternalEventSearchResponse;
import com.inventory.msp.vms.dto.request.EventCountFilterRequest;
import com.inventory.msp.vms.dto.request.EventSearchFilterRequest;
import com.inventory.msp.vms.dto.response.EventCountResponse;
import com.inventory.msp.vms.service.EventSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventSearchService eventSearchService;

    @PostMapping("/search")
    public ResponseEntity<ExternalEventSearchResponse> searchEvents(@RequestBody EventSearchFilterRequest request) {
        // Default to last 24 hours if timestamps not provided by the client
        long now = System.currentTimeMillis();
        if (request.getEndtimestamp() == null) {
            request.setEndtimestamp(now);
        }
        if (request.getStarttimestamp() == null) {
            request.setStarttimestamp(now - 24L * 60L * 60L * 1000L);
        }
        return ResponseEntity.ok(eventSearchService.searchEvents(request));
    }

    @PostMapping("/count")
    public ResponseEntity<EventCountResponse> countEvents(@Valid @RequestBody EventCountFilterRequest request) {
        return ResponseEntity.ok(eventSearchService.countEvents(request));
    }

    @GetMapping("/count")
    public ResponseEntity<EventCountResponse> countEventsGet(
            @RequestParam(required = false) String serverId,
            @RequestParam Long starttimestamp,
            @RequestParam Long endtimestamp,
            @RequestParam(required = false) String lpnumber,
            @RequestParam(required = false) String channelid,
            @RequestParam(required = false) String applicationid) {
        EventCountFilterRequest request = EventCountFilterRequest.builder()
                .serverId(parseServerId(serverId))
                .starttimestamp(starttimestamp)
                .endtimestamp(endtimestamp)
                .lpnumber(lpnumber)
                .channelid(channelid)
                .applicationid(applicationid)
                .build();
        return ResponseEntity.ok(eventSearchService.countEvents(request));
    }

    private Integer parseServerId(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("serverId must be a numeric ID or 'all'", e);
        }
    }
}
