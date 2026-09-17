package com.inventory.msp.incident.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsSender {

    private final SmsGateway gateway;

    public SmsSender(SmsGateway gateway) {
        this.gateway = gateway;
    }

    public void send(String to, String message) throws Exception {
        try {
            gateway.sendSms(to, message);
            log.info("SMS dispatched to {}", to);
        } catch (Exception ex) {
            log.error("Error sending SMS to {}: {}", to, ex.getMessage());
            throw ex;
        }
    }

    @Async
    public void sendAsync(String to, String message) {
        try {
            send(to, message);
        } catch (Exception e) {
            log.error("Async SMS error to {}: {}", to, e.getMessage());
        }
    }
}
