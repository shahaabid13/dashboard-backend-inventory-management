package com.inventory.msp.incident.service;

public interface SmsGateway {
    void sendSms(String to, String message) throws Exception;
}
