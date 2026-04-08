CREATE TABLE trade_cart_item (
    cart_item_id VARCHAR(64) PRIMARY KEY,
    buyer_id VARCHAR(64) NOT NULL,
    sku_id VARCHAR(64) NOT NULL,
    seller_id VARCHAR(64) NOT NULL,
    storefront_id VARCHAR(64) NOT NULL,
    quantity INTEGER NOT NULL,
    selected BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_trade_cart_buyer_sku_store
    ON trade_cart_item (buyer_id, sku_id, seller_id, storefront_id);

CREATE INDEX idx_trade_cart_buyer_updated
    ON trade_cart_item (buyer_id, updated_at DESC);
