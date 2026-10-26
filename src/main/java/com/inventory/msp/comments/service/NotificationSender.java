package com.inventory.msp.comments.service;

// Reserved integration point for future email/push delivery; mentions currently use in-app records only.
public interface NotificationSender {
    void sendMention(Long recipientUserId, Long commentId);
}
