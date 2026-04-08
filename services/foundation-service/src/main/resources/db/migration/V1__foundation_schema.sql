CREATE TABLE organization_profile (
    organization_id VARCHAR(64) PRIMARY KEY,
    organization_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    default_locale VARCHAR(32) NOT NULL,
    supported_locales VARCHAR(256) NOT NULL,
    operator_id VARCHAR(64) NOT NULL,
    operation_reason VARCHAR(256) NOT NULL,
    operation_at TIMESTAMP WITH TIME ZONE NOT NULL,
    aggregate_version BIGINT NOT NULL
);

CREATE TABLE seller_profile (
    seller_id VARCHAR(64) PRIMARY KEY,
    organization_id VARCHAR(64) NOT NULL,
    seller_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    operator_id VARCHAR(64) NOT NULL,
    operation_reason VARCHAR(256) NOT NULL,
    operation_at TIMESTAMP WITH TIME ZONE NOT NULL,
    aggregate_version BIGINT NOT NULL
);

CREATE TABLE storefront_profile (
    storefront_id VARCHAR(64) PRIMARY KEY,
    organization_id VARCHAR(64) NOT NULL,
    seller_id VARCHAR(64) NOT NULL,
    storefront_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    default_locale VARCHAR(32) NOT NULL,
    supported_locales VARCHAR(256) NOT NULL,
    operator_id VARCHAR(64) NOT NULL,
    operation_reason VARCHAR(256) NOT NULL,
    operation_at TIMESTAMP WITH TIME ZONE NOT NULL,
    aggregate_version BIGINT NOT NULL
);

CREATE TABLE outbox_event (
    event_id VARCHAR(64) PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload VARCHAR(8000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);
