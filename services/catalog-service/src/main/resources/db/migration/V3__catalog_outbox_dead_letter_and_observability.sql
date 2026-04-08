ALTER TABLE catalog_outbox_event
    ADD COLUMN last_attempt_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE catalog_outbox_event
    ADD COLUMN last_error_message VARCHAR(1024);

ALTER TABLE catalog_outbox_event
    ADD COLUMN dead_lettered_at TIMESTAMP WITH TIME ZONE;

UPDATE catalog_outbox_event
SET last_attempt_at = COALESCE(published_at, next_retry_at, created_at);

ALTER TABLE catalog_outbox_event
    ALTER COLUMN last_attempt_at SET NOT NULL;

CREATE INDEX idx_catalog_outbox_dead_letter
    ON catalog_outbox_event (publish_status, dead_lettered_at, created_at);
