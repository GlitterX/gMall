ALTER TABLE marketing_content_slot
    ADD COLUMN IF NOT EXISTS publication_window VARCHAR(4000) NOT NULL DEFAULT '{"scheduleType":"ALWAYS_ON","timezone":"Asia/Shanghai","manualOfflineAllowed":true}';

ALTER TABLE marketing_content_slot
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 1;

ALTER TABLE marketing_content_slot
    ALTER COLUMN media_list TYPE VARCHAR(8000);

CREATE TABLE IF NOT EXISTS marketing_campaign_banner (
    campaign_id VARCHAR(64) PRIMARY KEY,
    campaign_code VARCHAR(64) NOT NULL,
    campaign_type VARCHAR(32) NOT NULL,
    owner_type VARCHAR(32) NOT NULL,
    owner_id VARCHAR(64) NOT NULL,
    title_i18n VARCHAR(4000) NOT NULL,
    sub_title_i18n VARCHAR(4000) NOT NULL,
    banner_image VARCHAR(4000) NOT NULL,
    landing_target VARCHAR(4000) NOT NULL,
    publication_window VARCHAR(4000) NOT NULL,
    campaign_status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_marketing_campaign_owner_code
    ON marketing_campaign_banner (owner_type, owner_id, campaign_code);

CREATE INDEX IF NOT EXISTS idx_marketing_campaign_owner_status
    ON marketing_campaign_banner (owner_type, owner_id, campaign_status, updated_at);

CREATE TABLE IF NOT EXISTS topic_content_block (
    topic_block_id VARCHAR(64) PRIMARY KEY,
    topic_code VARCHAR(64) NOT NULL,
    topic_type VARCHAR(32) NOT NULL,
    owner_type VARCHAR(32) NOT NULL,
    owner_id VARCHAR(64) NOT NULL,
    topic_title_i18n VARCHAR(4000) NOT NULL,
    topic_summary_i18n VARCHAR(4000) NOT NULL,
    hero_image VARCHAR(4000) NOT NULL,
    content_blocks VARCHAR(8000) NOT NULL,
    landing_target VARCHAR(4000) NOT NULL,
    publication_window VARCHAR(4000) NOT NULL,
    topic_status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_topic_content_owner_code
    ON topic_content_block (owner_type, owner_id, topic_code);

CREATE INDEX IF NOT EXISTS idx_topic_content_owner_status
    ON topic_content_block (owner_type, owner_id, topic_status, updated_at);

CREATE TABLE IF NOT EXISTS marketing_projection_cache (
    cache_id VARCHAR(64) PRIMARY KEY,
    projection_type VARCHAR(32) NOT NULL,
    owner_type VARCHAR(32) NOT NULL,
    owner_id VARCHAR(64) NOT NULL,
    terminal_type VARCHAR(16) NOT NULL,
    page_context VARCHAR(64) NOT NULL,
    locale VARCHAR(16) NOT NULL,
    resolved_locale VARCHAR(16) NOT NULL,
    fallback_applied BOOLEAN NOT NULL,
    cache_ttl_seconds INTEGER NOT NULL,
    payload VARCHAR(16000) NOT NULL,
    last_built_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_marketing_projection_cache_key
    ON marketing_projection_cache (projection_type, owner_type, owner_id, terminal_type, page_context, locale);

CREATE INDEX IF NOT EXISTS idx_marketing_projection_cache_owner
    ON marketing_projection_cache (owner_type, owner_id, projection_type, terminal_type, locale);
