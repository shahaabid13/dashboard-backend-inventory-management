package com.inventory.msp.incident.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailSender {

    private final JavaMailSender mailSender;
    private final String mailFrom;
    private final boolean enabled;

    public EmailSender(JavaMailSender mailSender,
                       @Value("${MAIL_HOST:}") String host,
                       @Value("${MAIL_FROM:}") String mailFrom) {
        this.mailSender = mailSender;
        this.mailFrom = (mailFrom == null || mailFrom.isBlank()) ? "noreply@example.com" : mailFrom;
        this.enabled = host != null && !host.isBlank();
        if (!this.enabled) {
            log.warn("MAIL_HOST not configured - email sending will be skipped");
        }
    }

    public void send(String to, String subject, String body) {
        if (!enabled) {
            log.info("Email sending skipped (disabled). to={}, subject={}", to, subject);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email dispatched to {} subject={}", to, subject);
        } catch (Exception ex) {
            log.error("Error sending email to {}: {}", to, ex.getMessage());
            throw ex;
        }
    }

    @Async
    public void sendAsync(String to, String subject, String body) {
        send(to, subject, body);
    }
}
