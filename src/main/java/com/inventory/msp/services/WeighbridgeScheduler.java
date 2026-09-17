package com.inventory.msp.services;


import com.inventory.msp.services.WeighbridgeService;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;

@Component
public class WeighbridgeScheduler {

    private final WeighbridgeService service;
    private static final Logger log= (Logger) LoggerFactory.getLogger(WeighbridgeScheduler.class);

    public WeighbridgeScheduler(WeighbridgeService service) {
        this.service = service;
    }

    @Scheduled(fixedRate = 3600000)
    public void autoSync() {
        log.info("Scheduler triggered. Syncing Weighbridge Data...");
        service.syncWeighbridgeData();
    }
}
