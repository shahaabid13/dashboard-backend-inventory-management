package com.inventory.msp.comments.repository;

import com.inventory.msp.comments.model.Comment;
import com.inventory.msp.comments.model.EntityType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.util.List;
import java.util.Objects;

@Repository
public class CommentRepository {
    private static final RowMapper<Comment> ROW_MAPPER = (rs, rowNum) -> {
        Comment comment = new Comment();
        comment.setId(rs.getLong("id"));
        comment.setEntityType(EntityType.valueOf(rs.getString("entity_type")));
        comment.setEntityId(rs.getLong("entity_id"));
        comment.setAuthorId(rs.getLong("author_id"));
        comment.setAuthorName(rs.getString("author_name"));
        comment.setAuthorRole(rs.getString("author_role"));
        comment.setBody(rs.getString("body"));
        comment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return comment;
    };

    private final JdbcTemplate jdbcTemplate;

    public CommentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Comment insert(Comment comment) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO comments
                    (entity_type, entity_id, author_id, author_name, author_role, body, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, comment.getEntityType().name());
            statement.setLong(2, comment.getEntityId());
            statement.setLong(3, comment.getAuthorId());
            statement.setString(4, comment.getAuthorName());
            statement.setString(5, comment.getAuthorRole());
            statement.setString(6, comment.getBody());
            statement.setTimestamp(7, java.sql.Timestamp.valueOf(comment.getCreatedAt()));
            return statement;
        }, keyHolder);
        comment.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return comment;
    }

    public List<Comment> findThread(EntityType entityType, Long entityId) {
        return jdbcTemplate.query("""
                SELECT id, entity_type, entity_id, author_id, author_name, author_role, body, created_at
                FROM comments
                WHERE entity_type = ? AND entity_id = ?
                ORDER BY created_at ASC, id ASC
                """, ROW_MAPPER, entityType.name(), entityId);
    }
}
