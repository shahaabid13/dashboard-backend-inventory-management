package com.inventory.msp.comments.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
public class CommentMentionId implements Serializable {
    private Long commentId;
    private Long mentionedUserId;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CommentMentionId that)) return false;
        return Objects.equals(commentId, that.commentId)
                && Objects.equals(mentionedUserId, that.mentionedUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commentId, mentionedUserId);
    }
}
