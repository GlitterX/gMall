CREATE TABLE eligibility_projection (
    projection_key VARCHAR(160) PRIMARY KEY,
    seller_id VARCHAR(64) NOT NULL,
    storefront_id VARCHAR(64) NOT NULL,
    organization_id VARCHAR(64) NOT NULL,
    eligible BOOLEAN NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX idx_eligibility_projection_seller_store
    ON eligibility_projection (seller_id, storefront_id);

CREATE TABLE trade_order (
    order_id VARCHAR(64) PRIMARY KEY,
    order_no VARCHAR(128) NOT NULL UNIQUE,
    business_idempotency_key VARCHAR(128) NOT NULL,
    buyer_id VARCHAR(64) NOT NULL,
    seller_id VARCHAR(64) NOT NULL,
    seller_type VARCHAR(32) NOT NULL,
    seller_of_record VARCHAR(64) NOT NULL,
    settlement_beneficiary VARCHAR(64) NOT NULL,
    order_scene VARCHAR(32) NOT NULL,
    storefront_id VARCHAR(64) NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    total_amount BIGINT NOT NULL,
    freight_amount BIGINT NOT NULL,
    discount_amount BIGINT NOT NULL,
    payable_amount BIGINT NOT NULL,
    address_snapshot VARCHAR(4000) NOT NULL,
    pricing_snapshot_ref VARCHAR(128) NOT NULL,
    inventory_reservation_ref VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_trade_order_business_key
    ON trade_order (business_idempotency_key);

CREATE TABLE trade_sub_order (
    sub_order_id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    seller_id VARCHAR(64) NOT NULL,
    storefront_id VARCHAR(64) NOT NULL,
    order_item_snapshot VARCHAR(4000) NOT NULL
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
