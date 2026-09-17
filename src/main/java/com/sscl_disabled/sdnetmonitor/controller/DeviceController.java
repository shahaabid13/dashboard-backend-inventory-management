package com.sscl.sdnetmonitor.controller;

import com.sscl.sdnetmonitor.dto.DeviceDto;
import com.sscl.sdnetmonitor.dto.UptimeSummaryDto;
import com.sscl.sdnetmonitor.service.DeviceService;
import com.sscl.sdnetmonitor.service.UptimeCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController("sdnetDeviceController")
@RequestMapping("/api/sdnet-monitor/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final UptimeCalculationService uptimeCalculationService;

    @GetMapping
    public List<DeviceDto> findAll(
            @RequestParam(required = false) String junctionId,
            @RequestParam(required = false) String category) {
        if (junctionId != null) return deviceService.findByJunction(junctionId);
        if (category != null) return deviceService.findByCategory(category);
        return deviceService.findAll();
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return deviceService.allCategories();
    }

    @GetMapping("/{id}")
    public DeviceDto findById(@PathVariable Long id) {
        return deviceService.findById(id);
    }

    @GetMapping("/{id}/uptime")
    public UptimeSummaryDto uptime(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return uptimeCalculationService.forDevice(id, from, to != null ? to : Instant.now());
    }
}
