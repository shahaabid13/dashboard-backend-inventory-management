package com.inventory.msp.vms.controller;

import com.inventory.msp.vms.service.ChannelSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/channels")
@RequiredArgsConstructor
public class ChannelAdminController {

    private final ChannelSyncService channelSyncService;

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> manualSync() {
        List<String> results = channelSyncService.syncAllServers();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Channel synchronization executed",
                "details", results
        ));
    }
}
