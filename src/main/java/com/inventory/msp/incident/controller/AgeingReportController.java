package com.inventory.msp.incident.controller;

import com.inventory.msp.incident.dto.AgeingReportDTO;
import com.inventory.msp.incident.dto.TopDefaulterDTO;
import com.inventory.msp.incident.service.AgeingReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cims/reports/ageing")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','COORDINATOR','REVIEWER')")
public class AgeingReportController {

    private final AgeingReportService ageingReportService;

    @GetMapping
    public List<AgeingReportDTO> getAgeingReport() {
        return ageingReportService.getAgeingReport();
    }

    @GetMapping("/top")
    public List<TopDefaulterDTO> getTopDefaulters() {
        return ageingReportService.getTopDefaulters();
    }

    @GetMapping("/{userId}")
    public AgeingReportDTO getAgeingReportForUser(@PathVariable Long userId) {
        return ageingReportService.getAgeingReport(userId);
    }
}
