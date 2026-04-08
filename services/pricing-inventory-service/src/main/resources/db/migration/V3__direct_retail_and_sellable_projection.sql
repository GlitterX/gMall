CREATE TABLE direct_retail_price (
    price_id VARCHAR(64) PRIMARY KEY,
    source_sku_id VARCHAR(64) NOT NULL,
    amount BIGINT NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    effective_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_direct_retail_price_active
    ON direct_retail_price (source_sku_id, status);

CREATE TABLE pricing_inventory_sellable_projection (
    sellable_id VARCHAR(128) PRIMARY KEY,
    business_sku_type VARCHAR(32) NOT NULL,
    business_sku_id VARCHAR(64) NOT NULL,
    source_sku_id VARCHAR(64) NOT NULL,
    sellable_state VARCHAR(32) NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_sellable_projection_business_sku
    ON pricing_inventory_sellable_projection (business_sku_type, business_sku_id);
