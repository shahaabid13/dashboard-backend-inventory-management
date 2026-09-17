package com.inventory.msp.controller;

import com.inventory.msp.model.DeviceHistory;
import com.inventory.msp.repository.DeviceHistoryRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class DeviceHistoryController {

    private final DeviceHistoryRepository historyRepo;

    //  Full history
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','AGENCY','VIEWER')")
    public ResponseEntity<List<DeviceHistory>> all() {
        return ResponseEntity.ok(historyRepo.findAll());
    }

    //  History by device ID
    @GetMapping("/device/{deviceId}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENCY','VIEWER')")
    public ResponseEntity<List<DeviceHistory>> byDevice(@PathVariable Long deviceId) {
        return ResponseEntity.ok(historyRepo.findByDeviceIdOrderByCreatedAtDesc(deviceId));
    }

    //  Single record
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENCY','VIEWER')")
    public ResponseEntity<DeviceHistory> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                historyRepo.findById(id).orElseThrow(() -> new RuntimeException("Record not found"))
        );
    }
}
