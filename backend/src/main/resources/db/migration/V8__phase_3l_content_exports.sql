-- =============================================================================
-- PulseGPT Database Migration V8: Phase 3L Multi-Format Content Exporter & Production Packager
-- =============================================================================

CREATE TABLE IF NOT EXISTS content_exports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    production_asset_id UUID NOT NULL REFERENCES content_production_assets(id) ON DELETE CASCADE,
    export_type VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    content_hash VARCHAR(128) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    file_size_bytes BIGINT,
    metadata_json JSONB,
    content_data BYTEA,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE
);

-- Indexes for efficient queries and user isolation
CREATE INDEX IF NOT EXISTS idx_content_exports_user_id ON content_exports(user_id);
CREATE INDEX IF NOT EXISTS idx_content_exports_user_created ON content_exports(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_content_exports_user_asset ON content_exports(user_id, production_asset_id);
CREATE INDEX IF NOT EXISTS idx_content_exports_user_type ON content_exports(user_id, export_type);
