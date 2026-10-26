ALTER TABLE tickets
    ADD COLUMN flagged TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN flagged_at DATETIME NULL,
    ADD COLUMN last_reminder_at DATETIME NULL,
    ADD COLUMN reminder_count INT NOT NULL DEFAULT 0;

CREATE INDEX idx_tickets_flagged_status ON tickets (flagged, status);

CREATE TABLE in_app_notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(64) NOT NULL DEFAULT 'GENERAL',
    reference_id BIGINT NULL,
    message TEXT NOT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_in_app_notification_user_created (username, created_at)
);
