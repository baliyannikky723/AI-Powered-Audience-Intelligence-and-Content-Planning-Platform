-- =============================================================================
-- PulseGPT Database Migration V5: Phase 3I Evidence-Grounded Recommendation Engine
-- =============================================================================

-- 1. Alter Content Recommendations Table for Phase 3I Evidence Grounding
ALTER TABLE content_recommendations
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(64) DEFAULT 'VIDEO',
    ADD COLUMN IF NOT EXISTS angle TEXT,
    ADD COLUMN IF NOT EXISTS problem_addressed TEXT,
    ADD COLUMN IF NOT EXISTS hook TEXT,
    ADD COLUMN IF NOT EXISTS call_to_action TEXT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'GENERATED',
    ADD COLUMN IF NOT EXISTS generation_mode VARCHAR(32) NOT NULL DEFAULT 'EVIDENCE_GROUNDED',
    ADD COLUMN IF NOT EXISTS confidence DOUBLE PRECISION DEFAULT 0.85,
    ADD COLUMN IF NOT EXISTS evidence_snapshot JSONB,
    ADD COLUMN IF NOT EXISTS draft_json JSONB,
    ADD COLUMN IF NOT EXISTS validation_json JSONB,
    ADD COLUMN IF NOT EXISTS repair_attempted BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS repair_result_json JSONB;

-- 2. Indexes for Content Recommendations
CREATE INDEX IF NOT EXISTS idx_recommendations_status ON content_recommendations(status);
CREATE INDEX IF NOT EXISTS idx_recommendations_gen_mode ON content_recommendations(generation_mode);
CREATE INDEX IF NOT EXISTS idx_recommendations_user_created ON content_recommendations(user_id, created_at DESC);
