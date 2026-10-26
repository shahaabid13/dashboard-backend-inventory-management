package com.inventory.msp.comments.dto;

import com.inventory.msp.comments.model.EntityType;

import java.time.LocalDateTime;

public record CommentNotificationDto(
        Long id,
        String type,
        EntityType entityType,
        Long entityId,
        Long commentId,
        LocalDateTime createdAt,
        LocalDateTime readAt) {
}
