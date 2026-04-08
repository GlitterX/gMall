CREATE TABLE admission_review_record (
    record_id VARCHAR(96) PRIMARY KEY,
    application_id VARCHAR(64) NOT NULL,
    from_status VARCHAR(32) NOT NULL,
    to_status VARCHAR(32) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    operator_id VARCHAR(64) NOT NULL,
    operation_reason VARCHAR(256) NOT NULL,
    operated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    aggregate_version BIGINT NOT NULL
);

CREATE INDEX idx_admission_review_record_application_time
    ON admission_review_record (application_id, operated_at);
