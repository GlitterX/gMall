ALTER TABLE eligibility_projection
    ADD COLUMN organization_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN seller_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN storefront_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN organization_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN seller_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN storefront_version BIGINT NOT NULL DEFAULT 0;
