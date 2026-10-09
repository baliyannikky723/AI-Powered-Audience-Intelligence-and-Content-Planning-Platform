-- =============================================================================
-- PulseGPT Database Migration V1: Baseline Schema Initialization
-- =============================================================================

-- Enable required PostgreSQL extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Vector extension (optional/graceful check for pgvector compatibility)
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS vector;
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'pgvector extension could not be enabled automatically. Ensure pgvector is installed if vector embeddings are enabled.';
END
$$;

-- Baseline Schema Metadata Tracking Table
CREATE TABLE IF NOT EXISTS schema_baseline_info (
    id VARCHAR(64) PRIMARY KEY,
    initialized_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(32) NOT NULL,
    description TEXT
);

INSERT INTO schema_baseline_info (id, status, description)
VALUES ('V1', 'INITIALIZED', 'PulseGPT Spring Boot 3.x Baseline Migration Foundation')
ON CONFLICT (id) DO NOTHING;
