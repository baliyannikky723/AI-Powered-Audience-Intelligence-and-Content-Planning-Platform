import { apiClient, mockDelay } from '../lib/apiClient';
import type {
  ProductionAsset,
  ProductionGenerateRequest,
  UpdateProductionAssetRequest,
} from '../types/models';

let inMemoryAssets: ProductionAsset[] = [
  {
    id: 'prod-asset-1',
    userId: 'user-default-1',
    recommendationId: 'rec-1',
    recommendationTitle: 'Deep Dive: Battery Optimization & Fast Charging in 2026',
    calendarItemId: 'cal-item-1',
    assetType: 'CONTENT_BRIEF',
    status: 'VALIDATED',
    generationMode: 'EVIDENCE_GROUNDED',
    version: 1,
    promptVersion: 'PRODUCTION_PROMPT_V1',
    modelName: 'pulsegpt-production-v1',
    contentJson: {
      title: 'Deep Dive: Battery Optimization & Fast Charging in 2026',
      contentType: 'VIDEO',
      platform: 'YOUTUBE',
      targetAudience: 'Tech Enthusiasts & Mobile Gamers',
      audienceProblem: 'High battery drain while gaming and slow charging speed with non-OEM chargers.',
      audienceEvidence: [
        'Why does my battery drop from 80% to 20% in 2 hours?',
        'Does fast charging damage long term battery health?'
      ],
      coreMessage: 'Mastering modern battery charging cycles, thermal throttling, and real-world efficiency tips.',
      contentAngle: 'Empirical benchmark testing with thermal imaging and charging watt meters.',
      keyPoints: [
        'How 120W charging curves actually work',
        'Thermal throttling effects on battery longevity',
        'Top 3 settings to extend screen-on time without sacrificing performance'
      ],
      tone: 'Authoritative, Engaging, Practical',
      callToAction: 'Drop your average screen-on time in the comments below!',
      successObjective: 'Address top community questions while providing actionable battery optimization steps.'
    },
    evidenceSnapshot: {
      recommendationTitle: 'Deep Dive: Battery Optimization & Fast Charging in 2026',
      topicName: 'Battery & Charging',
      evidenceCount: 3,
      evidenceIds: ['topic:battery_1', 'comment:battery_q1', 'comment:battery_q2']
    },
    evidenceIds: ['topic:battery_1', 'comment:battery_q1', 'comment:battery_q2'],
    validationJson: {
      valid: true,
      checks: [
        { checkName: 'SCHEMA', passed: true, reason: 'All schema fields valid' },
        { checkName: 'EVIDENCE', passed: true, reason: 'Verified 3 audience evidence signals' },
        { checkName: 'UNSUPPORTED_CLAIMS', passed: true, reason: 'No fabricated metrics' },
        { checkName: 'CONTENT_RELEVANCE', passed: true, reason: 'Aligned with recommendation' },
        { checkName: 'SAFETY_AND_PRIVACY', passed: true, reason: 'No PII or secrets detected' },
        { checkName: 'QUALITY_AND_COMPLETENESS', passed: true, reason: 'Quality checks passed' }
      ]
    },
    createdAt: new Date(Date.now() - 3600000).toISOString(),
    updatedAt: new Date(Date.now() - 3600000).toISOString()
  },
  {
    id: 'prod-asset-2',
    userId: 'user-default-1',
    recommendationId: 'rec-1',
    recommendationTitle: 'Deep Dive: Battery Optimization & Fast Charging in 2026',
    calendarItemId: 'cal-item-1',
    assetType: 'HOOK',
    status: 'VALIDATED',
    generationMode: 'EVIDENCE_GROUNDED',
    version: 1,
    contentJson: {
      hooks: [
        {
          hookType: 'QUESTION',
          text: 'Have you ever noticed your phone battery dropping 30% in under an hour of normal use? Here is what is actually happening.',
          rationale: 'Directly validates a widespread viewer frustration.'
        },
        {
          hookType: 'PROBLEM',
          text: 'Most people think fast charging ruins their phone battery, but they are looking at the wrong setting entirely.',
          rationale: 'Challenges a common misconception without making deceptive claims.'
        },
        {
          hookType: 'PRACTICAL',
          text: 'We tested 5 flagship devices for 100 hours to answer your top battery questions once and for all.',
          rationale: 'Positions the video as empirical and evidence-backed.'
        }
      ]
    },
    evidenceSnapshot: {
      topicName: 'Battery & Charging',
      evidenceCount: 3
    },
    createdAt: new Date(Date.now() - 3500000).toISOString(),
    updatedAt: new Date(Date.now() - 3500000).toISOString()
  },
  {
    id: 'prod-asset-3',
    userId: 'user-default-1',
    recommendationId: 'rec-1',
    recommendationTitle: 'Deep Dive: Battery Optimization & Fast Charging in 2026',
    calendarItemId: 'cal-item-1',
    assetType: 'SCRIPT_OUTLINE',
    status: 'VALIDATED',
    generationMode: 'EVIDENCE_GROUNDED',
    version: 1,
    contentJson: {
      format: 'VIDEO_10_PART_STRUCTURE',
      sections: [
        {
          sectionTitle: '1. Hook & Intro',
          purpose: 'Hook viewer and state clear value proposition',
          talkingPoints: ['Highlight common battery drain dilemma', 'Overview of test benchmarks'],
          estimatedDurationSeconds: 25
        },
        {
          sectionTitle: '2. The Core Culprit: Thermal Throttling',
          purpose: 'Explain the root cause of unexpected drain',
          talkingPoints: ['Heat generation during fast charging', 'Impact of ambient room temperature'],
          estimatedDurationSeconds: 60
        },
        {
          sectionTitle: '3. 3 Practical Fixes',
          purpose: 'Provide actionable configuration walkthrough',
          talkingPoints: ['Adaptive refresh rate limits', 'Background network scanning toggle', 'Optimized charging modes'],
          estimatedDurationSeconds: 90
        },
        {
          sectionTitle: '4. Summary & Call to Action',
          purpose: 'Summarize takeaways and prompt engagement',
          talkingPoints: ['Quick recap checklist', 'Ask viewers to share their screen-on time'],
          estimatedDurationSeconds: 25
        }
      ],
      keyTakeaway: 'Practical thermal management and 3 specific setting changes can extend usable battery endurance by up to 25%.'
    },
    createdAt: new Date(Date.now() - 3400000).toISOString(),
    updatedAt: new Date(Date.now() - 3400000).toISOString()
  },
  {
    id: 'prod-asset-4',
    userId: 'user-default-1',
    recommendationId: 'rec-1',
    recommendationTitle: 'Deep Dive: Battery Optimization & Fast Charging in 2026',
    calendarItemId: 'cal-item-1',
    assetType: 'TITLE_VARIATION',
    status: 'VALIDATED',
    generationMode: 'EVIDENCE_GROUNDED',
    version: 1,
    contentJson: {
      titles: [
        {
          titleType: 'HOW_TO',
          text: 'How to Fix Sudden Battery Drain on Modern Smartphones',
          rationale: 'High search volume intent'
        },
        {
          titleType: 'DIRECT_BENEFIT',
          text: 'Double Your Screen-On Time with These 3 Battery Settings',
          rationale: 'Clear, compelling value proposition'
        },
        {
          titleType: 'CURIOSITY_GAP',
          text: 'The Real Truth About 120W Fast Charging & Battery Health',
          rationale: 'Addresses persistent consumer skepticism'
        }
      ]
    },
    createdAt: new Date(Date.now() - 3300000).toISOString(),
    updatedAt: new Date(Date.now() - 3300000).toISOString()
  },
  {
    id: 'prod-asset-5',
    userId: 'user-default-1',
    recommendationId: 'rec-1',
    recommendationTitle: 'Deep Dive: Battery Optimization & Fast Charging in 2026',
    calendarItemId: 'cal-item-1',
    assetType: 'CTA',
    status: 'VALIDATED',
    generationMode: 'EVIDENCE_GROUNDED',
    version: 1,
    contentJson: {
      ctas: [
        {
          ctaType: 'COMMENT_ENGAGEMENT',
          text: 'What phone are you currently using and what is your average battery life? Let me know in the comments below!'
        },
        {
          ctaType: 'SUBSCRIBE_FOLLOW',
          text: 'If this helped you optimize your battery, hit subscribe for more evidence-grounded hardware deep dives.'
        }
      ]
    },
    createdAt: new Date(Date.now() - 3200000).toISOString(),
    updatedAt: new Date(Date.now() - 3200000).toISOString()
  },
  {
    id: 'prod-asset-6',
    userId: 'user-default-1',
    recommendationId: 'rec-1',
    recommendationTitle: 'Deep Dive: Battery Optimization & Fast Charging in 2026',
    calendarItemId: 'cal-item-1',
    assetType: 'THUMBNAIL_PROMPT',
    status: 'VALIDATED',
    generationMode: 'EVIDENCE_GROUNDED',
    version: 1,
    contentJson: {
      concept: 'High-contrast split visual showing a glowing thermal phone next to clean battery meters',
      visualSubject: 'Smartphone displaying glowing charging graph with thermal color palette',
      composition: 'Rule of thirds close-up with soft background studio bokeh',
      textOverlay: 'FIX BATTERY DRAIN',
      emotion: 'Focused, eye-catching, authoritative',
      style: 'Clean modern tech digital photography with crisp high contrast'
    },
    createdAt: new Date(Date.now() - 3100000).toISOString(),
    updatedAt: new Date(Date.now() - 3100000).toISOString()
  },
  {
    id: 'prod-asset-7',
    userId: 'user-default-1',
    recommendationId: 'rec-1',
    recommendationTitle: 'Deep Dive: Battery Optimization & Fast Charging in 2026',
    calendarItemId: 'cal-item-1',
    assetType: 'PRODUCTION_CHECKLIST',
    status: 'VALIDATED',
    generationMode: 'EVIDENCE_GROUNDED',
    version: 1,
    contentJson: {
      items: [
        { phase: 'PRE_PRODUCTION', task: 'Verify benchmark test data on 5 devices', completed: true },
        { phase: 'PRE_PRODUCTION', task: 'Select final title and opening hook variant', completed: true },
        { phase: 'PRODUCTION', task: 'Record intro hook and problem framing', completed: false },
        { phase: 'PRODUCTION', task: 'Record step-by-step settings walkthrough', completed: false },
        { phase: 'PRODUCTION', task: 'Record outro and comment CTA', completed: false },
        { phase: 'POST_PRODUCTION', task: 'Edit A-roll and insert thermal graphic overlays', completed: false },
        { phase: 'POST_PRODUCTION', task: 'Review claims against verified audience evidence', completed: false },
        { phase: 'POST_PRODUCTION', task: 'Final creator sign-off', completed: false }
      ]
    },
    createdAt: new Date(Date.now() - 3000000).toISOString(),
    updatedAt: new Date(Date.now() - 3000000).toISOString()
  }
];

export const productionService = {
  async generateProductionAssets(payload: ProductionGenerateRequest): Promise<ProductionAsset[]> {
    try {
      const response = await apiClient.post<any>('/production-assets/generate', payload);
      if (response.data && response.data.data) {
        return response.data.data;
      }
    } catch (e) {
      console.warn('API call failed, using mock generator fallback', e);
    }

    await mockDelay(600);
    return inMemoryAssets.filter(a => a.recommendationId === payload.recommendationId);
  },

  async getProductionAssets(params?: {
    recommendationId?: string;
    calendarItemId?: string;
    assetType?: string;
    status?: string;
    page?: number;
    size?: number;
  }): Promise<{ content: ProductionAsset[]; totalElements: number; totalPages: number }> {
    try {
      const response = await apiClient.get<any>('/production-assets', { params });
      if (response.data && response.data.data) {
        return response.data.data;
      }
    } catch (e) {
      console.warn('API call failed, using mock assets fallback', e);
    }

    await mockDelay(300);
    let filtered = [...inMemoryAssets];
    if (params?.recommendationId) {
      filtered = filtered.filter(a => a.recommendationId === params.recommendationId);
    }
    if (params?.calendarItemId) {
      filtered = filtered.filter(a => a.calendarItemId === params.calendarItemId);
    }
    if (params?.assetType) {
      filtered = filtered.filter(a => a.assetType === params.assetType);
    }
    if (params?.status) {
      filtered = filtered.filter(a => a.status === params.status);
    }

    return {
      content: filtered,
      totalElements: filtered.length,
      totalPages: 1
    };
  },

  async getProductionAsset(id: string): Promise<ProductionAsset> {
    try {
      const response = await apiClient.get<any>(`/production-assets/${id}`);
      if (response.data && response.data.data) {
        return response.data.data;
      }
    } catch (e) {
      console.warn('API call failed, using inMemory asset fallback', e);
    }

    await mockDelay(200);
    const found = inMemoryAssets.find(a => a.id === id);
    if (!found) throw new Error('Asset not found');
    return found;
  },

  async updateProductionAsset(id: string, payload: UpdateProductionAssetRequest): Promise<ProductionAsset> {
    try {
      const response = await apiClient.patch<any>(`/production-assets/${id}`, payload);
      if (response.data && response.data.data) {
        return response.data.data;
      }
    } catch (e) {
      console.warn('API call failed, updating mock asset in memory', e);
    }

    await mockDelay(300);
    const idx = inMemoryAssets.findIndex(a => a.id === id);
    if (idx === -1) throw new Error('Asset not found');

    const prev = inMemoryAssets[idx];
    const updated: ProductionAsset = {
      ...prev,
      version: prev.version + 1,
      status: 'EDITED',
      contentJson: payload.contentJson || {
        ...prev.contentJson,
        ...(payload.title ? { title: payload.title } : {}),
        ...(payload.hook ? { hook: payload.hook } : {}),
        ...(payload.cta ? { cta: payload.cta } : {})
      },
      updatedAt: new Date().toISOString()
    };
    inMemoryAssets[idx] = updated;
    return updated;
  },

  async approveProductionAsset(id: string): Promise<ProductionAsset> {
    try {
      const response = await apiClient.post<any>(`/production-assets/${id}/approve`);
      if (response.data && response.data.data) {
        return response.data.data;
      }
    } catch (e) {
      console.warn('API call failed, approving in mock storage', e);
    }

    await mockDelay(300);
    const idx = inMemoryAssets.findIndex(a => a.id === id);
    if (idx === -1) throw new Error('Asset not found');

    const approved: ProductionAsset = {
      ...inMemoryAssets[idx],
      status: 'APPROVED',
      approvedAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    inMemoryAssets[idx] = approved;
    return approved;
  },

  async archiveProductionAsset(id: string): Promise<ProductionAsset> {
    try {
      const response = await apiClient.post<any>(`/production-assets/${id}/archive`);
      if (response.data && response.data.data) {
        return response.data.data;
      }
    } catch (e) {
      console.warn('API call failed, archiving in mock storage', e);
    }

    await mockDelay(300);
    const idx = inMemoryAssets.findIndex(a => a.id === id);
    if (idx === -1) throw new Error('Asset not found');

    const archived: ProductionAsset = {
      ...inMemoryAssets[idx],
      status: 'ARCHIVED',
      updatedAt: new Date().toISOString()
    };
    inMemoryAssets[idx] = archived;
    return archived;
  },

  async getProductionAssetEvidence(id: string): Promise<Record<string, any>> {
    try {
      const response = await apiClient.get<any>(`/production-assets/${id}/evidence`);
      if (response.data && response.data.data) {
        return response.data.data;
      }
    } catch (e) {
      console.warn('API call failed, getting mock evidence', e);
    }

    await mockDelay(200);
    const found = inMemoryAssets.find(a => a.id === id);
    return found?.evidenceSnapshot || {};
  },

  // ==========================================
  // Phase 3L: Multi-Format Exporter Methods
  // ==========================================

  async createExport(assetId: string, payload: { exportType: string }): Promise<any> {
    try {
      const response = await apiClient.post<any>(`/production-assets/${assetId}/exports`, payload);
      if (response.data && response.data.data) {
        return response.data.data;
      }
    } catch (e) {
      console.warn('Export API call failed, using mock export fallback', e);
    }

    await mockDelay(300);
    return {
      id: `export-${Date.now()}`,
      productionAssetId: assetId,
      recommendationId: 'rec-1',
      exportType: payload.exportType,
      fileName: `pulsegpt-production-${assetId.substring(0, 8)}-v1.${payload.exportType === 'PDF' ? 'pdf' : payload.exportType === 'PRODUCTION_PACKAGE' ? 'zip' : 'md'}`,
      mimeType: payload.exportType === 'PDF' ? 'application/pdf' : payload.exportType === 'PRODUCTION_PACKAGE' ? 'application/zip' : 'text/markdown',
      contentHash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
      version: 1,
      createdAt: new Date().toISOString(),
      downloadUrl: `/production-exports/export-${Date.now()}/download`
    };
  },

  async createProductionPackage(assetId: string): Promise<any> {
    try {
      const response = await apiClient.post<any>(`/production-assets/${assetId}/package`);
      if (response.data && response.data.data) {
        return response.data.data;
      }
    } catch (e) {
      console.warn('Package API call failed, using mock package fallback', e);
    }

    return this.createExport(assetId, { exportType: 'PRODUCTION_PACKAGE' });
  },

  async getExports(assetId: string): Promise<{ content: any[]; totalElements: number; totalPages: number }> {
    try {
      const response = await apiClient.get<any>(`/production-assets/${assetId}/exports`);
      if (response.data && response.data.data) {
        return response.data.data;
      }
    } catch (e) {
      console.warn('List exports API call failed, returning empty list', e);
    }

    await mockDelay(200);
    return {
      content: [],
      totalElements: 0,
      totalPages: 0
    };
  },

  async downloadExport(exportId: string, fileName: string): Promise<void> {
    try {
      const blob = await apiClient.download(`/production-exports/${exportId}/download`);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (e) {
      console.error('Download export failed:', e);
      throw e;
    }
  }
};

