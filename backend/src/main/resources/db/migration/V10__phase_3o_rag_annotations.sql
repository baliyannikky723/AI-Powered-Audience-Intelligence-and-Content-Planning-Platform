-- V10: Phase 3O — Graph-Augmented RAG & Evidence Annotations Schema

CREATE TABLE IF NOT EXISTS evidence_annotations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    query_id VARCHAR(128) NOT NULL,
    evidence_id VARCHAR(128) NOT NULL,
    relevance VARCHAR(32) NOT NULL, -- RELEVANT, PARTIALLY_RELEVANT, IRRELEVANT
    correctness VARCHAR(32) NOT NULL, -- SUPPORTED, PARTIALLY_SUPPORTED, UNSUPPORTED
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evidence_annotations_user_created ON evidence_annotations(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_evidence_annotations_query_id ON evidence_annotations(query_id);
CREATE INDEX IF NOT EXISTS idx_evidence_annotations_evidence_id ON evidence_annotations(evidence_id);
