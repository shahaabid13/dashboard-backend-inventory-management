package com.sscl.sdnetmonitor.controller;

import com.sscl.sdnetmonitor.dto.DashboardSummaryDto;
import com.sscl.sdnetmonitor.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sdnet-monitor/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryDto summary() {
        return dashboardService.summary();
    }
}
