CREATE TABLE marketing_content_slot (
    content_slot_id VARCHAR(64) PRIMARY KEY,
    slot_code VARCHAR(64) NOT NULL,
    slot_type VARCHAR(32) NOT NULL,
    owner_type VARCHAR(32) NOT NULL,
    owner_id VARCHAR(64) NOT NULL,
    title_i18n VARCHAR(4000) NOT NULL,
    body_i18n VARCHAR(8000) NOT NULL,
    media_list VARCHAR(4000) NOT NULL,
    landing_target VARCHAR(4000) NOT NULL,
    slot_status VARCHAR(16) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_marketing_slot_owner_code
    ON marketing_content_slot (owner_type, owner_id, slot_code);

CREATE INDEX idx_marketing_slot_owner_status
    ON marketing_content_slot (owner_type, owner_id, slot_status, updated_at);
