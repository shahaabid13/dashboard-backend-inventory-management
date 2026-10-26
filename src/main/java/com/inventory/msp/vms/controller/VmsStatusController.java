package com.inventory.msp.vms.controller;

import com.inventory.msp.vms.service.VmsStatusService;
import com.inventory.msp.vms.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/vms")
@RequiredArgsConstructor
public class VmsStatusController {

    private final ServerRepository serverRepository;
    private final VmsStatusService statusService;

    @GetMapping("/status")
    public ResponseEntity<List<VmsStatusService.VmsStatusDto>> status() {
        List<com.inventory.msp.vms.entity.Server> servers = serverRepository.findAll();
        List<VmsStatusService.VmsStatusDto> snapshot = statusService.snapshotStatus(servers);
        return ResponseEntity.ok(snapshot);
    }
}
