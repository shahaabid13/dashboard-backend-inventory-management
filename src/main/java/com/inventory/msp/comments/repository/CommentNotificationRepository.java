package com.inventory.msp.comments.repository;

import com.inventory.msp.comments.dto.CommentNotificationDto;
import com.inventory.msp.comments.model.EntityType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class CommentNotificationRepository {
    private final JdbcTemplate jdbcTemplate;

    public CommentNotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertMention(Long userId, EntityType entityType, Long entityId,
                              Long commentId, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                INSERT INTO notifications
                (user_id, type, entity_type, entity_id, comment_id, created_at)
                VALUES (?, 'MENTION', ?, ?, ?, ?)
                """, userId, entityType.name(), entityId, commentId, Timestamp.valueOf(createdAt));
    }

    public List<CommentNotificationDto> findForUser(Long userId, boolean unreadOnly) {
        return jdbcTemplate.query("""
                SELECT id, type, entity_type, entity_id, comment_id, created_at, read_at
                FROM notifications
                WHERE user_id = ? AND (? = FALSE OR read_at IS NULL)
                ORDER BY created_at DESC, id DESC
                """, (rs, rowNum) -> new CommentNotificationDto(
                        rs.getLong("id"),
                        rs.getString("type"),
                        EntityType.valueOf(rs.getString("entity_type")),
                        rs.getLong("entity_id"),
                        rs.getLong("comment_id"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toLocalDateTime()
                ), userId, unreadOnly);
    }

    public long countUnread(Long userId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notifications WHERE user_id = ? AND read_at IS NULL
                """, Long.class, userId);
        return count == null ? 0 : count;
    }

    public int markReadForUser(Long notificationId, Long userId, LocalDateTime readAt) {
        return jdbcTemplate.update("""
                UPDATE notifications SET read_at = ?
                WHERE id = ? AND user_id = ? AND read_at IS NULL
                """, Timestamp.valueOf(readAt), notificationId, userId);
    }
}
