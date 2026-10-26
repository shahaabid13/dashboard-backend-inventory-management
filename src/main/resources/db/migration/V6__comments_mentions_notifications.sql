ALTER TABLE app_user
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    entity_type VARCHAR(16) NOT NULL,
    entity_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    author_role VARCHAR(50) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES app_user (id),
    CONSTRAINT chk_comments_entity_type CHECK (entity_type IN ('TASK', 'TICKET')),
    CONSTRAINT chk_comments_body_not_blank CHECK (CHAR_LENGTH(TRIM(body)) > 0),
    INDEX idx_comments_thread_created (entity_type, entity_id, created_at)
);

CREATE TABLE comment_mentions (
    comment_id BIGINT NOT NULL,
    mentioned_user_id BIGINT NOT NULL,
    PRIMARY KEY (comment_id, mentioned_user_id),
    CONSTRAINT fk_comment_mentions_comment FOREIGN KEY (comment_id) REFERENCES comments (id),
    CONSTRAINT fk_comment_mentions_user FOREIGN KEY (mentioned_user_id) REFERENCES app_user (id)
);

CREATE TABLE thread_participants (
    entity_type VARCHAR(16) NOT NULL,
    entity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    granted_by BIGINT NOT NULL,
    reason VARCHAR(16) NOT NULL,
    granted_at DATETIME NOT NULL,
    PRIMARY KEY (entity_type, entity_id, user_id),
    CONSTRAINT fk_thread_participants_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_thread_participants_granted_by FOREIGN KEY (granted_by) REFERENCES app_user (id),
    CONSTRAINT chk_thread_participants_entity_type CHECK (entity_type IN ('TASK', 'TICKET')),
    CONSTRAINT chk_thread_participants_reason CHECK (reason = 'MENTION')
);

CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(16) NOT NULL,
    entity_type VARCHAR(16) NOT NULL,
    entity_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    read_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_notifications_comment FOREIGN KEY (comment_id) REFERENCES comments (id),
    CONSTRAINT chk_notifications_type CHECK (type = 'MENTION'),
    CONSTRAINT chk_notifications_entity_type CHECK (entity_type IN ('TASK', 'TICKET')),
    INDEX idx_notifications_user_read (user_id, read_at)
);
