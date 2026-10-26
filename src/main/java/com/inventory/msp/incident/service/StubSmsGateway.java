package com.inventory.msp.incident.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StubSmsGateway implements SmsGateway {

    private final boolean enabled;

    public StubSmsGateway(@Value("${SMS_GATEWAY_URL:}") String url) {
        this.enabled = url != null && !url.isBlank();
        if (!enabled) {
            log.warn("SMS_GATEWAY_URL not configured - SMS will be logged to console (stub)");
        }
    }

    @Override
    public void sendSms(String to, String message) throws Exception {
        // If no real provider configured, just log the message
        if (!enabled) {
            log.info("[Stub SMS] to={} message={}", to, message);
            return;
        }
        // Real provider wiring should be implemented by integrator
        log.info("[Configured SMS] to={} message={}", to, message);
    }
}
