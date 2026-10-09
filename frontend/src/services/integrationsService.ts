import { mockAccounts, type ConnectedAccount } from '../data/mockData';
import { mockDelay } from '../lib/apiClient';

let inMemoryAccounts: ConnectedAccount[] = [...mockAccounts];

export const integrationsService = {
  getAccounts: async (): Promise<ConnectedAccount[]> => {
    return mockDelay(inMemoryAccounts, 200);
  },

  connectAccount: async (platform: string, handle: string): Promise<ConnectedAccount> => {
    const existingIdx = inMemoryAccounts.findIndex(a => a.platform === platform);
    if (existingIdx !== -1) {
      inMemoryAccounts[existingIdx] = {
        ...inMemoryAccounts[existingIdx],
        status: 'connected',
        handle: handle.startsWith('@') ? handle : `@${handle}`,
      };
      return mockDelay(inMemoryAccounts[existingIdx], 300);
    }

    const newAcc: ConnectedAccount = {
      id: `${platform}-${Date.now()}`,
      platform,
      handle: handle.startsWith('@') ? handle : `@${handle}`,
      name: `${platform.charAt(0).toUpperCase() + platform.slice(1)} Channel`,
      avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=60',
      connectedAt: new Date().toISOString().split('T')[0],
      status: 'connected',
      followerCount: Math.floor(Math.random() * 50000) + 12000,
      postsCount: Math.floor(Math.random() * 80) + 10,
      recentPosts: [],
    };

    inMemoryAccounts.push(newAcc);
    return mockDelay(newAcc, 300);
  },

  disconnectAccount: async (platform: string): Promise<ConnectedAccount> => {
    const idx = inMemoryAccounts.findIndex(a => a.platform === platform);
    if (idx === -1) {
      throw new Error(`Account on ${platform} not found`);
    }

    inMemoryAccounts[idx] = {
      ...inMemoryAccounts[idx],
      status: 'disconnected',
    };

    return mockDelay(inMemoryAccounts[idx], 250);
  },
};
