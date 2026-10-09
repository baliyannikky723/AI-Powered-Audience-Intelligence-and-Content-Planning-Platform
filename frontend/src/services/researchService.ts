import { apiClient, mockDelay } from '../lib/apiClient';
import type {
  ExperimentRun,
  CreateExperimentRequest,
  DatasetSnapshot,
  CreateSnapshotRequest,
  ClusteringStabilityReport,
  ResearchMetricsResponse,
} from '../types/models';

let inMemoryExperiments: ExperimentRun[] = [
  {
    id: 'exp-run-001',
    experimentName: 'Clustering Stability: HDBSCAN vs Agglomerative',
    experimentType: 'CLUSTERING_STABILITY',
    description: 'Evaluating cluster centroid shift across 5 random seeds on multi-platform YouTube/Reddit dataset.',
    baselineMode: 'HDBSCAN_EUCLIDEAN_3',
    treatmentMode: 'AGGLOMERATIVE_COSINE_3',
    datasetSnapshotId: 'snap-001',
    promptVersion: 'N/A',
    modelName: 'all-MiniLM-L6-v2',
    modelVersion: 'v1.0.0',
    randomSeed: 42,
    status: 'COMPLETED',
    sampleCount: 1540,
    parameters: { minClusterSize: 3, metric: 'euclidean', clusterSelectionEpsilon: 0.2 },
    metricsSummary: {
      stabilityScore: 0.884,
      matchedClustersCount: 14,
      unmatchedClustersCount: 2,
      averageCentroidDistance: 0.116,
    },
    reproducibilityMetadata: {
      embeddingModel: 'all-MiniLM-L6-v2',
      embeddingDimension: 384,
      randomSeed: 42,
    },
    startedAt: new Date(Date.now() - 86400000).toISOString(),
    completedAt: new Date(Date.now() - 85200000).toISOString(),
    createdAt: new Date(Date.now() - 86400000).toISOString(),
  },
  {
    id: 'exp-run-002',
    experimentName: 'Recommendation Grounding: Baseline vs Evidence-Grounded',
    experimentType: 'BASELINE_VS_EVIDENCE_GROUNDED',
    description: 'A/B benchmark comparing LLM hallucinations and claim grounding rates with and without Audience Evidence Snapshots.',
    baselineMode: 'BASELINE_UNGROUNDED',
    treatmentMode: 'EVIDENCE_GROUNDED',
    datasetSnapshotId: 'snap-001',
    promptVersion: 'PROMPT_V1',
    modelName: 'gemini-1.5-flash',
    modelVersion: '2026.1',
    randomSeed: 1337,
    status: 'COMPLETED',
    sampleCount: 250,
    metricsSummary: {
      baselineEvidenceCoverage: 0.32,
      treatmentEvidenceCoverage: 0.94,
      baselineValidationPassRate: 0.64,
      treatmentValidationPassRate: 0.98,
      hallucinationReductionPct: 68.5,
    },
    reproducibilityMetadata: {
      temperature: 0.2,
      topP: 0.95,
      promptVersion: 'PROMPT_V1',
    },
    startedAt: new Date(Date.now() - 43200000).toISOString(),
    completedAt: new Date(Date.now() - 41800000).toISOString(),
    createdAt: new Date(Date.now() - 43200000).toISOString(),
  },
  {
    id: 'exp-run-003',
    experimentName: 'Production Copilot: Multi-Format Script Outline Evaluation',
    experimentType: 'PRODUCTION_COPILOT',
    description: 'Automated 6-point verification benchmark and repair loop effectiveness on technical tutorials.',
    baselineMode: 'SINGLE_PASS_RAW',
    treatmentMode: 'VERIFY_AND_REPAIR_LOOP',
    datasetSnapshotId: 'snap-002',
    promptVersion: 'PRODUCTION_PROMPT_V1',
    modelName: 'gemini-1.5-flash',
    modelVersion: '2026.1',
    randomSeed: 99,
    status: 'RUNNING',
    sampleCount: 100,
    metricsSummary: {
      initialPassRate: 0.81,
      postRepairPassRate: 0.97,
      repairSuccessRate: 0.84,
      creatorEditRate: 0.12,
    },
    reproducibilityMetadata: {
      temperature: 0.3,
      promptVersion: 'PRODUCTION_PROMPT_V1',
    },
    startedAt: new Date(Date.now() - 3600000).toISOString(),
    createdAt: new Date(Date.now() - 3600000).toISOString(),
  },
];

let inMemorySnapshots: DatasetSnapshot[] = [
  {
    id: 'snap-001',
    name: 'Tech & Hardware Multi-Platform Corpus Q1-2026',
    description: 'Cleaned corpus of YouTube comments & Reddit r/hardware discussions across 12 product launches.',
    platform: 'MULTI_PLATFORM',
    dateFrom: '2026-01-01',
    dateTo: '2026-03-31',
    commentCount: 4820,
    clusterCount: 38,
    recommendationCount: 114,
    metadataJson: {
      platforms: ['YOUTUBE', 'REDDIT'],
      languageBreakdown: { en: 0.88, hi: 0.08, 'hi-Latn': 0.04 },
      anonymizedPii: true,
    },
    createdAt: new Date(Date.now() - 7 * 86400000).toISOString(),
  },
  {
    id: 'snap-002',
    name: 'AI Engineering & Developer Tutorials Corpus',
    description: 'Developer feedback on agents, vector DBs, and LLM orchestration tools.',
    platform: 'YOUTUBE',
    dateFrom: '2026-02-01',
    dateTo: '2026-04-01',
    commentCount: 2950,
    clusterCount: 22,
    recommendationCount: 66,
    metadataJson: {
      platforms: ['YOUTUBE'],
      languageBreakdown: { en: 0.96, hi: 0.04 },
      anonymizedPii: true,
    },
    createdAt: new Date(Date.now() - 3 * 86400000).toISOString(),
  },
];

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}

export const researchService = {
  getMetrics: async (): Promise<ResearchMetricsResponse> => {
    try {
      const response = await apiClient.get<ApiResponse<ResearchMetricsResponse>>('/research/metrics');
      if (response && response.data) return response.data;
    } catch {
      // Fallback to rich mock data
    }
    return mockDelay<ResearchMetricsResponse>({
      systemMetrics: {
        jvmUptimeSec: 14280,
        availableProcessors: 8,
        freeMemoryMb: 512,
        totalMemoryMb: 1024,
        maxMemoryMb: 2048,
        httpRequests24h: 18450,
        avgLatencyMs: 142.5,
        p95LatencyMs: 385.0,
        p99LatencyMs: 820.0,
      },
      nlpMetrics: {
        status: 'OPTIMAL',
        supportedLanguages: ['en', 'hi', 'hi-Latn'],
        embeddingModel: 'all-MiniLM-L6-v2',
        embeddingDimension: 384,
        avgInferenceMs: 18.2,
        throughputPerSec: 145,
      },
      clusteringMetrics: {
        algorithm: 'HDBSCAN',
        metric: 'euclidean',
        minClusterSize: 3,
        clusterSelectionEpsilon: 0.2,
        avgSilhouetteScore: 0.73,
        stabilityScore: 0.884,
        noiseRatio: 0.065,
      },
      recommendationMetrics: {
        totalEvaluated: 142,
        averageEvidenceCoverageScore: 0.942,
        validationPassRate: 0.965,
        averageNoveltyScore: 0.812,
        averageLatencyMs: 420.0,
      },
      productionMetrics: {
        totalEvaluated: 88,
        validationPassRate: 0.954,
        repairSuccessRate: 0.882,
        creatorEditRate: 0.145,
        averageApprovalTimeHours: 3.4,
        averageRevisionsPerAsset: 1.15,
      },
      creatorWorkflowMetrics: {
        avgRevisionsPerAsset: 1.15,
        creatorEditRate: 0.145,
        approvalRate: 0.912,
        rejectionRate: 0.042,
        archiveRate: 0.046,
      },
      exportMetrics: {
        supportedFormatsCount: 7,
        pdfEngine: 'OpenPDF',
        deterministicExport: true,
        totalExportsGenerated: 320,
        avgExportDurationMs: 64.0,
      },
      reproducibilityMetadata: {
        modelName: 'gemini-1.5-flash',
        modelVersion: '2026.1',
        promptVersion: 'PROMPT_V1',
        algorithmName: 'HDBSCAN',
        algorithmVersion: 'v0.8.38',
        embeddingModel: 'all-MiniLM-L6-v2',
        embeddingDimension: 384,
        randomSeed: 42,
      },
    });
  },

  getExperiments: async (): Promise<ExperimentRun[]> => {
    try {
      const response = await apiClient.get<ApiResponse<{ content: ExperimentRun[] } | ExperimentRun[]>>('/research/experiments');
      if (response && response.data) {
        if (Array.isArray(response.data)) return response.data;
        if ('content' in response.data && Array.isArray(response.data.content)) return response.data.content;
      }
    } catch {
      // Fallback
    }
    return mockDelay<ExperimentRun[]>([...inMemoryExperiments]);
  },

  getExperimentById: async (id: string): Promise<ExperimentRun> => {
    try {
      const response = await apiClient.get<ApiResponse<ExperimentRun>>(`/research/experiments/${id}`);
      if (response && response.data) return response.data;
    } catch {
      // Fallback
    }
    const found = inMemoryExperiments.find(e => e.id === id);
    if (found) return mockDelay(found);
    throw new Error('Experiment not found');
  },

  createExperiment: async (request: CreateExperimentRequest): Promise<ExperimentRun> => {
    try {
      const response = await apiClient.post<ApiResponse<ExperimentRun>>('/research/experiments', request);
      if (response && response.data) return response.data;
    } catch {
      // Fallback
    }
    const newRun: ExperimentRun = {
      id: `exp-run-${Date.now()}`,
      experimentName: request.experimentName,
      experimentType: request.experimentType,
      description: request.description,
      baselineMode: request.baselineMode,
      treatmentMode: request.treatmentMode,
      datasetSnapshotId: request.datasetSnapshotId,
      status: 'CREATED',
      sampleCount: 0,
      parameters: request.parameters,
      reproducibilityMetadata: {
        embeddingModel: 'all-MiniLM-L6-v2',
        embeddingDimension: 384,
        randomSeed: 42,
      },
      createdAt: new Date().toISOString(),
    };
    inMemoryExperiments.unshift(newRun);
    return mockDelay(newRun);
  },

  startExperiment: async (id: string): Promise<ExperimentRun> => {
    try {
      const response = await apiClient.post<ApiResponse<ExperimentRun>>(`/research/experiments/${id}/start`);
      if (response && response.data) return response.data;
    } catch {
      // Fallback
    }
    const idx = inMemoryExperiments.findIndex(e => e.id === id);
    if (idx !== -1) {
      inMemoryExperiments[idx].status = 'RUNNING';
      inMemoryExperiments[idx].startedAt = new Date().toISOString();
      return mockDelay(inMemoryExperiments[idx]);
    }
    throw new Error('Experiment not found');
  },

  completeExperiment: async (id: string, metrics?: Record<string, any>): Promise<ExperimentRun> => {
    try {
      const response = await apiClient.post<ApiResponse<ExperimentRun>>(`/research/experiments/${id}/complete`, { metrics });
      if (response && response.data) return response.data;
    } catch {
      // Fallback
    }
    const idx = inMemoryExperiments.findIndex(e => e.id === id);
    if (idx !== -1) {
      inMemoryExperiments[idx].status = 'COMPLETED';
      inMemoryExperiments[idx].completedAt = new Date().toISOString();
      if (metrics) {
        inMemoryExperiments[idx].metricsSummary = metrics;
      }
      return mockDelay(inMemoryExperiments[idx]);
    }
    throw new Error('Experiment not found');
  },

  getClusteringStability: async (runId: string, compareRunId?: string): Promise<ClusteringStabilityReport> => {
    try {
      const response = await apiClient.get<ApiResponse<ClusteringStabilityReport>>(`/research/clustering/${runId}/stability`, {
        params: compareRunId ? { compareRunId } : undefined,
      });
      if (response && response.data) return response.data;
    } catch {
      // Fallback
    }
    return mockDelay<ClusteringStabilityReport>({
      stabilityScore: 0.884,
      matchedClustersCount: 14,
      unmatchedClustersCount: 2,
      averageCentroidDistance: 0.116,
      clusterPairSimilarities: [
        { runClusterId: 'cluster-1', baselineClusterId: 'base-1', cosineSimilarity: 0.982 },
        { runClusterId: 'cluster-2', baselineClusterId: 'base-2', cosineSimilarity: 0.945 },
        { runClusterId: 'cluster-3', baselineClusterId: 'base-3', cosineSimilarity: 0.912 },
        { runClusterId: 'cluster-4', baselineClusterId: 'base-4', cosineSimilarity: 0.889 },
        { runClusterId: 'cluster-5', baselineClusterId: 'base-5', cosineSimilarity: 0.865 },
        { runClusterId: 'cluster-6', baselineClusterId: 'base-6', cosineSimilarity: 0.852 },
      ],
    });
  },

  getSnapshots: async (): Promise<DatasetSnapshot[]> => {
    try {
      const response = await apiClient.get<ApiResponse<{ content: DatasetSnapshot[] } | DatasetSnapshot[]>>('/research/snapshots');
      if (response && response.data) {
        if (Array.isArray(response.data)) return response.data;
        if ('content' in response.data && Array.isArray(response.data.content)) return response.data.content;
      }
    } catch {
      // Fallback
    }
    return mockDelay<DatasetSnapshot[]>([...inMemorySnapshots]);
  },

  createSnapshot: async (request: CreateSnapshotRequest): Promise<DatasetSnapshot> => {
    try {
      const response = await apiClient.post<ApiResponse<DatasetSnapshot>>('/research/snapshots', request);
      if (response && response.data) return response.data;
    } catch {
      // Fallback
    }
    const newSnapshot: DatasetSnapshot = {
      id: `snap-${Date.now()}`,
      name: request.name,
      description: request.description,
      platform: request.platform || 'MULTI_PLATFORM',
      dateFrom: request.dateFrom,
      dateTo: request.dateTo,
      commentCount: Math.floor(Math.random() * 2000) + 1000,
      clusterCount: Math.floor(Math.random() * 15) + 10,
      recommendationCount: Math.floor(Math.random() * 30) + 20,
      createdAt: new Date().toISOString(),
    };
    inMemorySnapshots.unshift(newSnapshot);
    return mockDelay(newSnapshot);
  },
};
