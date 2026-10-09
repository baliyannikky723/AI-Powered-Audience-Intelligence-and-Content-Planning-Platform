-- =============================================================================
-- PulseGPT Database Migration V6: Phase 3J Content Calendar & Planning Management
-- =============================================================================

-- 1. Alter Content Recommendations Table for Creator Approval Audit
ALTER TABLE content_recommendations
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS approved_by UUID REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_recommendations_approved_at ON content_recommendations(approved_at);

-- 2. Alter Calendar Items Table for Rich Planning and Evidence Grounding
ALTER TABLE calendar_items
    ADD COLUMN IF NOT EXISTS topic_id UUID REFERENCES topics(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS scheduled_start TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS scheduled_end TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS priority VARCHAR(32) DEFAULT 'MEDIUM',
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP WITH TIME ZONE;

-- 3. Backfill scheduled_start and scheduled_end from existing scheduled_at if present
UPDATE calendar_items
SET scheduled_start = scheduled_at
WHERE scheduled_start IS NULL AND scheduled_at IS NOT NULL;

UPDATE calendar_items
SET scheduled_end = scheduled_start + INTERVAL '1 hour'
WHERE scheduled_end IS NULL AND scheduled_start IS NOT NULL;

-- 4. Indexes for Efficient Calendar Querying and Scheduling Conflict Detection
CREATE INDEX IF NOT EXISTS idx_calendar_items_user_scheduled_start ON calendar_items(user_id, scheduled_start);
CREATE INDEX IF NOT EXISTS idx_calendar_items_user_platform ON calendar_items(user_id, platform);
CREATE INDEX IF NOT EXISTS idx_calendar_items_topic_id ON calendar_items(topic_id);
CREATE INDEX IF NOT EXISTS idx_calendar_items_recommendation_id ON calendar_items(recommendation_id);
CREATE INDEX IF NOT EXISTS idx_calendar_items_priority ON calendar_items(priority);
