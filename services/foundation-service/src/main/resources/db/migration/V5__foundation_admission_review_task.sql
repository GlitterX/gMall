CREATE TABLE admission_review_task (
    task_id VARCHAR(96) PRIMARY KEY,
    application_id VARCHAR(64) NOT NULL,
    task_sequence BIGINT NOT NULL,
    task_status VARCHAR(32) NOT NULL,
    reviewer_id VARCHAR(64) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deadline_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_operated_by VARCHAR(64) NOT NULL,
    last_operation_reason VARCHAR(256) NOT NULL,
    last_operated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    aggregate_version BIGINT NOT NULL
);

CREATE UNIQUE INDEX uk_admission_review_task_application_sequence
    ON admission_review_task (application_id, task_sequence);

CREATE INDEX idx_admission_review_task_application_status
    ON admission_review_task (application_id, task_status);
