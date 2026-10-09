-- V9: Phase 3M — Production Observability, Evaluation & Research Experimentation Schema

CREATE TABLE IF NOT EXISTS dataset_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    platform VARCHAR(64),
    date_from TIMESTAMPTZ,
    date_to TIMESTAMPTZ,
    comment_count INT NOT NULL DEFAULT 0,
    processed_comment_count INT NOT NULL DEFAULT 0,
    embedding_model VARCHAR(128),
    processing_version VARCHAR(64),
    clustering_version VARCHAR(64),
    snapshot_metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dataset_snapshots_user_created ON dataset_snapshots(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_dataset_snapshots_platform ON dataset_snapshots(user_id, platform);

-- Alter experiment_runs to add research experimentation fields
ALTER TABLE experiment_runs ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE experiment_runs ADD COLUMN IF NOT EXISTS baseline_mode VARCHAR(64) DEFAULT 'BASELINE';
ALTER TABLE experiment_runs ADD COLUMN IF NOT EXISTS treatment_mode VARCHAR(64) DEFAULT 'EVIDENCE_GROUNDED';
ALTER TABLE experiment_runs ADD COLUMN IF NOT EXISTS dataset_snapshot_id UUID REFERENCES dataset_snapshots(id) ON DELETE SET NULL;
ALTER TABLE experiment_runs ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(64);
ALTER TABLE experiment_runs ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_experiment_runs_user_created ON experiment_runs(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS evaluation_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    experiment_run_id UUID REFERENCES experiment_runs(id) ON DELETE CASCADE,
    target_type VARCHAR(64) NOT NULL,
    target_id UUID NOT NULL,
    generation_mode VARCHAR(64) NOT NULL,
    metrics_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evaluation_records_user_created ON evaluation_records(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_evaluation_records_user_type ON evaluation_records(user_id, target_type);
CREATE INDEX IF NOT EXISTS idx_evaluation_records_run_id ON evaluation_records(experiment_run_id);
