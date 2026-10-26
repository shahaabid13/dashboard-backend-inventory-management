package com.inventory.msp.comments.repository;

import com.inventory.msp.comments.model.EntityType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class ThreadParticipantRepository {
    private final JdbcTemplate jdbcTemplate;

    public ThreadParticipantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertMention(EntityType entityType, Long entityId, Long userId,
                              Long grantedBy, LocalDateTime grantedAt) {
        jdbcTemplate.update("""
                INSERT INTO thread_participants
                (entity_type, entity_id, user_id, granted_by, reason, granted_at)
                VALUES (?, ?, ?, ?, 'MENTION', ?)
                ON DUPLICATE KEY UPDATE user_id = VALUES(user_id)
                """, entityType.name(), entityId, userId, grantedBy,
                java.sql.Timestamp.valueOf(grantedAt));
    }

    public boolean exists(EntityType entityType, Long entityId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM thread_participants
                WHERE entity_type = ? AND entity_id = ? AND user_id = ?
                """, Integer.class, entityType.name(), entityId, userId);
        return count != null && count > 0;
    }
}
