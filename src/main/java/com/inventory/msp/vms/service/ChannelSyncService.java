package com.inventory.msp.vms.service;

import com.inventory.msp.vms.dto.external.ExternalChannelDto;
import com.inventory.msp.vms.entity.Channel;
import com.inventory.msp.vms.entity.Server;
import com.inventory.msp.vms.exception.ExternalApiException;
import com.inventory.msp.vms.repository.ChannelRepository;
import com.inventory.msp.vms.repository.ServerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ChannelSyncService {

    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;
    private final RestTemplate vmsRestTemplate;

    public ChannelSyncService(
            ServerRepository serverRepository,
            ChannelRepository channelRepository,
            @Qualifier("vmsRestTemplate") RestTemplate vmsRestTemplate) {
        this.serverRepository = serverRepository;
        this.channelRepository = channelRepository;
        this.vmsRestTemplate = vmsRestTemplate;
    }

    @Scheduled(cron = "${vms-tms.sync-cron:0 0 2 * * ?}")
    public void scheduledSync() {
        log.info("Starting scheduled channel sync for all active servers...");
        syncAllServers();
    }

    @Transactional
    public List<String> syncAllServers() {
        List<Server> activeServers = serverRepository.findByIsActiveTrue();
        List<String> syncResults = new ArrayList<>();

        for (Server server : activeServers) {
            try {
                int syncedCount = syncServerChannels(server);
                String msg = String.format("Server ID %d (%s): Successfully synced %d channels.",
                        server.getServerId(), server.getServerName(), syncedCount);
                log.info(msg);
                syncResults.add(msg);
            } catch (Exception e) {
                String errMsg = String.format("Server ID %d (%s) sync failed: %s",
                        server.getServerId(), server.getServerName(), e.getMessage());
                log.error(errMsg, e);
                syncResults.add(errMsg);
            }
        }
        return syncResults;
    }

    @Transactional
    public int syncServerChannels(Server server) {
        String url = String.format("%s/REST/%d/channel", server.getBaseUrl(), server.getServerId());
        log.info("Fetching channels from: {}", url);

        try {
            ResponseEntity<List<ExternalChannelDto>> response = vmsRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ExternalChannelDto>>() {}
            );

            List<ExternalChannelDto> externalChannels = response.getBody();
            if (externalChannels == null || externalChannels.isEmpty()) {
                log.warn("No channels returned from server {}", server.getServerId());
                return 0;
            }

            for (ExternalChannelDto ext : externalChannels) {
                upsertChannel(server.getServerId(), ext);
            }

            return externalChannels.size();
        } catch (Exception e) {
            throw new ExternalApiException("Failed to sync channels from external endpoint: " + url + ". Reason: " + e.getMessage(), e);
        }
    }

    private void upsertChannel(Integer serverId, ExternalChannelDto ext) {
        Channel channel = channelRepository.findByServerIdAndChannelId(serverId, ext.getChannelId())
                .orElseGet(() -> Channel.builder()
                        .serverId(serverId)
                        .channelId(ext.getChannelId())
                        .build());

        channel.setChannelName(ext.getChannelName());
        channel.setChannelIp(ext.getChannelIp());
        channel.setChannelType(ext.getChannelType());
        channel.setSnapUrl(ext.getSnapUrl());
        channel.setMajorUrl(ext.getMajorUrl());
        channel.setMinorUrl(ext.getMinorUrl());
        channel.setAnalyticUrl(ext.getAnalyticUrl());
        channel.setUsername(ext.getUsername());
        channel.setPasswordEncrypted(ext.getPassword());
        channel.setLatitude(ext.getLatitude());
        channel.setLongitude(ext.getLongitude());
        channel.setLocation(ext.getLocation());
        channel.setDescription(ext.getDescription());
        channel.setChannelMacId(ext.getChannelMacId());
        channel.setRecordingServerId(ext.getRecordingServerId());
        channel.setRecordingServerName(ext.getRecordingServerName());
        channel.setRecordingStream(ext.getRecordingStream());
        channel.setCameraInstallationType(ext.getCameraInstallationType());
        channel.setUuid(ext.getUuid());

        channelRepository.save(channel);
    }
}
