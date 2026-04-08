CREATE TABLE catalog_source_product (
    source_product_id VARCHAR(64) PRIMARY KEY,
    product_view_id VARCHAR(64) NOT NULL UNIQUE,
    owner_type VARCHAR(32) NOT NULL,
    owner_id VARCHAR(64) NOT NULL,
    source_mode VARCHAR(32) NOT NULL,
    category_id VARCHAR(64) NOT NULL,
    brand_id VARCHAR(64),
    product_content VARCHAR(4000) NOT NULL,
    product_status VARCHAR(32) NOT NULL,
    content_version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE catalog_source_sku (
    source_sku_id VARCHAR(64) PRIMARY KEY,
    source_product_id VARCHAR(64) NOT NULL,
    spec_signature VARCHAR(256) NOT NULL,
    spec_payload VARCHAR(4000) NOT NULL,
    sku_status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL
);

CREATE UNIQUE INDEX uk_catalog_source_sku_signature
    ON catalog_source_sku (source_product_id, spec_signature);

CREATE TABLE catalog_merchant_offer_product (
    merchant_offer_product_id VARCHAR(64) PRIMARY KEY,
    product_view_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id VARCHAR(64) NOT NULL,
    relation_id VARCHAR(64) NOT NULL,
    source_product_id VARCHAR(64) NOT NULL,
    offer_content VARCHAR(4000) NOT NULL,
    offer_status VARCHAR(32) NOT NULL,
    sync_confirmation_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_offer_relation_source
    ON catalog_merchant_offer_product (merchant_id, relation_id, source_product_id);

CREATE TABLE catalog_merchant_offer_sku (
    merchant_offer_sku_id VARCHAR(64) PRIMARY KEY,
    merchant_offer_product_id VARCHAR(64) NOT NULL,
    source_sku_id VARCHAR(64) NOT NULL,
    offer_sku_name VARCHAR(4000) NOT NULL,
    offer_sku_status VARCHAR(32) NOT NULL,
    mapping_version BIGINT NOT NULL
);

CREATE UNIQUE INDEX uk_offer_source_sku
    ON catalog_merchant_offer_sku (merchant_offer_product_id, source_sku_id);

CREATE TABLE catalog_projection (
    projection_id VARCHAR(64) PRIMARY KEY,
    presentation_type VARCHAR(32) NOT NULL,
    business_product_id VARCHAR(64) NOT NULL,
    source_product_id VARCHAR(64),
    merchant_offer_product_id VARCHAR(64),
    product_view_id VARCHAR(64) NOT NULL,
    locale VARCHAR(16) NOT NULL,
    projection_payload VARCHAR(4000) NOT NULL,
    projection_status VARCHAR(32) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_projection_type_view_locale
    ON catalog_projection (presentation_type, product_view_id, locale);

CREATE TABLE catalog_outbox_event (
    event_id VARCHAR(64) PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    aggregate_version BIGINT NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload VARCHAR(8000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);
