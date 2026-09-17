package com.inventory.msp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.inventory.msp.services.TrammApiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tramm")
@CrossOrigin(origins = {"http://localhost:4200"}) // adjust once deployed behind a real domain
public class TrammController {

    private final TrammApiService trammApiService;

    public TrammController(TrammApiService trammApiService) {
        this.trammApiService = trammApiService;
    }

    @GetMapping("/corridors")
    public JsonNode corridors() {
        return trammApiService.getCorridorNames();
    }

    @GetMapping("/junctions")
    public JsonNode junctions(@RequestParam String corridorName) {
        return trammApiService.getJunctionNamesForCorridor(corridorName);
    }

    @GetMapping("/junction-details")
    public JsonNode junctionDetails(@RequestParam String corridorName,
                                    @RequestParam String junctionName) {
        return trammApiService.getJunctionDetails(corridorName, junctionName);
    }
}