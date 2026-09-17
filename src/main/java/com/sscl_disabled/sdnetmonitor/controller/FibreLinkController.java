package com.sscl.sdnetmonitor.controller;

import com.sscl.sdnetmonitor.dto.FibreLinkDto;
import com.sscl.sdnetmonitor.dto.UptimeSummaryDto;
import com.sscl.sdnetmonitor.service.FibreLinkService;
import com.sscl.sdnetmonitor.service.UptimeCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/sdnet-monitor/fibre-links")
@RequiredArgsConstructor
public class FibreLinkController {

    private final FibreLinkService fibreLinkService;
    private final UptimeCalculationService uptimeCalculationService;

    @GetMapping
    public List<FibreLinkDto> findAll(@RequestParam(required = false) String junctionId) {
        if (junctionId != null) return fibreLinkService.findByJunction(junctionId);
        return fibreLinkService.findAll();
    }

    @GetMapping("/{id}")
    public FibreLinkDto findById(@PathVariable String id) {
        return fibreLinkService.findById(id);
    }

    @GetMapping("/{id}/uptime")
    public UptimeSummaryDto uptime(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return uptimeCalculationService.forFibreLink(id, from, to != null ? to : Instant.now());
    }
}
