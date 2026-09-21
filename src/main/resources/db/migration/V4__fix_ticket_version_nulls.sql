UPDATE tickets
SET version = 0
WHERE version IS NULL;

ALTER TABLE tickets
    MODIFY version BIGINT NOT NULL DEFAULT 0;
