// Domain model types for Audience Intelligence and Planning
// Audience Intelligence
export interface AudienceSegment {
  id: string;
  name: string;
  percentage: number;
  sentimentScore: number;
  engagementRate: number;
  topInterests: string[];
  keyPainPoints: string[];
  growth: number;
}

export interface AudienceOverview {
  totalAudience: number;
  activeFollowers: number;
  avgEngagementRate: number;
  netSentimentScore: number;
  segments: AudienceSegment[];
  demographics: {
    ageGroups: { label: string; value: number }[];
    locations: { country: string; percentage: number }[];
    topPlatforms: { platform: string; users: number; percentage: number }[];
  };
  sentimentTrends: { date: string; positive: number; neutral: number; negative: number }[];
}

// Trends & Viral Signals
export interface TrendSignal {
  id: string;
  topic: string;
  category: 'tech' | 'design' | 'ai' | 'career' | 'business';
  velocity: number; // 0 - 100
  volume: number;
  sentiment: 'positive' | 'neutral' | 'negative';
  platform: string;
  recommendedAngle: string;
  urgency: 'high' | 'medium' | 'low';
  createdAt: string;
}

// Audience Memory & Long-term Context
export interface AudienceMemoryItem {
  id: string;
  category: 'recurring_question' | 'feature_request' | 'criticism' | 'praise' | 'persona_insight';
  title: string;
  summary: string;
  mentionCount: number;
  sourcePlatforms: string[];
  firstObserved: string;
  lastObserved: string;
  confidenceScore: number;
  actionTaken?: string;
}

// AI Content Recommendations
export interface ContentRecommendation {
  id: string;
  topicId?: string;
  topicName?: string;
  title: string;
  hook: string;
  description?: string;
  targetPlatform?: string;
  format?: 'short_video' | 'carousel' | 'long_post' | 'thread' | 'newsletter' | string;
  contentType?: string;
  angle?: string;
  targetAudience?: string;
  problemAddressed?: string;
  callToAction?: string;
  keyPoints?: string[];
  rationale?: string;
  reason?: string;
  projectedEngagement?: number;
  confidence?: number;
  targetSegment?: string;
  status: 'new' | 'saved' | 'in_planner' | 'dismissed' | 'GENERATED' | 'VALIDATED' | 'REJECTED' | 'APPROVED';
  recommendationStatus?: 'GENERATED' | 'VALIDATED' | 'REJECTED' | 'APPROVED';
  validationPassed?: boolean;
  evidenceSnapshot?: Record<string, any>;
  validation?: Record<string, any>;
  repairAttempted?: boolean;
  repairResult?: Record<string, any>;
  approvedAt?: string;
  approvedBy?: string;
  createdAt?: string;
}

// Topic Diversity Warning
export interface TopicRecencyWarning {
  type: string;
  message: string;
  severity: 'WARNING' | 'INFO';
  topicId?: string;
  lastScheduledDate?: string;
}

// Conflict details
export interface CalendarConflictDetail {
  calendarItemId: string;
  title: string;
  platform: string;
  scheduledStart: string;
  scheduledEnd: string;
}

export interface CalendarConflictResponse {
  conflict: boolean;
  message?: string;
  conflicts: CalendarConflictDetail[];
}

export interface SuggestedSlot {
  slotName: 'Morning' | 'Afternoon' | 'Evening' | string;
  time: string;
  scheduledStart: string;
  scheduledEnd: string;
  available: boolean;
  conflictReason?: string;
}

export interface CalendarSuggestionResponse {
  date: string;
  platform: string;
  timezone: string;
  slots: SuggestedSlot[];
}

// Editorial Calendar Item
export interface CalendarItem {
  id: string;
  userId: string;
  recommendationId?: string;
  topicId?: string;
  topicName?: string;
  title: string;
  description?: string;
  notes?: string;
  contentType?: string;
  platform: 'YOUTUBE' | 'INSTAGRAM' | 'LINKEDIN' | 'TIKTOK' | 'TWITTER' | 'FACEBOOK' | string;
  scheduledStart: string;
  scheduledEnd: string;
  timezone: string;
  status: 'PLANNED' | 'SCHEDULED' | 'CANCELLED' | 'COMPLETED' | 'DRAFT' | 'PUBLISHED' | 'FAILED';
  priority?: 'LOW' | 'MEDIUM' | 'HIGH';
  approvedAt?: string;
  cancelledAt?: string;
  createdAt?: string;
  updatedAt?: string;
  evidenceSnapshot?: Record<string, any>;
  validationPassed?: boolean;
  warnings?: TopicRecencyWarning[];
}

// Legacy Calendar Event Adapter
export interface CalendarEvent {
  id: string;
  title: string;
  platform: string;
  scheduledDate: string;
  scheduledTime: string;
  status: 'draft' | 'scheduled' | 'published' | 'failed' | string;
  format: string;
  authorName: string;
  previewText?: string;
  notes?: string;
  timezone?: string;
  priority?: string;
  recommendationId?: string;
  topicId?: string;
  topicName?: string;
  scheduledStart?: string;
  scheduledEnd?: string;
}

// Model Evaluation & Guardrails
export interface EvaluationMetrics {
  ragAccuracy: number; // e.g. 98.4%
  hallucinationRate: number; // e.g. 0.8%
  avgLatencyMs: number; // e.g. 420ms
  sentimentF1Score: number; // e.g. 0.94
  guardrailTriggers: number;
  recentLogs: {
    id: string;
    query: string;
    confidence: number;
    hallucinationCheck: 'PASSED' | 'FLAGGED';
    latencyMs: number;
    timestamp: string;
  }[];
}

// Admin Management
export interface AdminUserRecord {
  id: string;
  name: string;
  email: string;
  role: 'USER' | 'ADMIN';
  status: 'active' | 'suspended' | 'pending';
  lastActive: string;
  tokenUsage: number;
  accountsConnected: number;
}

export interface AdminSystemStats {
  totalUsers: number;
  activeToday: number;
  totalTokensUsed: number;
  apiRequests24h: number;
  avgResponseTimeMs: number;
  serverHealth: 'healthy' | 'degraded' | 'maintenance';
}

// ==========================================
// Phase 3K: Content Production Copilot Types
// ==========================================

export type ProductionAssetType =
  | 'CONTENT_BRIEF'
  | 'SCRIPT_OUTLINE'
  | 'HOOK'
  | 'TITLE_VARIATION'
  | 'CTA'
  | 'THUMBNAIL_PROMPT'
  | 'PRODUCTION_CHECKLIST';

export type ProductionAssetStatus =
  | 'GENERATED'
  | 'VALIDATED'
  | 'EDITED'
  | 'APPROVED'
  | 'REJECTED'
  | 'ARCHIVED';

export interface ContentBrief {
  title: string;
  contentType: string;
  platform: string;
  targetAudience: string;
  audienceProblem: string;
  audienceEvidence?: string[];
  coreMessage: string;
  contentAngle: string;
  keyPoints: string[];
  tone?: string;
  callToAction: string;
  successObjective?: string;
}

export interface ScriptOutlineSection {
  sectionTitle: string;
  purpose: string;
  talkingPoints: string[];
  estimatedDurationSeconds?: number;
}

export interface ScriptOutline {
  format: string;
  sections: ScriptOutlineSection[];
  keyTakeaway: string;
}

export interface HookVariant {
  hookType: 'QUESTION' | 'PROBLEM' | 'CONTRAST' | 'CURIOSITY' | 'PRACTICAL' | string;
  text: string;
  rationale?: string;
}

export interface TitleVariant {
  titleType: 'HOW_TO' | 'DIRECT_BENEFIT' | 'CURIOSITY_GAP' | 'QUESTION' | 'STORY' | string;
  text: string;
  rationale?: string;
}

export interface CtaVariant {
  ctaType: 'COMMENT_ENGAGEMENT' | 'SUBSCRIBE_FOLLOW' | 'SAVE_SHARE' | 'QUESTION_DISCUSSION' | string;
  text: string;
}

export interface ThumbnailPrompt {
  concept: string;
  visualSubject: string;
  composition: string;
  textOverlay: string;
  emotion: string;
  style?: string;
}

export interface ChecklistItem {
  phase: 'PRE_PRODUCTION' | 'PRODUCTION' | 'POST_PRODUCTION' | string;
  task: string;
  completed: boolean;
}

export interface ProductionChecklist {
  items: ChecklistItem[];
}

export interface ProductionAsset {
  id: string;
  userId: string;
  recommendationId: string;
  recommendationTitle: string;
  calendarItemId?: string;
  assetType: ProductionAssetType;
  status: ProductionAssetStatus;
  generationMode: 'BASELINE' | 'EVIDENCE_GROUNDED';
  version: number;
  promptVersion?: string;
  modelName?: string;
  contentJson: Record<string, any>;
  evidenceSnapshot?: Record<string, any>;
  evidenceIds?: string[];
  validationJson?: {
    valid: boolean;
    failureSummary?: string;
    checks?: Array<{
      checkName: string;
      passed: boolean;
      reason: string;
    }>;
  };
  revisionHistory?: Array<{
    version: number;
    contentJson: Record<string, any>;
    status: string;
    modifiedAt: string;
    modifiedBy: string;
  }>;
  repairAttempted?: boolean;
  repairResultJson?: Record<string, any>;
  approvedAt?: string;
  approvedBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductionGenerateRequest {
  recommendationId: string;
  calendarItemId?: string;
  assetTypes?: ProductionAssetType[];
  mode?: 'BASELINE' | 'EVIDENCE_GROUNDED';
}

export interface UpdateProductionAssetRequest {
  contentJson?: Record<string, any>;
  title?: string;
  hook?: string;
  cta?: string;
  notes?: string;
}

// ==========================================
// Phase 3L: Multi-Format Content Exporter Types
// ==========================================

export type ExportType =
  | 'MARKDOWN'
  | 'PDF'
  | 'TELEPROMPTER'
  | 'CHECKLIST'
  | 'JSON'
  | 'TIMELINE'
  | 'PRODUCTION_PACKAGE';

export interface ContentExport {
  id: string;
  productionAssetId: string;
  recommendationId: string;
  exportType: ExportType;
  fileName: string;
  mimeType: string;
  contentHash: string;
  version: number;
  fileSizeBytes?: number;
  createdAt: string;
  downloadUrl: string;
  metadata?: Record<string, any>;
}
export interface ExportRequest {
  exportType: ExportType;
}

// ==========================================
// Phase 3M: Research, Observability & Experimentation Types
// ==========================================

export type ExperimentType =
  | 'RECOMMENDATION_EVALUATION'
  | 'PRODUCTION_COPILOT'
  | 'CLUSTERING_STABILITY'
  | 'BASELINE_VS_EVIDENCE_GROUNDED'
  | 'PROMPT_ABLATION';

export type ExperimentStatus =
  | 'CREATED'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED';

export interface ExperimentRun {
  id: string;
  experimentName: string;
  experimentType: ExperimentType;
  description?: string;
  baselineMode?: string;
  treatmentMode?: string;
  datasetSnapshotId?: string;
  promptVersion?: string;
  modelName?: string;
  modelVersion?: string;
  randomSeed?: number;
  status: ExperimentStatus;
  sampleCount: number;
  parameters?: Record<string, any>;
  metricsSummary?: Record<string, any>;
  evaluationSummary?: Record<string, any>;
  reproducibilityMetadata?: Record<string, any>;
  startedAt?: string;
  completedAt?: string;
  createdAt: string;
}

export interface CreateExperimentRequest {
  experimentName: string;
  experimentType: ExperimentType;
  description?: string;
  baselineMode?: string;
  treatmentMode?: string;
  datasetSnapshotId?: string;
  parameters?: Record<string, any>;
}

export interface DatasetSnapshot {
  id: string;
  name: string;
  description?: string;
  platform?: string;
  dateFrom?: string;
  dateTo?: string;
  commentCount: number;
  clusterCount: number;
  recommendationCount: number;
  metadataJson?: Record<string, any>;
  createdAt: string;
}

export interface CreateSnapshotRequest {
  name: string;
  description?: string;
  platform?: string;
  dateFrom?: string;
  dateTo?: string;
}

export interface ClusteringStabilityReport {
  stabilityScore: number;
  matchedClustersCount: number;
  unmatchedClustersCount: number;
  averageCentroidDistance: number;
  clusterPairSimilarities: Array<{
    runClusterId: string;
    baselineClusterId: string;
    cosineSimilarity: number;
  }>;
}

export interface RecommendationEvaluationMetrics {
  totalEvaluated: number;
  averageEvidenceCoverageScore: number;
  validationPassRate: number;
  averageNoveltyScore: number;
  averageLatencyMs: number;
}

export interface ProductionEvaluationMetrics {
  totalEvaluated: number;
  validationPassRate: number;
  repairSuccessRate: number;
  creatorEditRate: number;
  averageApprovalTimeHours: number;
  averageRevisionsPerAsset: number;
}

export interface ResearchMetricsResponse {
  systemMetrics: Record<string, any>;
  nlpMetrics: Record<string, any>;
  clusteringMetrics: Record<string, any>;
  recommendationMetrics: RecommendationEvaluationMetrics | Record<string, any>;
  productionMetrics: ProductionEvaluationMetrics | Record<string, any>;
  creatorWorkflowMetrics: Record<string, any>;
  exportMetrics: Record<string, any>;
  reproducibilityMetadata: Record<string, any>;
}
