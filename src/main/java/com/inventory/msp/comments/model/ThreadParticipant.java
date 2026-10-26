package com.inventory.msp.comments.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "thread_participants")
@IdClass(ThreadParticipantId.class)
@Getter
@NoArgsConstructor
public class ThreadParticipant {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 16)
    private EntityType entityType;

    @Id
    @Column(name = "entity_id")
    private Long entityId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "granted_by", nullable = false)
    private Long grantedBy;

    @Column(nullable = false, length = 16)
    private String reason;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    public ThreadParticipant(EntityType entityType, Long entityId, Long userId,
                             Long grantedBy, String reason, LocalDateTime grantedAt) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.userId = userId;
        this.grantedBy = grantedBy;
        this.reason = reason;
        this.grantedAt = grantedAt;
    }
}
