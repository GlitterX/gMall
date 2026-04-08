CREATE TABLE source_supply_price (
    price_id VARCHAR(64) PRIMARY KEY,
    source_sku_id VARCHAR(64) NOT NULL,
    amount BIGINT NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    effective_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_source_supply_price_active
    ON source_supply_price (source_sku_id, status);

CREATE TABLE merchant_offer_price (
    price_id VARCHAR(64) PRIMARY KEY,
    merchant_offer_sku_id VARCHAR(64) NOT NULL,
    source_sku_id VARCHAR(64) NOT NULL,
    amount BIGINT NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    effective_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_merchant_offer_price_active
    ON merchant_offer_price (merchant_offer_sku_id, status);

CREATE TABLE inventory_balance (
    source_sku_id VARCHAR(64) PRIMARY KEY,
    available_qty BIGINT NOT NULL,
    reserved_qty BIGINT NOT NULL,
    sold_qty BIGINT NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE inventory_reservation (
    reservation_id VARCHAR(128) PRIMARY KEY,
    business_key VARCHAR(128) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    reservation_payload VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE inventory_ledger_entry (
    ledger_id VARCHAR(128) PRIMARY KEY,
    source_sku_id VARCHAR(64) NOT NULL,
    business_key VARCHAR(128) NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    delta_qty BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_inventory_ledger_source_sku
    ON inventory_ledger_entry (source_sku_id, created_at);

CREATE TABLE outbox_event (
    event_id VARCHAR(64) PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload VARCHAR(8000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);
