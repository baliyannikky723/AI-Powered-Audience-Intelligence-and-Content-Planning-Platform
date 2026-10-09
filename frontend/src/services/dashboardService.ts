import { mockAccounts, mockComments, type ConnectedAccount, type Comment } from '../data/mockData';
import { mockDelay } from '../lib/apiClient';

export interface DashboardMetrics {
  totalAudience: number;
  audienceGrowth: number;
  totalComments: number;
  commentsGrowth: number;
  sentimentRatio: number;
  sentimentGrowth: number;
  highPriorityIssues: number;
  accounts: ConnectedAccount[];
  recentComments: Comment[];
  sentimentBreakdown: { name: string; value: number; color: string }[];
  engagementData: { day: string; views: number; comments: number }[];
  platformBreakdown: { name: string; count: number; color: string }[];
}

export const dashboardService = {
  getOverviewMetrics: async (): Promise<DashboardMetrics> => {
    const totalAudience = mockAccounts.reduce((sum, acc) => sum + acc.followerCount, 0);
    const totalComments = mockComments.length;
    const positiveCount = mockComments.filter(c => c.sentiment === 'positive').length;
    const negativeCount = mockComments.filter(c => c.sentiment === 'negative').length;
    const neutralCount = mockComments.filter(c => c.sentiment === 'neutral').length;
    const highPriorityIssues = mockComments.filter(c => c.priority === 'high' && !c.replied).length;

    const sentimentRatio = Math.round((positiveCount / (totalComments || 1)) * 100);

    const sentimentBreakdown = [
      { name: 'Positive', value: positiveCount, color: '#10B981' },
      { name: 'Neutral',  value: neutralCount,  color: '#64748B' },
      { name: 'Negative', value: negativeCount, color: '#EF4444' },
    ];

    const engagementData = [
      { day: 'Mon', views: 12400, comments: 145 },
      { day: 'Tue', views: 18200, comments: 220 },
      { day: 'Wed', views: 15600, comments: 180 },
      { day: 'Thu', views: 24500, comments: 340 },
      { day: 'Fri', views: 32000, comments: 490 },
      { day: 'Sat', views: 28400, comments: 310 },
      { day: 'Sun', views: 36800, comments: 520 },
    ];

    const platformBreakdown = [
      { name: 'YouTube',   count: mockComments.filter(c => c.platform === 'youtube').length,   color: '#FF0000' },
      { name: 'Instagram', count: mockComments.filter(c => c.platform === 'instagram').length, color: '#E1306C' },
      { name: 'LinkedIn',  count: mockComments.filter(c => c.platform === 'linkedin').length,  color: '#0A66C2' },
      { name: 'Facebook',  count: mockComments.filter(c => c.platform === 'facebook').length,  color: '#1877F2' },
      { name: 'X / Twt',   count: mockComments.filter(c => c.platform === 'twitter').length,   color: '#1DA1F2' },
      { name: 'Reddit',    count: mockComments.filter(c => c.platform === 'reddit').length,    color: '#FF4500' },
    ];

    return mockDelay<DashboardMetrics>({
      totalAudience,
      audienceGrowth: 12.4,
      totalComments,
      commentsGrowth: 8.7,
      sentimentRatio,
      sentimentGrowth: 4.2,
      highPriorityIssues,
      accounts: mockAccounts,
      recentComments: mockComments.slice(0, 5),
      sentimentBreakdown,
      engagementData,
      platformBreakdown,
    }, 250);
  },
};
