ALTER TABLE catalog_outbox_event
    ADD COLUMN publish_status VARCHAR(32);

ALTER TABLE catalog_outbox_event
    ADD COLUMN retry_count INTEGER;

ALTER TABLE catalog_outbox_event
    ADD COLUMN next_retry_at TIMESTAMP WITH TIME ZONE;

UPDATE catalog_outbox_event
SET publish_status = CASE
                         WHEN published_at IS NULL THEN 'PENDING'
                         ELSE 'SENT'
    END,
    retry_count   = 0,
    next_retry_at = CASE
                        WHEN published_at IS NULL THEN created_at
                        ELSE NULL
        END;

ALTER TABLE catalog_outbox_event
    ALTER COLUMN publish_status SET NOT NULL;

ALTER TABLE catalog_outbox_event
    ALTER COLUMN retry_count SET NOT NULL;

CREATE INDEX idx_catalog_outbox_publish_status
    ON catalog_outbox_event (publish_status, next_retry_at, created_at);
