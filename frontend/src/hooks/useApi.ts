import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { dashboardService } from '../services/dashboardService';
import { commentsService } from '../services/commentsService';
import { intelligenceService } from '../services/intelligenceService';
import { plannerService, type PlannerCard } from '../services/plannerService';
import { chatService } from '../services/chatService';
import { integrationsService } from '../services/integrationsService';
import { evaluationService, adminService } from '../services/adminService';
import { productionService } from '../services/productionService';
import { researchService } from '../services/researchService';
import type {
  ContentRecommendation,
  ProductionGenerateRequest,
  UpdateProductionAssetRequest,
  CreateExperimentRequest,
  CreateSnapshotRequest,
} from '../types/models';

import { calendarService, type CreateCalendarItemPayload, type UpdateCalendarItemPayload, type CalendarQueryParams } from '../services/calendarService';
import { apiClient } from '../lib/apiClient';

// Query Keys
export const queryKeys = {
  dashboardOverview: ['dashboard', 'overview'],
  comments: (filters?: any) => ['comments', filters],
  audienceOverview: ['audience', 'overview'],
  trends: (category?: string) => ['trends', category],
  audienceMemory: ['audience', 'memory'],
  recommendations: ['recommendations'],
  productionAssets: (filters?: any) => ['productionAssets', filters],
  productionAsset: (id: string) => ['productionAsset', id],
  productionAssetEvidence: (id: string) => ['productionAssetEvidence', id],
  plannerCards: ['planner', 'cards'],
  plannerSuggestions: ['planner', 'suggestions'],
  calendarEvents: ['calendar', 'events'],
  calendarItems: (filters?: any) => ['calendar', 'items', filters],
  calendarSuggestions: (date?: string, platform?: string, tz?: string) => ['calendar', 'suggestions', date, platform, tz],
  integrations: ['integrations', 'accounts'],
  evaluation: ['evaluation', 'metrics'],
  adminStats: ['admin', 'stats'],
  adminUsers: ['admin', 'users'],
};


// 1. Dashboard Overview
export function useDashboardOverview() {
  return useQuery({
    queryKey: queryKeys.dashboardOverview,
    queryFn: () => dashboardService.getOverviewMetrics(),
  });
}

// 2. Comments
export function useComments(filters?: {
  platform?: string;
  sentiment?: string;
  priority?: string;
  search?: string;
}) {
  return useQuery({
    queryKey: queryKeys.comments(filters),
    queryFn: () => commentsService.getComments(filters),
  });
}

export function usePostReplyMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ commentId, text }: { commentId: string; text: string }) =>
      commentsService.postReply(commentId, text),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['comments'] });
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboardOverview });
    },
  });
}

// 3. Audience Intelligence
export function useAudienceOverview() {
  return useQuery({
    queryKey: queryKeys.audienceOverview,
    queryFn: () => intelligenceService.getAudienceOverview(),
  });
}

// 4. Trends
export function useTrends(category?: string) {
  return useQuery({
    queryKey: queryKeys.trends(category),
    queryFn: () => intelligenceService.getTrends(category),
  });
}

// 5. Memory
export function useAudienceMemory() {
  return useQuery({
    queryKey: queryKeys.audienceMemory,
    queryFn: () => intelligenceService.getAudienceMemory(),
  });
}

// 6. Recommendations
export function useRecommendations() {
  return useQuery({
    queryKey: queryKeys.recommendations,
    queryFn: async () => {
      try {
        const res = await apiClient.get<any>('/recommendations');
        if (res?.data?.content) return res.data.content;
      } catch (err) {
        // fallback
      }
      return intelligenceService.getRecommendations();
    },
  });
}

export function useApproveRecommendation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      try {
        const res = await apiClient.post<any>(`/recommendations/${id}/approve`);
        if (res?.data) return res.data;
      } catch (err) {
        // fallback mock
      }
      return intelligenceService.updateRecommendationStatus(id, 'APPROVED' as any);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.recommendations });
    },
  });
}

export function useUpdateRecommendationStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      status,
    }: {
      id: string;
      status: ContentRecommendation['status'];
    }) => intelligenceService.updateRecommendationStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.recommendations });
    },
  });
}

// 7. Planner Kanban
export function usePlannerCards() {
  return useQuery({
    queryKey: queryKeys.plannerCards,
    queryFn: () => plannerService.getCards(),
  });
}

export function useCreatePlannerCard() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (card: Omit<PlannerCard, 'id'>) => plannerService.createCard(card),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.plannerCards });
    },
  });
}

export function useUpdatePlannerCardStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: PlannerCard['status'] }) =>
      plannerService.updateCardStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.plannerCards });
    },
  });
}

export function useDeletePlannerCard() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => plannerService.deleteCard(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.plannerCards });
    },
  });
}

export function usePlannerSuggestions() {
  return useQuery({
    queryKey: queryKeys.plannerSuggestions,
    queryFn: () => plannerService.getAiSuggestions(),
  });
}

// 8. Phase 3J Calendar Items & Planning
export function useCalendarItems(params?: CalendarQueryParams) {
  return useQuery({
    queryKey: queryKeys.calendarItems(params),
    queryFn: () => calendarService.getCalendarItems(params),
  });
}

export function useCreateCalendarItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateCalendarItemPayload) => calendarService.createCalendarItem(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['calendar'] });
      queryClient.invalidateQueries({ queryKey: queryKeys.recommendations });
    },
  });
}

export function useUpdateCalendarItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateCalendarItemPayload }) =>
      calendarService.updateCalendarItem(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['calendar'] });
    },
  });
}

export function useCancelCalendarItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      calendarService.cancelCalendarItem(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['calendar'] });
    },
  });
}

export function useDeleteCalendarItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => calendarService.deleteCalendarItem(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['calendar'] });
    },
  });
}

export function useCalendarSuggestions(date?: string, platform?: string, timezone?: string) {
  return useQuery({
    queryKey: queryKeys.calendarSuggestions(date, platform, timezone),
    queryFn: () => calendarService.getSuggestions(date, platform, timezone),
  });
}

// 9. Legacy Calendar Events (Adapter)
export function useCalendarEvents() {
  return useQuery({
    queryKey: queryKeys.calendarEvents,
    queryFn: () => plannerService.getCalendarEvents(),
  });
}

// 9. Chat RAG
export function useChatMutation() {
  return useMutation({
    mutationFn: (query: string) => chatService.sendQuery(query),
  });
}

// 10. Integrations
export function useIntegrations() {
  return useQuery({
    queryKey: queryKeys.integrations,
    queryFn: () => integrationsService.getAccounts(),
  });
}

export function useConnectAccountMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ platform, handle }: { platform: string; handle: string }) =>
      integrationsService.connectAccount(platform, handle),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.integrations });
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboardOverview });
    },
  });
}

export function useDisconnectAccountMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (platform: string) => integrationsService.disconnectAccount(platform),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.integrations });
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboardOverview });
    },
  });
}

// 11. Evaluation
export function useEvaluationMetrics() {
  return useQuery({
    queryKey: queryKeys.evaluation,
    queryFn: () => evaluationService.getMetrics(),
  });
}

// 12. Admin
export function useAdminStats() {
  return useQuery({
    queryKey: queryKeys.adminStats,
    queryFn: () => adminService.getStats(),
  });
}

export function useAdminUsers() {
  return useQuery({
    queryKey: queryKeys.adminUsers,
    queryFn: () => adminService.getUsers(),
  });
}

export function useUpdateUserRoleMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: 'USER' | 'ADMIN' }) =>
      adminService.updateUserRole(userId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.adminUsers });
    },
  });
}

export function useUpdateUserStatusMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, status }: { userId: string; status: 'active' | 'suspended' }) =>
      adminService.updateUserStatus(userId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.adminUsers });
    },
  });
}

// ==========================================
// Phase 3K: Production Copilot Hooks
// ==========================================

export function useProductionAssets(params?: {
  recommendationId?: string;
  calendarItemId?: string;
  assetType?: string;
  status?: string;
  page?: number;
  size?: number;
}) {
  return useQuery({
    queryKey: queryKeys.productionAssets(params),
    queryFn: () => productionService.getProductionAssets(params),
  });
}

export function useProductionAsset(id: string) {
  return useQuery({
    queryKey: queryKeys.productionAsset(id),
    queryFn: () => productionService.getProductionAsset(id),
    enabled: Boolean(id),
  });
}

export function useProductionAssetEvidence(id: string) {
  return useQuery({
    queryKey: queryKeys.productionAssetEvidence(id),
    queryFn: () => productionService.getProductionAssetEvidence(id),
    enabled: Boolean(id),
  });
}

export function useGenerateProductionAssetsMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ProductionGenerateRequest) =>
      productionService.generateProductionAssets(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productionAssets'] });
    },
  });
}

export function useUpdateProductionAssetMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateProductionAssetRequest }) =>
      productionService.updateProductionAsset(id, payload),
    onSuccess: (_data, vars) => {
      queryClient.invalidateQueries({ queryKey: ['productionAssets'] });
      queryClient.invalidateQueries({ queryKey: queryKeys.productionAsset(vars.id) });
    },
  });
}

export function useApproveProductionAssetMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => productionService.approveProductionAsset(id),
    onSuccess: (_data, id) => {
      queryClient.invalidateQueries({ queryKey: ['productionAssets'] });
      queryClient.invalidateQueries({ queryKey: queryKeys.productionAsset(id) });
    },
  });
}

export function useArchiveProductionAssetMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => productionService.archiveProductionAsset(id),
    onSuccess: (_data, id) => {
      queryClient.invalidateQueries({ queryKey: ['productionAssets'] });
      queryClient.invalidateQueries({ queryKey: queryKeys.productionAsset(id) });
    },
  });
}

// ==========================================
// Phase 3L: Exporter & Packaging Hooks
// ==========================================

export function useProductionExports(assetId?: string) {
  return useQuery({
    queryKey: ['productionExports', assetId],
    queryFn: () => (assetId ? productionService.getExports(assetId) : Promise.resolve({ content: [], totalElements: 0, totalPages: 0 })),
    enabled: Boolean(assetId),
  });
}

export function useExportAssetMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ assetId, exportType }: { assetId: string; exportType: string }) =>
      productionService.createExport(assetId, { exportType }),
    onSuccess: (_data, vars) => {
      queryClient.invalidateQueries({ queryKey: ['productionExports', vars.assetId] });
    },
  });
}

export function useCreatePackageMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (assetId: string) => productionService.createProductionPackage(assetId),
    onSuccess: (_data, assetId) => {
      queryClient.invalidateQueries({ queryKey: ['productionExports', assetId] });
    },
  });
}

// ==========================================
// Phase 3M: Research, Observability & Experimentation Hooks
// ==========================================

export function useResearchMetrics() {
  return useQuery({
    queryKey: ['research', 'metrics'],
    queryFn: () => researchService.getMetrics(),
    refetchInterval: 30000,
  });
}

export function useExperiments() {
  return useQuery({
    queryKey: ['research', 'experiments'],
    queryFn: () => researchService.getExperiments(),
  });
}

export function useExperiment(id?: string) {
  return useQuery({
    queryKey: ['research', 'experiments', id],
    queryFn: () => (id ? researchService.getExperimentById(id) : Promise.reject('No ID')),
    enabled: Boolean(id),
  });
}

export function useCreateExperimentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateExperimentRequest) => researchService.createExperiment(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['research', 'experiments'] });
      queryClient.invalidateQueries({ queryKey: ['research', 'metrics'] });
    },
  });
}

export function useStartExperimentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => researchService.startExperiment(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['research', 'experiments'] });
    },
  });
}

export function useCompleteExperimentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, metrics }: { id: string; metrics?: Record<string, any> }) =>
      researchService.completeExperiment(id, metrics),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['research', 'experiments'] });
      queryClient.invalidateQueries({ queryKey: ['research', 'metrics'] });
    },
  });
}

export function useClusteringStability(runId?: string, compareRunId?: string) {
  return useQuery({
    queryKey: ['research', 'clusteringStability', runId, compareRunId],
    queryFn: () => (runId ? researchService.getClusteringStability(runId, compareRunId) : Promise.reject('No Run ID')),
    enabled: Boolean(runId),
  });
}

export function useDatasetSnapshots() {
  return useQuery({
    queryKey: ['research', 'snapshots'],
    queryFn: () => researchService.getSnapshots(),
  });
}

export function useCreateSnapshotMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateSnapshotRequest) => researchService.createSnapshot(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['research', 'snapshots'] });
      queryClient.invalidateQueries({ queryKey: ['research', 'metrics'] });
    },
  });
}

// ==========================================
// Phase 3O: RAG & Graph Retrieval Hooks
// ==========================================

import { ragService } from '../services/ragService';
import type { EvidenceAnnotationRequest } from '../types/rag';

export function useRagEvaluation() {
  return useQuery({
    queryKey: ['rag', 'evaluation'],
    queryFn: () => ragService.getEvaluationMetrics(),
    refetchInterval: 30000,
  });
}

export function useCreateAnnotationMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: EvidenceAnnotationRequest) => ragService.createAnnotation(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rag', 'evaluation'] });
    },
  });
}


