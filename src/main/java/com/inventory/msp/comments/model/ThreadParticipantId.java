package com.inventory.msp.comments.model;

import java.io.Serializable;
import java.util.Objects;

public class ThreadParticipantId implements Serializable {
    private EntityType entityType;
    private Long entityId;
    private Long userId;

    public ThreadParticipantId() {
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThreadParticipantId that)) return false;
        return entityType == that.entityType
                && Objects.equals(entityId, that.entityId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityType, entityId, userId);
    }
}
