package com.inventory.msp.vms.controller;

import com.inventory.msp.vms.entity.Channel;
import com.inventory.msp.vms.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelRepository channelRepository;

    @GetMapping
    public ResponseEntity<List<Channel>> getChannels(
            @RequestParam(required = false) Integer serverId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String location) {
        return ResponseEntity.ok(channelRepository.searchChannels(serverId, type, location));
    }
}
