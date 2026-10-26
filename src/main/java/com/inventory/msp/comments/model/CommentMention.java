package com.inventory.msp.comments.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comment_mentions")
@IdClass(CommentMentionId.class)
@Getter
@NoArgsConstructor
public class CommentMention {
    @Id
    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Id
    @Column(name = "mentioned_user_id", nullable = false)
    private Long mentionedUserId;

    public CommentMention(Long commentId, Long mentionedUserId) {
        this.commentId = commentId;
        this.mentionedUserId = mentionedUserId;
    }
}
