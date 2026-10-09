export type RagMode = 'BASELINE' | 'VECTOR_ONLY' | 'GRAPH_AUGMENTED' | 'FULL_EVIDENCE_GROUNDED' | 'EVIDENCE_GROUNDED';

export type EvidenceSourceType = 'COMMENT' | 'TOPIC' | 'QUESTION' | 'TREND' | 'MEMORY' | 'CONTENT_HISTORY';

export type RagRelevance = 'RELEVANT' | 'PARTIALLY_RELEVANT' | 'IRRELEVANT';
export type RagCorrectness = 'SUPPORTED' | 'PARTIALLY_SUPPORTED' | 'UNSUPPORTED';

export interface RagEvidenceItem {
  citationId: string;
  evidenceId: string;
  sourceType: EvidenceSourceType;
  sourceId: string;
  text: string;
  similarity: number;
  recency: number;
  relevance: number;
  quality: number;
  evidenceScore: number;
  metadata?: Record<string, any>;
  createdAt?: string;
}

export interface RagCitationDto {
  citationId: string;
  evidenceId: string;
  sourceType: EvidenceSourceType;
  snippet: string;
  relevanceScore: number;
  valid: boolean;
}

export interface RagValidationCheckResult {
  checkName: string;
  passed: boolean;
  reason: string;
}

export interface RagValidationResult {
  valid: boolean;
  checks: RagValidationCheckResult[];
  failureSummary?: string;
  citationValidityScore: number;
  unsupportedClaimsDetected: boolean;
  promptInjectionDetected: boolean;
}

export interface RagRetrievalMetadata {
  retrievalVersion: string;
  embeddingModel: string;
  topK: number;
  similarityThreshold: number;
  evidenceBudget: number;
  evidenceCount: number;
  graphAvailable: boolean;
  vectorAvailable: boolean;
  sourceCounts: Record<string, number>;
  retrievalLatencyMs: number;
}

export interface RagQueryRequest {
  query: string;
  generationMode?: RagMode;
  platform?: string;
  topicId?: string;
  maxEvidence?: number;
  timeRangeDays?: number;
}

export interface RagQueryResponse {
  answer: string;
  citations: RagCitationDto[];
  evidence: RagEvidenceItem[];
  retrievalMetadata: RagRetrievalMetadata;
  validation: RagValidationResult;
  generationLatencyMs: number;
}

export interface RagRetrieveResponse {
  query: string;
  evidence: RagEvidenceItem[];
  retrievalMetadata: RagRetrievalMetadata;
}

export interface EvidenceAnnotation {
  id: string;
  userId: string;
  queryId: string;
  evidenceId: string;
  relevance: RagRelevance;
  correctness: RagCorrectness;
  notes?: string;
  createdAt: string;
}

export interface EvidenceAnnotationRequest {
  queryId: string;
  evidenceId: string;
  relevance: RagRelevance;
  correctness: RagCorrectness;
  notes?: string;
}

export interface RagEvaluationMetrics {
  precisionAtK?: number | null;
  recallAtK?: number | null;
  humanEvaluationStatus: string;
  evidenceCoverage: number;
  citationValidityRate: number;
  sourceDiversityScore: number;
  memoryUtilizationRate: number;
  graphUtilizationRate: number;
  avgRetrievalLatencyMs: number;
  avgGenerationLatencyMs: number;
  totalQueriesEvaluated: number;
  totalAnnotationsCount: number;
  modeComparisons: Record<string, {
    evidenceCoverage: number;
    citationValidity: number;
    unsupportedClaimRate: number;
    sourceDiversity: number;
    retrievalLatencyMs: number;
  }>;
}
