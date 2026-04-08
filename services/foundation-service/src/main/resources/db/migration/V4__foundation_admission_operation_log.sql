CREATE TABLE admission_operation_log (
    operation_id VARCHAR(192) PRIMARY KEY,
    application_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    aggregate_version BIGINT NOT NULL,
    operated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_admission_operation_log_request
    ON admission_operation_log (application_id, action_type, request_fingerprint);

CREATE INDEX idx_admission_operation_log_application_time
    ON admission_operation_log (application_id, operated_at);
