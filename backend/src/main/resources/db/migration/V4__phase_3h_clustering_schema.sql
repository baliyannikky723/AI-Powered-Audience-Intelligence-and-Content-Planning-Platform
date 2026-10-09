-- =============================================================================
-- PulseGPT Database Migration V4: Phase 3H Audience Clustering & Topic Modeling
-- =============================================================================

-- 1. Clustering Runs Table
CREATE TABLE IF NOT EXISTS clustering_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
    time_window_start TIMESTAMP WITH TIME ZONE,
    time_window_end TIMESTAMP WITH TIME ZONE,
    input_comment_count INT NOT NULL DEFAULT 0,
    clustered_comment_count INT NOT NULL DEFAULT 0,
    noise_count INT NOT NULL DEFAULT 0,
    cluster_count INT NOT NULL DEFAULT 0,
    algorithm VARCHAR(64) NOT NULL,
    algorithm_version VARCHAR(32),
    embedding_model VARCHAR(64),
    embedding_dimension INT,
    config JSONB,
    metrics JSONB,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_clustering_runs_user_id ON clustering_runs(user_id);
CREATE INDEX IF NOT EXISTS idx_clustering_runs_status ON clustering_runs(status);
CREATE INDEX IF NOT EXISTS idx_clustering_runs_started_at ON clustering_runs(started_at);

-- 2. Alter Topics Table to support Phase 3H Audience Intelligence metadata
ALTER TABLE topics
    ADD COLUMN IF NOT EXISTS comment_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS keyword_scores JSONB,
    ADD COLUMN IF NOT EXISTS sentiment_distribution JSONB,
    ADD COLUMN IF NOT EXISTS intent_distribution JSONB,
    ADD COLUMN IF NOT EXISTS language_distribution JSONB,
    ADD COLUMN IF NOT EXISTS platform_distribution JSONB,
    ADD COLUMN IF NOT EXISTS first_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_clustering_run_id UUID REFERENCES clustering_runs(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_topics_last_run ON topics(last_clustering_run_id);

-- 3. Topic Assignments Table
CREATE TABLE IF NOT EXISTS topic_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID REFERENCES topics(id) ON DELETE CASCADE,
    processed_comment_id UUID NOT NULL REFERENCES processed_comments(id) ON DELETE CASCADE,
    clustering_run_id UUID NOT NULL REFERENCES clustering_runs(id) ON DELETE CASCADE,
    cluster_id INT NOT NULL,
    is_noise BOOLEAN NOT NULL DEFAULT FALSE,
    membership_probability DOUBLE PRECISION,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_assignment_run_comment UNIQUE (clustering_run_id, processed_comment_id)
);

CREATE INDEX IF NOT EXISTS idx_topic_assignments_topic_id ON topic_assignments(topic_id);
CREATE INDEX IF NOT EXISTS idx_topic_assignments_comment_id ON topic_assignments(processed_comment_id);
CREATE INDEX IF NOT EXISTS idx_topic_assignments_run_id ON topic_assignments(clustering_run_id);
