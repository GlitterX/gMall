ALTER TABLE organization_profile
    ADD COLUMN organization_name VARCHAR(128) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN source_application_id VARCHAR(64);

CREATE TABLE admission_application (
    application_id VARCHAR(64) PRIMARY KEY,
    organization_type VARCHAR(32) NOT NULL,
    application_status VARCHAR(32) NOT NULL,
    applicant_name VARCHAR(128) NOT NULL,
    business_license_no VARCHAR(64) NOT NULL,
    contact_name VARCHAR(64) NOT NULL,
    contact_mobile VARCHAR(32) NOT NULL,
    review_comment VARCHAR(256),
    reviewed_by VARCHAR(64),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    aggregate_version BIGINT NOT NULL
);

CREATE TABLE organization_operating_profile (
    organization_id VARCHAR(64) PRIMARY KEY,
    admission_channel VARCHAR(64) NOT NULL,
    industry_category VARCHAR(64) NOT NULL,
    business_scope VARCHAR(256) NOT NULL,
    remark VARCHAR(256) NOT NULL,
    last_verified_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE direct_supplier_qualification (
    qualification_id VARCHAR(64) PRIMARY KEY,
    organization_id VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    operator_id VARCHAR(64) NOT NULL,
    operation_reason VARCHAR(256) NOT NULL,
    operation_at TIMESTAMP WITH TIME ZONE NOT NULL,
    aggregate_version BIGINT NOT NULL
);

CREATE TABLE supply_relation (
    relation_id VARCHAR(64) PRIMARY KEY,
    supplier_organization_id VARCHAR(64) NOT NULL,
    merchant_organization_id VARCHAR(64) NOT NULL,
    authorized_catalog_scope VARCHAR(256) NOT NULL,
    supply_price_rule VARCHAR(256) NOT NULL,
    settlement_rule VARCHAR(256) NOT NULL,
    status VARCHAR(32) NOT NULL,
    operator_id VARCHAR(64) NOT NULL,
    operation_reason VARCHAR(256) NOT NULL,
    operation_at TIMESTAMP WITH TIME ZONE NOT NULL,
    aggregate_version BIGINT NOT NULL
);

CREATE TABLE storefront_terminal (
    terminal_key VARCHAR(96) PRIMARY KEY,
    storefront_id VARCHAR(64) NOT NULL,
    terminal_type VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL,
    home_page_id VARCHAR(64),
    operator_id VARCHAR(64) NOT NULL,
    operation_reason VARCHAR(256) NOT NULL,
    operation_at TIMESTAMP WITH TIME ZONE NOT NULL,
    aggregate_version BIGINT NOT NULL
);

CREATE TABLE storefront_permission_binding (
    storefront_id VARCHAR(64) PRIMARY KEY,
    editor_role_ids VARCHAR(512) NOT NULL,
    submitter_role_ids VARCHAR(512) NOT NULL,
    binding_version BIGINT NOT NULL,
    operator_id VARCHAR(64) NOT NULL,
    operation_reason VARCHAR(256) NOT NULL,
    operation_at TIMESTAMP WITH TIME ZONE NOT NULL
);
