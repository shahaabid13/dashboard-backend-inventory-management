package com.inventory.msp.vms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "vms-tms")
public class VmsTmsProperties {

    private String username = "bel";
    private String password = "Password@123";

    // Default connect timeout reduced to 5s to avoid long user-facing waits; can be overridden via application.properties
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(30);
    private String storageDirectory = "uploads/evidence";
    private String syncCron = "0 0 2 * * ?";
    private boolean trustSelfSignedCert = true;
    private String loginBaseUrl = "https://172.30.0.52:7443";
    private Integer defaultServerId = 100;
    private String baseUrl = "https://172.30.0.52:7443/REST/{serverId}";
    private String loginEndpoint = "${vms-tms.base-url}/login";
    private String eventCountEndpoint = "${vms-tms.base-url}/event/count";
    private String eventSearchEndpoint = "${vms-tms.base-url}/event/search";

    // Event sync configuration
    private long eventSyncIntervalMs = 60000; // how often the background sync runs (ms)
    private long eventSyncWindowMs = 300000; // lookback window when polling external events (ms)
    private int eventSyncRetries = 3; // retry attempts when polling fails
    private long eventSyncRetryBaseMs = 1000; // base delay for exponential backoff (ms)
}
