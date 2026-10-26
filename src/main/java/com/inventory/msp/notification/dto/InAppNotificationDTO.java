package com.inventory.msp.notification.dto;

import com.inventory.msp.notification.model.InAppNotification;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class InAppNotificationDTO {
    Long id;
    String title;
    String type;
    Long referenceId;
    String message;
    boolean read;
    LocalDateTime createdAt;

    public static InAppNotificationDTO from(InAppNotification notification) {
        return InAppNotificationDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .type(notification.getType())
                .referenceId(notification.getReferenceId())
                .message(notification.getMessage())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
