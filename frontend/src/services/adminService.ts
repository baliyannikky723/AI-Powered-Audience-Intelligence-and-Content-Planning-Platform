import type { EvaluationMetrics, AdminUserRecord, AdminSystemStats } from '../types/models';
import { mockDelay } from '../lib/apiClient';

const MOCK_EVALUATION: EvaluationMetrics = {
  ragAccuracy: 98.4,
  hallucinationRate: 0.6,
  avgLatencyMs: 380,
  sentimentF1Score: 0.94,
  guardrailTriggers: 12,
  recentLogs: [
    {
      id: 'log-1',
      query: 'What are users asking about the course pricing?',
      confidence: 0.99,
      hallucinationCheck: 'PASSED',
      latencyMs: 340,
      timestamp: '2 mins ago',
    },
    {
      id: 'log-2',
      query: 'Are there any sound problems reported in the audio?',
      confidence: 0.97,
      hallucinationCheck: 'PASSED',
      latencyMs: 395,
      timestamp: '14 mins ago',
    },
    {
      id: 'log-3',
      query: 'Will we support Rust backend in future videos?',
      confidence: 0.88,
      hallucinationCheck: 'FLAGGED',
      latencyMs: 460,
      timestamp: '1 hour ago',
    },
    {
      id: 'log-4',
      query: 'Summarize top negative sentiment themes',
      confidence: 0.98,
      hallucinationCheck: 'PASSED',
      latencyMs: 310,
      timestamp: '2 hours ago',
    },
  ],
};

export const evaluationService = {
  getMetrics: async (): Promise<EvaluationMetrics> => {
    return mockDelay(MOCK_EVALUATION, 250);
  },
};

let inMemoryUsers: AdminUserRecord[] = [
  {
    id: 'u-1',
    name: 'Alex Rivera',
    email: 'admin@pulse-gpt.ai',
    role: 'ADMIN',
    status: 'active',
    lastActive: 'Just now',
    tokenUsage: 145000,
    accountsConnected: 5,
  },
  {
    id: 'u-2',
    name: 'Sarah Chen',
    email: 'sarah.chen@creator.io',
    role: 'USER',
    status: 'active',
    lastActive: '5 mins ago',
    tokenUsage: 89000,
    accountsConnected: 3,
  },
  {
    id: 'u-3',
    name: 'Marcus Vance',
    email: 'marcus@vancemedia.co',
    role: 'USER',
    status: 'active',
    lastActive: '2 hours ago',
    tokenUsage: 210000,
    accountsConnected: 6,
  },
  {
    id: 'u-4',
    name: 'Elena Rostova',
    email: 'elena.tech@studio.net',
    role: 'USER',
    status: 'suspended',
    lastActive: '3 days ago',
    tokenUsage: 45000,
    accountsConnected: 1,
  },
];

const MOCK_ADMIN_STATS: AdminSystemStats = {
  totalUsers: 1420,
  activeToday: 489,
  totalTokensUsed: 14850000,
  apiRequests24h: 184500,
  avgResponseTimeMs: 245,
  serverHealth: 'healthy',
};

export const adminService = {
  getStats: async (): Promise<AdminSystemStats> => {
    return mockDelay(MOCK_ADMIN_STATS, 200);
  },

  getUsers: async (): Promise<AdminUserRecord[]> => {
    return mockDelay(inMemoryUsers, 250);
  },

  updateUserRole: async (
    userId: string,
    role: 'USER' | 'ADMIN'
  ): Promise<AdminUserRecord> => {
    const user = inMemoryUsers.find(u => u.id === userId);
    if (!user) throw new Error('User not found');
    user.role = role;
    return mockDelay(user, 200);
  },

  updateUserStatus: async (
    userId: string,
    status: 'active' | 'suspended'
  ): Promise<AdminUserRecord> => {
    const user = inMemoryUsers.find(u => u.id === userId);
    if (!user) throw new Error('User not found');
    user.status = status;
    return mockDelay(user, 200);
  },
};
