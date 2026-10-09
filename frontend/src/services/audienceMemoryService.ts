import { apiClient, mockDelay } from '../lib/apiClient';
import type {
  AudienceInterest,
  AudienceQuestion,
  RelatedTopic,
  TopicContentIdea,
  AudienceMemorySummary,
  MemoryRebuildResult,
} from '../types/memory';

const MOCK_INTERESTS: AudienceInterest[] = [
  {
    topicId: 'topic-101',
    label: 'Spring Boot 3.4 & Micrometer Observability',
    status: 'ACTIVE',
    confidence: 0.94,
    decayedConfidence: 0.91,
    evidenceCount: 142,
    consistency: 0.96,
    firstSeenAt: '2026-08-15T10:00:00Z',
    lastSeenAt: '2026-10-06T18:30:00Z',
    daysSinceLastSeen: 2,
    trend: 'STRENGTHENING',
  },
  {
    topicId: 'topic-102',
    label: 'Neo4j Knowledge Graphs & Cypher Projections',
    status: 'ACTIVE',
    confidence: 0.88,
    decayedConfidence: 0.86,
    evidenceCount: 98,
    consistency: 0.92,
    firstSeenAt: '2026-09-01T14:00:00Z',
    lastSeenAt: '2026-10-07T09:15:00Z',
    daysSinceLastSeen: 1,
    trend: 'STRENGTHENING',
  },
  {
    topicId: 'topic-103',
    label: 'HDBSCAN vs Agglomerative Clustering Stability',
    status: 'ACTIVE',
    confidence: 0.79,
    decayedConfidence: 0.74,
    evidenceCount: 65,
    consistency: 0.89,
    firstSeenAt: '2026-07-20T12:00:00Z',
    lastSeenAt: '2026-09-28T16:45:00Z',
    daysSinceLastSeen: 10,
    trend: 'STABLE',
  },
  {
    topicId: 'topic-104',
    label: 'Docker Compose & Container Resource Tuning',
    status: 'WEAKENING',
    confidence: 0.54,
    decayedConfidence: 0.42,
    evidenceCount: 31,
    consistency: 0.81,
    firstSeenAt: '2026-06-10T08:00:00Z',
    lastSeenAt: '2026-08-25T11:20:00Z',
    daysSinceLastSeen: 44,
    trend: 'WEAKENING',
  },
  {
    topicId: 'topic-105',
    label: 'Legacy Python 3.9 Virtualenv Setup',
    status: 'INACTIVE',
    confidence: 0.28,
    decayedConfidence: 0.18,
    evidenceCount: 14,
    consistency: 0.72,
    firstSeenAt: '2026-05-01T09:00:00Z',
    lastSeenAt: '2026-07-15T10:00:00Z',
    daysSinceLastSeen: 85,
    trend: 'WEAKENING',
  },
];

const MOCK_QUESTIONS: AudienceQuestion[] = [
  {
    questionHash: 'qhash-001',
    normalizedText: 'How do you configure Neo4j Testcontainers in Spring Boot without Docker failures?',
    confidence: 0.92,
    evidenceCount: 38,
    firstSeenAt: '2026-09-10T11:00:00Z',
    lastSeenAt: '2026-10-07T14:20:00Z',
    topicLabel: 'Neo4j Knowledge Graphs & Cypher Projections',
  },
  {
    questionHash: 'qhash-002',
    normalizedText: 'What is the exact mathematical formula for memory decay and half-life calculation?',
    confidence: 0.89,
    evidenceCount: 29,
    firstSeenAt: '2026-09-15T09:30:00Z',
    lastSeenAt: '2026-10-06T16:40:00Z',
    topicLabel: 'Spring Boot 3.4 & Micrometer Observability',
  },
  {
    questionHash: 'qhash-003',
    normalizedText: 'Why does cosine distance outperform euclidean distance in HDBSCAN text embeddings?',
    confidence: 0.84,
    evidenceCount: 22,
    firstSeenAt: '2026-08-20T15:00:00Z',
    lastSeenAt: '2026-10-02T18:10:00Z',
    topicLabel: 'HDBSCAN vs Agglomerative Clustering Stability',
  },
];

export const audienceMemoryService = {
  getSummary: async (): Promise<AudienceMemorySummary> => {
    try {
      const response = await apiClient.get<AudienceMemorySummary>('/memory/summary');
      if (response) return response;
    } catch {
      // Fallback to mock data
    }
    const active = MOCK_INTERESTS.filter((i) => i.status === 'ACTIVE');
    const weakening = MOCK_INTERESTS.filter((i) => i.status === 'WEAKENING');
    return mockDelay<AudienceMemorySummary>({
      activeInterests: active,
      weakeningInterests: weakening,
      recurringQuestions: MOCK_QUESTIONS,
      relatedTopicCount: 8,
      contentIdeaCount: 12,
      lastUpdated: new Date().toISOString(),
    });
  },

  getInterests: async (status?: string): Promise<AudienceInterest[]> => {
    try {
      const params = status ? { status } : {};
      const response = await apiClient.get<AudienceInterest[]>('/memory/interests', { params });
      if (response) return response;
    } catch {
      // Fallback
    }
    if (status) {
      return mockDelay(MOCK_INTERESTS.filter((i) => i.status === status));
    }
    return mockDelay([...MOCK_INTERESTS]);
  },

  getInterestByTopic: async (topicId: string): Promise<AudienceInterest | null> => {
    try {
      const response = await apiClient.get<AudienceInterest>(`/memory/interests/${topicId}`);
      if (response) return response;
    } catch {
      // Fallback
    }
    const found = MOCK_INTERESTS.find((i) => i.topicId === topicId);
    return mockDelay(found || null);
  },

  getQuestions: async (): Promise<AudienceQuestion[]> => {
    try {
      const response = await apiClient.get<AudienceQuestion[]>('/memory/questions');
      if (response) return response;
    } catch {
      // Fallback
    }
    return mockDelay([...MOCK_QUESTIONS]);
  },

  getRelatedTopics: async (topicId: string): Promise<RelatedTopic[]> => {
    try {
      const response = await apiClient.get<RelatedTopic[]>(`/memory/topics/${topicId}/related`);
      if (response) return response;
    } catch {
      // Fallback
    }
    return mockDelay<RelatedTopic[]>([
      {
        sourceTopicId: topicId,
        sourceTopicLabel: 'Selected Topic',
        targetTopicId: 'topic-102',
        targetTopicLabel: 'Neo4j Knowledge Graphs & Cypher Projections',
        similarity: 0.87,
        coOccurrenceCount: 42,
      },
      {
        sourceTopicId: topicId,
        sourceTopicLabel: 'Selected Topic',
        targetTopicId: 'topic-103',
        targetTopicLabel: 'HDBSCAN vs Agglomerative Clustering Stability',
        similarity: 0.76,
        coOccurrenceCount: 28,
      },
    ]);
  },

  getTopicContentIdeas: async (topicId: string): Promise<TopicContentIdea[]> => {
    try {
      const response = await apiClient.get<TopicContentIdea[]>(`/memory/topics/${topicId}/content-ideas`);
      if (response) return response;
    } catch {
      // Fallback
    }
    return mockDelay<TopicContentIdea[]>([
      {
        contentIdeaId: 'idea-001',
        title: 'Deep-Dive: Building Resilient Microservices with Spring Boot 3.4 and Neo4j Knowledge Graphs',
        angle: 'Technical Architectural Tutorial',
        status: 'SUGGESTED',
        scheduledDate: '2026-10-15T09:00:00Z',
      },
      {
        contentIdeaId: 'idea-002',
        title: 'Observability & Metrics without Cardinality Explosions: A Micrometer Guide',
        angle: 'Performance Best Practices',
        status: 'PLANNED',
        scheduledDate: '2026-10-20T14:00:00Z',
      },
    ]);
  },

  rebuildKnowledgeGraph: async (): Promise<MemoryRebuildResult> => {
    try {
      const response = await apiClient.post<MemoryRebuildResult>('/memory/rebuild');
      if (response) return response;
    } catch {
      // Fallback
    }
    return mockDelay<MemoryRebuildResult>({
      success: true,
      mode: 'FULL_REBUILD',
      topicsProjected: 12,
      questionsProjected: 24,
      contentIdeasProjected: 16,
      relationshipsCreated: 58,
      durationMs: 342,
      timestamp: new Date().toISOString(),
      message: 'Knowledge graph projection successfully rebuilt from PostgreSQL.',
    });
  },
};
