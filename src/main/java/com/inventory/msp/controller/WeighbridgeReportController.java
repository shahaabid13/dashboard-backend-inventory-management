package com.inventory.msp.controller;
import com.inventory.msp.dumpingdto.SummaryResponse;
import com.inventory.msp.dumpingdto.TrendResponse;
import com.inventory.msp.dumpingdto.VehicleTrendResponse;
import com.inventory.msp.model.WeighBridgeEntry;
import com.inventory.msp.services.WeighbridgeReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weighbridge/report")
public class WeighbridgeReportController {

    @Autowired
    private WeighbridgeReportService reportService;

    @GetMapping("/data/all/{wbId}")
    public List<WeighBridgeEntry> getAllDataByWbId(@PathVariable String wbId) {
        return reportService.getAllDataByWbId(wbId);
    }


    @GetMapping("/trend/net/{wbId}")
    public List<TrendResponse> netTrend(@PathVariable String wbId) {
        return reportService.getNetWeightTrend(wbId);
    }

    @GetMapping("/trend/gross/{wbId}")
    public List<TrendResponse> grossTrend(@PathVariable String wbId) {
        return reportService.getGrossWeightTrend(wbId);
    }

    @GetMapping("/trend/vehicle/{wbId}")
    public List<VehicleTrendResponse> vehicleTrend(@PathVariable String wbId) {
        return reportService.getNetWeightByVehicle(wbId);
    }

    @GetMapping("/trend/last24/{wbId}")
    public List<TrendResponse> last24(@PathVariable String wbId) {
        return reportService.getLast24Hours(wbId);
    }

    @GetMapping("/summary")
    public SummaryResponse summary(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam String wbId
    ) {
        return reportService.getRangeSummary(start, end, wbId);
    }
}
