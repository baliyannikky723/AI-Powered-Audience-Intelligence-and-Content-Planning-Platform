import type {
  AudienceOverview,
  TrendSignal,
  AudienceMemoryItem,
  ContentRecommendation,
} from '../types/models';
import { mockDelay } from '../lib/apiClient';

const MOCK_AUDIENCE_OVERVIEW: AudienceOverview = {
  totalAudience: 446300,
  activeFollowers: 128900,
  avgEngagementRate: 6.8,
  netSentimentScore: 78,
  segments: [
    {
      id: 'seg-1',
      name: 'Senior Frontend & Fullstack Devs',
      percentage: 38,
      sentimentScore: 84,
      engagementRate: 8.2,
      topInterests: ['React 19', 'Next.js 15', 'TypeScript', 'Server Actions'],
      keyPainPoints: ['Hydration bugs', 'Bundle size bloat', 'API caching complexity'],
      growth: 14.5,
    },
    {
      id: 'seg-2',
      name: 'AI Engineers & LLM Builders',
      percentage: 29,
      sentimentScore: 76,
      engagementRate: 9.4,
      topInterests: ['RAG Architectures', 'LangChain', 'Local LLMs (Ollama)', 'Vector DBs'],
      keyPainPoints: ['Hallucination in production', 'Chunking strategies', 'Token cost runaway'],
      growth: 28.2,
    },
    {
      id: 'seg-3',
      name: 'UI/UX Designers & Design Engineers',
      percentage: 21,
      sentimentScore: 81,
      engagementRate: 5.6,
      topInterests: ['Design Systems', 'Micro-interactions', 'Figma to Code', 'CSS Architecture'],
      keyPainPoints: ['Handoff friction', 'Component accessibility', 'Color tokens'],
      growth: 6.8,
    },
    {
      id: 'seg-4',
      name: 'Tech Lead / Startup Founders',
      percentage: 12,
      sentimentScore: 70,
      engagementRate: 4.8,
      topInterests: ['Engineering Velocity', 'Tech Stack Selection', 'AI Automation'],
      keyPainPoints: ['Hiring talent', 'Architecture refactoring', 'Burn rate'],
      growth: 9.1,
    },
  ],
  demographics: {
    ageGroups: [
      { label: '18-24', value: 22 },
      { label: '25-34', value: 48 },
      { label: '35-44', value: 20 },
      { label: '45+',   value: 10 },
    ],
    locations: [
      { country: 'United States', percentage: 36 },
      { country: 'India',         percentage: 24 },
      { country: 'United Kingdom', percentage: 12 },
      { country: 'Germany',       percentage: 9 },
      { country: 'Others',        percentage: 19 },
    ],
    topPlatforms: [
      { platform: 'YouTube',   users: 142000, percentage: 32 },
      { platform: 'Instagram', users: 89300,  percentage: 20 },
      { platform: 'Facebook',  users: 215000, percentage: 48 },
    ],
  },
  sentimentTrends: [
    { date: 'Sep 01', positive: 68, neutral: 22, negative: 10 },
    { date: 'Sep 08', positive: 72, neutral: 20, negative: 8 },
    { date: 'Sep 15', positive: 75, neutral: 18, negative: 7 },
    { date: 'Sep 22', positive: 71, neutral: 21, negative: 8 },
    { date: 'Sep 29', positive: 78, neutral: 16, negative: 6 },
    { date: 'Oct 05', positive: 82, neutral: 13, negative: 5 },
  ],
};

const MOCK_TREND_SIGNALS: TrendSignal[] = [
  {
    id: 'tr-1',
    topic: 'Ollama 3.2 on local Apple Silicon & Edge Devices',
    category: 'ai',
    velocity: 94,
    volume: 8400,
    sentiment: 'positive',
    platform: 'youtube',
    recommendedAngle: 'Benchmark local Ollama vs GPT-4o-mini latency on MacBook M3 for dev workflows',
    urgency: 'high',
    createdAt: '2026-10-04T10:00:00Z',
  },
  {
    id: 'tr-2',
    topic: 'React 19 Server Actions vs Traditional REST Endpoints',
    category: 'tech',
    velocity: 88,
    volume: 6200,
    sentiment: 'positive',
    platform: 'linkedin',
    recommendedAngle: 'Practical architectural breakdown: when Server Actions create security risks & when they shine',
    urgency: 'high',
    createdAt: '2026-10-03T14:30:00Z',
  },
  {
    id: 'tr-3',
    topic: 'Hybrid RAG with BM25 + Dense Vector Re-ranking',
    category: 'ai',
    velocity: 79,
    volume: 4100,
    sentiment: 'positive',
    platform: 'youtube',
    recommendedAngle: 'Step-by-step tutorial showing 40% retrieval accuracy improvement with Cohere Rerank',
    urgency: 'medium',
    createdAt: '2026-10-02T09:15:00Z',
  },
  {
    id: 'tr-4',
    topic: 'Figma to Production Code with AI Tokens',
    category: 'design',
    velocity: 64,
    volume: 3800,
    sentiment: 'neutral',
    platform: 'instagram',
    recommendedAngle: 'Carousel breaking down token naming conventions that AI coding assistants parse flawlessly',
    urgency: 'medium',
    createdAt: '2026-10-01T16:00:00Z',
  },
];

const MOCK_AUDIENCE_MEMORY: AudienceMemoryItem[] = [
  {
    id: 'mem-1',
    category: 'recurring_question',
    title: 'How to handle RAG hallucinations in strict enterprise domains?',
    summary: 'Over 48 viewers across YouTube and Reddit asked for practical guardrail setups and evaluation frameworks (e.g., Ragas / TruLens).',
    mentionCount: 48,
    sourcePlatforms: ['youtube', 'reddit'],
    firstObserved: '2026-08-12',
    lastObserved: '2026-10-03',
    confidenceScore: 0.96,
    actionTaken: 'Created video outline for Episode 14 on RAG Evaluation',
  },
  {
    id: 'mem-2',
    category: 'criticism',
    title: 'Audio background music volume too loud in coding tutorials',
    summary: '7 comments specifically noted that background lo-fi music made speaker explanations hard to discern with headphones.',
    mentionCount: 7,
    sourcePlatforms: ['youtube'],
    firstObserved: '2026-09-02',
    lastObserved: '2026-09-28',
    confidenceScore: 0.92,
    actionTaken: 'Lowered master background audio track by -6dB in Premiere template',
  },
  {
    id: 'mem-3',
    category: 'feature_request',
    title: 'Request for downloadable GitHub starter boilerplate repo',
    summary: 'Community repeatedly asks for pre-configured Vite + FastAPI + Tailwind starter templates with Docker Compose.',
    mentionCount: 34,
    sourcePlatforms: ['youtube', 'linkedin', 'twitter'],
    firstObserved: '2026-07-20',
    lastObserved: '2026-10-02',
    confidenceScore: 0.98,
  },
  {
    id: 'mem-4',
    category: 'persona_insight',
    title: 'Audience prefers full code walkthroughs over high-level conceptual slides',
    summary: 'Comments praising videos that show actual IDE debugging with terminal outputs (82% higher retention).',
    mentionCount: 65,
    sourcePlatforms: ['youtube', 'instagram'],
    firstObserved: '2026-06-15',
    lastObserved: '2026-10-04',
    confidenceScore: 0.95,
  },
];

const MOCK_RECOMMENDATIONS: ContentRecommendation[] = [
  {
    id: 'rec-1',
    title: '5 Costly Mistakes In Production RAG Applications',
    hook: 'Why 80% of LangChain apps fail when users start asking multi-turn questions (and how we fixed our latency by 60%).',
    targetPlatform: 'youtube',
    format: 'short_video',
    rationale: 'High velocity topic with strong audience pain point in comment history (#mem-1).',
    projectedEngagement: 2.8,
    confidence: 0.94,
    targetSegment: 'AI Engineers & LLM Builders',
    status: 'new',
  },
  {
    id: 'rec-2',
    title: 'Clean Architecture in React 19: The 2026 Playbook',
    hook: 'Stop putting useEffect everywhere: Here is the modern way to structure state, server actions, and optimistic updates.',
    targetPlatform: 'linkedin',
    format: 'carousel',
    rationale: 'React 19 discussions are trending up by +88% this week among Senior Frontend developers.',
    projectedEngagement: 2.2,
    confidence: 0.89,
    targetSegment: 'Senior Frontend & Fullstack Devs',
    status: 'saved',
  },
  {
    id: 'rec-3',
    title: 'Local Ollama vs Cloud APIs: Pricing & Privacy Matrix',
    hook: 'When is it actually cheaper to run your own GPU instance vs paying OpenAI per million tokens?',
    targetPlatform: 'twitter',
    format: 'thread',
    rationale: 'Addresses cost runaway fear identified in audience segment analysis.',
    projectedEngagement: 3.1,
    confidence: 0.91,
    targetSegment: 'Tech Lead / Startup Founders',
    status: 'new',
  },
];

export const intelligenceService = {
  getAudienceOverview: async (): Promise<AudienceOverview> => {
    return mockDelay(MOCK_AUDIENCE_OVERVIEW, 250);
  },

  getTrends: async (category?: string): Promise<TrendSignal[]> => {
    let list = [...MOCK_TREND_SIGNALS];
    if (category && category !== 'all') {
      list = list.filter(t => t.category === category);
    }
    return mockDelay(list, 200);
  },

  getAudienceMemory: async (): Promise<AudienceMemoryItem[]> => {
    return mockDelay(MOCK_AUDIENCE_MEMORY, 200);
  },

  getRecommendations: async (): Promise<ContentRecommendation[]> => {
    return mockDelay(MOCK_RECOMMENDATIONS, 200);
  },

  updateRecommendationStatus: async (
    id: string,
    status: ContentRecommendation['status']
  ): Promise<ContentRecommendation> => {
    const item = MOCK_RECOMMENDATIONS.find(r => r.id === id);
    if (!item) throw new Error('Recommendation not found');
    item.status = status;
    return mockDelay(item, 200);
  },
};
