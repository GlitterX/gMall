CREATE TABLE pricing_snapshot (
    pricing_snapshot_ref VARCHAR(128) PRIMARY KEY,
    business_key VARCHAR(128) NOT NULL UNIQUE,
    total_amount BIGINT NOT NULL,
    snapshot_payload VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
