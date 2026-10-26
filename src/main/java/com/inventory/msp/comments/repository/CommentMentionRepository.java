package com.inventory.msp.comments.repository;

import com.inventory.msp.comments.model.CommentMention;
import com.inventory.msp.comments.model.CommentMentionId;
import com.inventory.msp.comments.model.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentMentionRepository extends JpaRepository<CommentMention, CommentMentionId> {
    List<CommentMention> findByCommentIdInOrderByCommentIdAscMentionedUserIdAsc(List<Long> commentIds);

    @Query(value = """
            SELECT COUNT(*)
            FROM comment_mentions cm
            JOIN comments c ON c.id = cm.comment_id
            WHERE c.entity_type = :entityType
              AND c.entity_id = :entityId
              AND cm.mentioned_user_id = :userId
            """, nativeQuery = true)
    long countForEntityAndMentionedUser(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("userId") Long userId);
}
