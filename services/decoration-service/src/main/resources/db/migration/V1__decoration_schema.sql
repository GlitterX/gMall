CREATE TABLE decoration_page (
    page_id VARCHAR(64) PRIMARY KEY,
    storefront_id VARCHAR(64) NOT NULL,
    page_code VARCHAR(64) NOT NULL,
    page_type VARCHAR(32) NOT NULL,
    terminal_type VARCHAR(16) NOT NULL,
    page_name VARCHAR(128) NOT NULL,
    page_status VARCHAR(32) NOT NULL,
    current_snapshot_id VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_decoration_page_storefront_code_terminal
    ON decoration_page (storefront_id, page_code, terminal_type);

CREATE TABLE decoration_draft (
    draft_id VARCHAR(64) PRIMARY KEY,
    page_id VARCHAR(64) NOT NULL,
    draft_version INTEGER NOT NULL,
    draft_status VARCHAR(32) NOT NULL,
    layout_config VARCHAR(8000) NOT NULL,
    component_tree VARCHAR(8000) NOT NULL,
    locale_content_map VARCHAR(8000) NOT NULL,
    theme_config VARCHAR(4000) NOT NULL,
    navigation_config VARCHAR(4000) NOT NULL,
    validation_status VARCHAR(32) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64) NOT NULL
);

CREATE UNIQUE INDEX uk_decoration_draft_page_version
    ON decoration_draft (page_id, draft_version);

CREATE TABLE decoration_review_record (
    review_id VARCHAR(64) PRIMARY KEY,
    draft_id VARCHAR(64) NOT NULL,
    review_status VARCHAR(32) NOT NULL,
    reviewer_id VARCHAR(64),
    review_comment VARCHAR(1000),
    reviewed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE decoration_snapshot (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    page_id VARCHAR(64) NOT NULL,
    terminal_type VARCHAR(16) NOT NULL,
    snapshot_version INTEGER NOT NULL,
    source_draft_id VARCHAR(64) NOT NULL,
    published_payload VARCHAR(16000) NOT NULL,
    payload_checksum VARCHAR(128) NOT NULL,
    snapshot_status VARCHAR(32) NOT NULL,
    published_by VARCHAR(64) NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_decoration_snapshot_page_terminal_version
    ON decoration_snapshot (page_id, terminal_type, snapshot_version);

CREATE TABLE decoration_publish_record (
    publish_id VARCHAR(64) PRIMARY KEY,
    page_id VARCHAR(64) NOT NULL,
    draft_id VARCHAR(64),
    review_id VARCHAR(64),
    operation_type VARCHAR(32) NOT NULL,
    operation_status VARCHAR(32) NOT NULL,
    operation_request_id VARCHAR(128) NOT NULL,
    target_terminal_type VARCHAR(16) NOT NULL,
    from_snapshot_id VARCHAR(64),
    result_snapshot_id VARCHAR(64),
    rollback_target_snapshot_id VARCHAR(64),
    operator_id VARCHAR(64) NOT NULL,
    operation_comment VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    operated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_decoration_publish_record_request_id
    ON decoration_publish_record (operation_request_id);

CREATE TABLE decoration_outbox_event (
    event_id VARCHAR(64) PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    aggregate_version BIGINT NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload VARCHAR(8000) NOT NULL,
    publish_status VARCHAR(32) NOT NULL,
    retry_count INTEGER NOT NULL,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_decoration_outbox_publish_status
    ON decoration_outbox_event (publish_status, next_retry_at, created_at);
