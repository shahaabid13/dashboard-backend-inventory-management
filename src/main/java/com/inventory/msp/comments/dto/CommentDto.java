package com.inventory.msp.comments.dto;

import com.inventory.msp.comments.model.EntityType;

import java.time.LocalDateTime;
import java.util.List;

public record CommentDto(
        Long id,
        EntityType entityType,
        Long entityId,
        Long authorId,
        String authorName,
        String authorRole,
        String body,
        LocalDateTime createdAt,
        List<Long> mentionedUserIds) {
}
