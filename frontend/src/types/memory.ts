export type AudienceInterestStatus = 'ACTIVE' | 'WEAKENING' | 'INACTIVE';

export interface AudienceInterest {
  id?: string;
  topicId: string;
  label: string;
  topicName?: string;
  status: AudienceInterestStatus;
  confidence: number;
  decayedConfidence?: number;
  evidenceCount: number;
  consistency?: number;
  firstSeenAt?: string;
  lastSeenAt: string;
  daysSinceLastSeen?: number;
  halfLifeDays?: number;
  trend?: 'GROWING' | 'STRENGTHENING' | 'STABLE' | 'WEAKENING';
  evidenceIds?: string[];
}

export interface AudienceQuestion {
  id?: string;
  questionHash: string;
  normalizedText: string;
  confidence: number;
  evidenceCount: number;
  firstSeenAt: string;
  lastSeenAt: string;
  topicId?: string;
  topicLabel?: string;
  topicName?: string;
}

export interface RelatedTopic {
  sourceTopicId: string;
  sourceTopicLabel?: string;
  targetTopicId: string;
  targetTopicLabel?: string;
  targetTopicName?: string;
  similarity?: number;
  weight?: number;
  coOccurrenceCount?: number;
  cooccurrenceCount?: number;
}

export interface TopicContentIdea {
  id?: string;
  contentIdeaId?: string;
  topicId?: string;
  title: string;
  angle: string;
  status: string;
  createdAt?: string;
  scheduledDate?: string;
  scheduledAt?: string;
  platform?: string;
}

export interface AudienceMemorySummary {
  activeInterests: AudienceInterest[];
  weakeningInterests: AudienceInterest[];
  recurringQuestions: AudienceQuestion[];
  relatedTopicCount: number;
  contentIdeaCount: number;
  lastUpdated: string;
}

export interface MemoryRebuildResult {
  success?: boolean;
  status?: string;
  mode?: string;
  userId?: string;
  topicsProjected?: number;
  topicCount?: number;
  questionsProjected?: number;
  questionCount?: number;
  contentIdeasProjected?: number;
  contentIdeaCount?: number;
  relationshipsCreated?: number;
  relationshipCount?: number;
  durationMs: number;
  timestamp: string;
  message?: string;
}
