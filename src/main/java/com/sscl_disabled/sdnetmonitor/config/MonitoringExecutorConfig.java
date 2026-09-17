package com.sscl.sdnetmonitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Thread pool the monitoring sweep uses to ping ~1177 devices concurrently.
 * Sized via sdnet.monitoring.ping-thread-pool-size (default 300) so a full
 * sweep fits inside the 10s sweep-interval-ms cadence -- see the comment
 * next to ping-thread-pool-size in application.yml for the batching math.
 */
@Configuration
@EnableScheduling
public class MonitoringExecutorConfig {

    @Bean
    public ExecutorService pingExecutorService(
            @Value("${sdnet.monitoring.ping-thread-pool-size:80}") int poolSize) {
        return Executors.newFixedThreadPool(poolSize);
    }
}
