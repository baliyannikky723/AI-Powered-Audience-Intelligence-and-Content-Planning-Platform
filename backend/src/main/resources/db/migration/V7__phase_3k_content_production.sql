-- =============================================================================
-- PulseGPT Database Migration V7: Phase 3K Content Scripting & Production Copilot
-- =============================================================================

CREATE TABLE IF NOT EXISTS content_production_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recommendation_id UUID NOT NULL REFERENCES content_recommendations(id) ON DELETE CASCADE,
    calendar_item_id UUID REFERENCES calendar_items(id) ON DELETE SET NULL,
    asset_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'GENERATED',
    generation_mode VARCHAR(32) NOT NULL DEFAULT 'EVIDENCE_GROUNDED',
    version INT NOT NULL DEFAULT 1,
    prompt_version VARCHAR(32),
    model_name VARCHAR(64),
    content_json JSONB NOT NULL,
    evidence_snapshot JSONB,
    validation_json JSONB,
    revision_history JSONB,
    repair_attempted BOOLEAN NOT NULL DEFAULT FALSE,
    repair_result_json JSONB,
    approved_at TIMESTAMP WITH TIME ZONE,
    approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for efficient query access and user isolation
CREATE INDEX IF NOT EXISTS idx_prod_assets_user_id ON content_production_assets(user_id);
CREATE INDEX IF NOT EXISTS idx_prod_assets_recommendation_id ON content_production_assets(recommendation_id);
CREATE INDEX IF NOT EXISTS idx_prod_assets_calendar_item_id ON content_production_assets(calendar_item_id);
CREATE INDEX IF NOT EXISTS idx_prod_assets_asset_type ON content_production_assets(asset_type);
CREATE INDEX IF NOT EXISTS idx_prod_assets_status ON content_production_assets(status);
CREATE INDEX IF NOT EXISTS idx_prod_assets_created_at ON content_production_assets(created_at);
CREATE INDEX IF NOT EXISTS idx_prod_assets_user_rec ON content_production_assets(user_id, recommendation_id);
