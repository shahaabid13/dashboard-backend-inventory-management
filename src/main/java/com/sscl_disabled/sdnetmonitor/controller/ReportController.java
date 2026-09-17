package com.sscl.sdnetmonitor.controller;

import com.sscl.sdnetmonitor.dto.CategorySlaDto;
import com.sscl.sdnetmonitor.dto.DowntimeIncidentDto;
import com.sscl.sdnetmonitor.entity.DeviceCategory;
import com.sscl.sdnetmonitor.service.DowntimeReportPdfService;
import com.sscl.sdnetmonitor.service.DowntimeReportService;
import com.sscl.sdnetmonitor.service.SlaReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/sdnet-monitor/reports")
@RequiredArgsConstructor
public class ReportController {

    private final DowntimeReportService downtimeReportService;
    private final DowntimeReportPdfService downtimeReportPdfService;
    private final SlaReportService slaReportService;

    @GetMapping("/downtime")
    public List<DowntimeIncidentDto> downtime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long deviceId) {
        return downtimeReportService.generateIncidents(from, to != null ? to : Instant.now(), parseCategory(category), deviceId);
    }

    @GetMapping(value = "/downtime.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downtimePdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long deviceId) {
        Instant effectiveTo = to != null ? to : Instant.now();
        DeviceCategory parsedCategory = parseCategory(category);
        List<DowntimeIncidentDto> incidents = downtimeReportService.generateIncidents(from, effectiveTo, parsedCategory, deviceId);
        String filterNote = deviceId != null ? "Device #" + deviceId : category;
        byte[] pdf = downtimeReportPdfService.render(incidents, from, effectiveTo, filterNote);

        String filename = "sdnet-downtime-" + (deviceId != null ? "device" + deviceId + "-" : category != null ? category.toLowerCase() + "-" : "")
                + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                        .withZone(java.time.ZoneId.of("Asia/Kolkata")).format(Instant.now()) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** SLA figures (uptime %, total down time, currently-down count) per
     *  device category over [from, to] -- one row per category, or a single
     *  row if `category` is given. */
    @GetMapping("/sla-summary")
    public List<CategorySlaDto> slaSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String category) {
        return slaReportService.generateSummary(from, to != null ? to : Instant.now(), parseCategory(category));
    }

    private DeviceCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return DeviceCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown device category '" + raw + "' -- must be one of " + List.of(DeviceCategory.values()));
        }
    }
}
