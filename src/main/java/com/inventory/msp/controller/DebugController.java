package com.inventory.msp.controller;

import com.inventory.msp.model.Device;
import com.inventory.msp.services.DeviceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private static final Logger log = LoggerFactory.getLogger(DebugController.class);
    private final DeviceService deviceService;

    @GetMapping("/devices")
    public ResponseEntity<List<Map<String,Object>>> allDevicesRaw() {
        List<Device> devices = deviceService.getAllDevices();
        List<Map<String,Object>> out = new ArrayList<>();
        for (Device d : devices) {
            Map<String,Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("serialNumber", d.getSerialNumber());
            m.put("location", d.getLocation() != null ? d.getLocation().getName() : null);
            out.add(m);
        }

        if (log.isDebugEnabled()) {
            for (Map<String,Object> m : out) {
                log.debug("[DebugController] device id={} serial='{}'", m.get("id"), m.get("serialNumber"));
            }
        }
        return ResponseEntity.ok(out);
    }
}
