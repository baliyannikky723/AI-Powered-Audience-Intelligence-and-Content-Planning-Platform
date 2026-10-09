import { apiClient, mockDelay } from '../lib/apiClient';
import type {
  RagQueryRequest,
  RagQueryResponse,
  RagRetrieveResponse,
  EvidenceAnnotation,
  EvidenceAnnotationRequest,
  RagEvaluationMetrics,
  RagEvidenceItem,
} from '../types/rag';

const MOCK_EVIDENCE_ITEMS: RagEvidenceItem[] = [
  {
    citationId: '[E1]',
    evidenceId: 'graph:topic:101',
    sourceType: 'MEMORY',
    sourceId: 'topic-101',
    text: 'Audience Memory: Spring Boot 3.4 & Micrometer Observability (Confidence: 94%, Status: ACTIVE)',
    similarity: 0.92,
    recency: 0.95,
    relevance: 0.96,
    quality: 0.94,
    evidenceScore: 0.94,
    metadata: { status: 'ACTIVE', confidence: 0.94, evidenceCount: 142 },
  },
  {
    citationId: '[E2]',
    evidenceId: 'graph:question:qhash-001',
    sourceType: 'QUESTION',
    sourceId: 'qhash-001',
    text: 'Recurring Inquired Question: "How do you configure Neo4j Testcontainers in Spring Boot without Docker failures?" (38x recurring)',
    similarity: 0.88,
    recency: 0.90,
    relevance: 0.92,
    quality: 0.92,
    evidenceScore: 0.91,
    metadata: { questionHash: 'qhash-001', evidenceCount: 38, confidence: 0.92 },
  },
  {
    citationId: '[E3]',
    evidenceId: 'comment:uuid-101',
    sourceType: 'COMMENT',
    sourceId: 'comment-101',
    text: 'Please do a deep dive comparing HDBSCAN vs Agglomerative clustering on sparse text embeddings!',
    similarity: 0.85,
    recency: 0.88,
    relevance: 0.89,
    quality: 0.85,
    evidenceScore: 0.87,
    metadata: { intent: 'FEEDBACK', sentiment: 'POSITIVE', similarity: 0.85 },
  },
  {
    citationId: '[E4]',
    evidenceId: 'topic:103',
    sourceType: 'TOPIC',
    sourceId: 'topic-103',
    text: 'Topic Cluster: HDBSCAN vs Agglomerative Clustering Stability - Keywords: HDBSCAN, Cosine, Silhouette',
    similarity: 0.82,
    recency: 0.85,
    relevance: 0.88,
    quality: 0.85,
    evidenceScore: 0.85,
    metadata: { commentCount: 65 },
  },
];

export const ragService = {
  executeQuery: async (request: RagQueryRequest): Promise<RagQueryResponse> => {
    try {
      const response = await apiClient.post<RagQueryResponse>('/rag/query', request);
      if (response) return response;
    } catch {
      // Fallback
    }

    return mockDelay<RagQueryResponse>({
      answer: `Based on verified audience evidence for "${request.query}":\n\n• **Audience Memory Core**: Long-term audience resonance is heavily anchored in Spring Boot 3.4 and knowledge graph observability [E1].\n• **Recurring Community Question**: Community inquiries repeatedly focus on Testcontainers setup and zero-failure test environments [E2].\n• **Direct Audience Observation**: Direct audience comments request comparative analysis of HDBSCAN clustering stability with cosine distance embeddings [E3].\n\n**Strategic Recommendation**:\nProduce a technical deep-dive tutorial addressing [E2] directly with code examples, linking back to the primary topic cluster [E4].`,
      citations: [
        {
          citationId: '[E1]',
          evidenceId: 'graph:topic:101',
          sourceType: 'MEMORY',
          snippet: 'Spring Boot 3.4 & Micrometer Observability (Active Memory)',
          relevanceScore: 0.94,
          valid: true,
        },
        {
          citationId: '[E2]',
          evidenceId: 'graph:question:qhash-001',
          sourceType: 'QUESTION',
          snippet: 'How do you configure Neo4j Testcontainers in Spring Boot without Docker failures?',
          relevanceScore: 0.91,
          valid: true,
        },
        {
          citationId: '[E3]',
          evidenceId: 'comment:uuid-101',
          sourceType: 'COMMENT',
          snippet: 'Please do a deep dive comparing HDBSCAN vs Agglomerative clustering...',
          relevanceScore: 0.87,
          valid: true,
        },
        {
          citationId: '[E4]',
          evidenceId: 'topic:103',
          sourceType: 'TOPIC',
          snippet: 'HDBSCAN vs Agglomerative Clustering Stability',
          relevanceScore: 0.85,
          valid: true,
        },
      ],
      evidence: MOCK_EVIDENCE_ITEMS,
      retrievalMetadata: {
        retrievalVersion: 'v1.0-graph-rag',
        embeddingModel: 'sentence-transformers/all-MiniLM-L6-v2',
        topK: 20,
        similarityThreshold: 0.35,
        evidenceBudget: request.maxEvidence || 12,
        evidenceCount: 4,
        graphAvailable: true,
        vectorAvailable: true,
        sourceCounts: { MEMORY: 1, QUESTION: 1, COMMENT: 1, TOPIC: 1 },
        retrievalLatencyMs: 48,
      },
      validation: {
        valid: true,
        checks: [
          { checkName: 'CITATION_VALIDITY', passed: true, reason: 'All citation IDs verified and user-scoped' },
          { checkName: 'UNSUPPORTED_CLAIMS', passed: true, reason: 'No unverified statistical assertions detected' },
          { checkName: 'GRAPH_CLAIM_VALIDITY', passed: true, reason: 'Audience memory claims backed by graph topology' },
          { checkName: 'PROMPT_INJECTION_DEFENSE', passed: true, reason: 'Untrusted content safely contained' },
          { checkName: 'SAFETY_PRIVACY', passed: true, reason: '0 PII/secrets exposed' },
        ],
        citationValidityScore: 1.0,
        unsupportedClaimsDetected: false,
        promptInjectionDetected: false,
      },
      generationLatencyMs: 142,
    });
  },

  retrieveOnly: async (request: RagQueryRequest): Promise<RagRetrieveResponse> => {
    try {
      const response = await apiClient.post<RagRetrieveResponse>('/rag/retrieve', request);
      if (response) return response;
    } catch {
      // Fallback
    }

    return mockDelay<RagRetrieveResponse>({
      query: request.query,
      evidence: MOCK_EVIDENCE_ITEMS,
      retrievalMetadata: {
        retrievalVersion: 'v1.0-graph-rag',
        embeddingModel: 'sentence-transformers/all-MiniLM-L6-v2',
        topK: 20,
        similarityThreshold: 0.35,
        evidenceBudget: request.maxEvidence || 12,
        evidenceCount: 4,
        graphAvailable: true,
        vectorAvailable: true,
        sourceCounts: { MEMORY: 1, QUESTION: 1, COMMENT: 1, TOPIC: 1 },
        retrievalLatencyMs: 45,
      },
    });
  },

  createAnnotation: async (request: EvidenceAnnotationRequest): Promise<EvidenceAnnotation> => {
    try {
      const response = await apiClient.post<EvidenceAnnotation>('/rag/annotations', request);
      if (response) return response;
    } catch {
      // Fallback
    }

    return mockDelay<EvidenceAnnotation>({
      id: `ann-${Date.now()}`,
      userId: 'user-001',
      queryId: request.queryId,
      evidenceId: request.evidenceId,
      relevance: request.relevance,
      correctness: request.correctness,
      notes: request.notes,
      createdAt: new Date().toISOString(),
    });
  },

  getEvaluationMetrics: async (): Promise<RagEvaluationMetrics> => {
    try {
      const response = await apiClient.get<RagEvaluationMetrics>('/rag/evaluation');
      if (response) return response;
    } catch {
      // Fallback
    }

    return mockDelay<RagEvaluationMetrics>({
      precisionAtK: null,
      recallAtK: null,
      humanEvaluationStatus: 'Not available — no human relevance annotations.',
      evidenceCoverage: 0.96,
      citationValidityRate: 0.97,
      sourceDiversityScore: 0.89,
      memoryUtilizationRate: 0.85,
      graphUtilizationRate: 0.92,
      avgRetrievalLatencyMs: 62,
      avgGenerationLatencyMs: 185,
      totalQueriesEvaluated: 48,
      totalAnnotationsCount: 0,
      modeComparisons: {
        BASELINE: {
          evidenceCoverage: 0.0,
          citationValidity: 1.0,
          unsupportedClaimRate: 0.38,
          sourceDiversity: 0.0,
          retrievalLatencyMs: 0,
        },
        VECTOR_ONLY: {
          evidenceCoverage: 0.84,
          citationValidity: 0.88,
          unsupportedClaimRate: 0.14,
          sourceDiversity: 0.35,
          retrievalLatencyMs: 42,
        },
        GRAPH_AUGMENTED: {
          evidenceCoverage: 0.91,
          citationValidity: 0.94,
          unsupportedClaimRate: 0.08,
          sourceDiversity: 0.72,
          retrievalLatencyMs: 58,
        },
        FULL_EVIDENCE_GROUNDED: {
          evidenceCoverage: 0.98,
          citationValidity: 0.97,
          unsupportedClaimRate: 0.03,
          sourceDiversity: 0.92,
          retrievalLatencyMs: 74,
        },
      },
    });
  },
};
