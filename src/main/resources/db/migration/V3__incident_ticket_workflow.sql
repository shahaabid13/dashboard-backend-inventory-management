ALTER TABLE tickets
    ADD COLUMN updated_at TIMESTAMP NULL AFTER created_at,
    ADD COLUMN version BIGINT NULL AFTER updated_at;

UPDATE tickets
SET version = 0
WHERE version IS NULL;

ALTER TABLE tickets
    MODIFY version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE tickets
    MODIFY description LONGTEXT NULL;

ALTER TABLE ticket_history
    ADD COLUMN action VARCHAR(50) NULL AFTER to_status,
    ADD COLUMN remarks VARCHAR(1000) NULL AFTER action,
    ADD COLUMN performed_by_user_id BIGINT NULL AFTER changed_by_user_id,
    ADD COLUMN performed_at TIMESTAMP NULL AFTER changed_at,
    ADD COLUMN assigned_to_user_id BIGINT NULL AFTER performed_at,
    ADD COLUMN assigned_to_role VARCHAR(50) NULL AFTER assigned_to_user_id;

UPDATE ticket_history
SET performed_by_user_id = changed_by_user_id,
    performed_at = changed_at,
    remarks = notes
WHERE performed_by_user_id IS NULL;

ALTER TABLE ticket_history
    ADD CONSTRAINT fk_history_performed_by FOREIGN KEY (performed_by_user_id) REFERENCES appuser(id);

ALTER TABLE ticket_history
    ADD CONSTRAINT fk_history_assigned_to FOREIGN KEY (assigned_to_user_id) REFERENCES appuser(id);

CREATE INDEX idx_ticket_history_performed_at ON ticket_history (performed_at DESC);
CREATE INDEX idx_ticket_history_action ON ticket_history (action);
CREATE INDEX idx_ticket_updated_at ON tickets (updated_at DESC);
