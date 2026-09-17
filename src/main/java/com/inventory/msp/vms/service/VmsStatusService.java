package com.inventory.msp.vms.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class VmsStatusService {

    @Getter
    static class ServerHealth {
        private volatile Instant lastSuccess;
        private volatile Instant lastFailure;
        private volatile int consecutiveFailures;

        void recordSuccess() {
            lastSuccess = Instant.now();
            consecutiveFailures = 0;
        }

        void recordFailure() {
            lastFailure = Instant.now();
            consecutiveFailures++;
        }
    }

    private final Map<Integer, ServerHealth> health = new ConcurrentHashMap<>();

    public void recordSuccess(Integer serverId) {
        health.computeIfAbsent(serverId, k -> new ServerHealth()).recordSuccess();
        log.debug("Recorded success for server {}", serverId);
    }

    public void recordFailure(Integer serverId) {
        health.computeIfAbsent(serverId, k -> new ServerHealth()).recordFailure();
        log.debug("Recorded failure for server {}", serverId);
    }

    public Map<Integer, ServerHealth> getHealthMap() {
        return health;
    }

    public String getLastSuccessIso(Integer serverId) {
        ServerHealth h = health.get(serverId);
        return h == null || h.lastSuccess == null ? null : h.lastSuccess.toString();
    }

    public String getLastFailureIso(Integer serverId) {
        ServerHealth h = health.get(serverId);
        return h == null || h.lastFailure == null ? null : h.lastFailure.toString();
    }

    public List<VmsStatusDto> snapshotStatus(List<com.inventory.msp.vms.entity.Server> servers) {
        List<VmsStatusDto> out = new ArrayList<>();
        for (com.inventory.msp.vms.entity.Server s : servers) {
            ServerHealth h = health.get(s.getServerId());
            VmsStatusDto dto = new VmsStatusDto();
            dto.setServerId(s.getServerId());
            dto.setServerName(s.getServerName());
            dto.setReachable(h == null || h.consecutiveFailures == 0);
            dto.setLastSuccess(h == null ? null : h.lastSuccess == null ? null : h.lastSuccess.toString());
            dto.setLastFailure(h == null ? null : h.lastFailure == null ? null : h.lastFailure.toString());
            dto.setConsecutiveFailures(h == null ? 0 : h.consecutiveFailures);
            out.add(dto);
        }
        return out;
    }

    @Getter
    public static class VmsStatusDto {
        private Integer serverId;
        private String serverName;
        private boolean reachable;
        private String lastSuccess;
        private String lastFailure;
        private int consecutiveFailures;

        public void setServerId(Integer serverId) { this.serverId = serverId; }
        public void setServerName(String serverName) { this.serverName = serverName; }
        public void setReachable(boolean reachable) { this.reachable = reachable; }
        public void setLastSuccess(String lastSuccess) { this.lastSuccess = lastSuccess; }
        public void setLastFailure(String lastFailure) { this.lastFailure = lastFailure; }
        public void setConsecutiveFailures(int consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }
    }
}
