package com.inventory.msp.controller;

import com.inventory.msp.services.WeighbridgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

@RequestMapping("/api/weighbridge")
public class WeighbridgeController {

    @Autowired
    private WeighbridgeService service;


    @GetMapping("/sync")
    public String sync() {
        service.syncWeighbridgeData();
        return "Data synced successfully!";
    }
}
