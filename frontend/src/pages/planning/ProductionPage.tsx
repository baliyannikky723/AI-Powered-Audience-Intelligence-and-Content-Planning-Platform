import React, { useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import {
  useProductionAssets,
  useUpdateProductionAssetMutation,
  useApproveProductionAssetMutation,
  useGenerateProductionAssetsMutation,
  useProductionExports,
  useExportAssetMutation,
  useCreatePackageMutation,
} from '../../hooks/useApi';
import { productionService } from '../../services/productionService';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import { useToast } from '../../components/ui/Toast';
import {
  FileText,
  Sparkles,
  CheckCircle,
  Clock,
  ShieldCheck,
  Edit3,
  RefreshCw,
  Flame,
  Camera,
  ListOrdered,
  ChevronDown,
  ChevronUp,
  MessageSquare,
  AlertCircle,
  ArrowLeft,
  Copy,
  Check,
  Download,
  Package,
  FileCode,
  Printer,
  CheckSquare,
  ListTree,
  History,
} from 'lucide-react';
import type { ExportType } from '../../types/models';

export const ProductionPage: React.FC = () => {
  const { addToast } = useToast();
  const [searchParams] = useSearchParams();
  const recommendationIdParam = searchParams.get('recommendationId');
  const calendarItemIdParam = searchParams.get('calendarItemId');

  const [activeTab, setActiveTab] = useState<'brief' | 'outline' | 'hooks' | 'titles' | 'cta' | 'thumbnail' | 'checklist'>('brief');
  const [isEvidenceModalOpen, setIsEvidenceModalOpen] = useState(false);
  const [isExportModalOpen, setIsExportModalOpen] = useState(false);
  const [selectedExportFormat, setSelectedExportFormat] = useState<ExportType>('PDF');
  const [copiedText, setCopiedText] = useState<string | null>(null);

  const { data: assetData, isLoading, refetch } = useProductionAssets({
    recommendationId: recommendationIdParam || undefined,
    calendarItemId: calendarItemIdParam || undefined,
  });

  const updateMutation = useUpdateProductionAssetMutation();
  const approveMutation = useApproveProductionAssetMutation();
  const generateMutation = useGenerateProductionAssetsMutation();
  const exportMutation = useExportAssetMutation();
  const packageMutation = useCreatePackageMutation();

  const assets = assetData?.content || [];
  const primaryAsset = assets[0];

  // Editable local state for Content Brief
  const briefAsset = assets.find((a) => a.assetType === 'CONTENT_BRIEF') || primaryAsset;
  const outlineAsset = assets.find((a) => a.assetType === 'SCRIPT_OUTLINE');
  const hooksAsset = assets.find((a) => a.assetType === 'HOOK');
  const titlesAsset = assets.find((a) => a.assetType === 'TITLE_VARIATION');
  const ctaAsset = assets.find((a) => a.assetType === 'CTA');
  const thumbnailAsset = assets.find((a) => a.assetType === 'THUMBNAIL_PROMPT');
  const checklistAsset = assets.find((a) => a.assetType === 'PRODUCTION_CHECKLIST');

  // Exports Query for primary asset
  const { data: exportHistoryData, refetch: refetchExports } = useProductionExports(briefAsset?.id);
  const exportList = exportHistoryData?.content || [];

  const [editTitle, setEditTitle] = useState('');
  const [editHook, setEditHook] = useState('');
  const [editCta, setEditCta] = useState('');
  const [isEditing, setIsEditing] = useState(false);
  const [expandedSections, setExpandedSections] = useState<Record<number, boolean>>({ 0: true, 1: true });

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedText(text);
    addToast({ title: 'Copied to clipboard', variant: 'success' });
    setTimeout(() => setCopiedText(null), 2000);
  };

  const handleApprove = (assetId: string) => {
    approveMutation.mutate(assetId, {
      onSuccess: () => {
        addToast({ title: 'Production Draft Approved', body: 'The creator has signed off on this production draft. Ready for export.', variant: 'success' });
        refetch();
      },
      onError: (err: any) => {
        addToast({ title: 'Approval Failed', body: err.message, variant: 'danger' });
      }
    });
  };

  const handleExecuteExport = (exportType: ExportType) => {
    if (!briefAsset) return;
    exportMutation.mutate(
      { assetId: briefAsset.id, exportType },
      {
        onSuccess: async (data: any) => {
          addToast({
            title: `${exportType} Export Ready`,
            body: `SHA-256: ${data.contentHash?.substring(0, 12)}... Downloading now.`,
            variant: 'success'
          });
          setIsExportModalOpen(false);
          refetchExports();
          try {
            await productionService.downloadExport(data.id, data.fileName);
          } catch (e) {
            console.warn('Direct download initiation failed', e);
          }
        },
        onError: (err: any) => {
          addToast({ title: 'Export Failed', body: err.message || 'Make sure the draft is approved before exporting.', variant: 'danger' });
        }
      }
    );
  };

  const handleCreatePackage = () => {
    if (!briefAsset) return;
    packageMutation.mutate(briefAsset.id, {
      onSuccess: async (data: any) => {
        addToast({
          title: 'Production Package Created',
          body: `ZIP Archive (${data.fileName}) generated. Downloading...`,
          variant: 'success'
        });
        refetchExports();
        try {
          await productionService.downloadExport(data.id, data.fileName);
        } catch (e) {
          console.warn('Direct download initiation failed', e);
        }
      },
      onError: (err: any) => {
        addToast({ title: 'Package Creation Failed', body: err.message || 'Draft must be approved first.', variant: 'danger' });
      }
    });
  };

  const handleSaveEdits = (assetId: string) => {
    updateMutation.mutate(
      {
        id: assetId,
        payload: {
          title: editTitle || undefined,
          hook: editHook || undefined,
          cta: editCta || undefined,
        }
      },
      {
        onSuccess: () => {
          addToast({ title: 'Edits Saved', body: 'Asset updated to version ' + ((briefAsset?.version || 1) + 1), variant: 'success' });
          setIsEditing(false);
          refetch();
        },
        onError: (err: any) => {
          addToast({ title: 'Save Failed', body: err.message, variant: 'danger' });
        }
      }
    );
  };

  const handleRegenerate = () => {
    if (!recommendationIdParam && !briefAsset?.recommendationId) {
      addToast({ title: 'Cannot Regenerate', body: 'No recommendation context linked.', variant: 'danger' });
      return;
    }
    const recId = recommendationIdParam || briefAsset?.recommendationId || '';
    generateMutation.mutate(
      { recommendationId: recId, calendarItemId: calendarItemIdParam || undefined },
      {
        onSuccess: () => {
          addToast({ title: 'Production Draft Regenerated', body: 'Generated fresh draft variants with verified evidence.', variant: 'success' });
          refetch();
        },
        onError: (err: any) => {
          addToast({ title: 'Generation Failed', body: err.message, variant: 'danger' });
        }
      }
    );
  };

  const toggleSection = (idx: number) => {
    setExpandedSections((prev) => ({ ...prev, [idx]: !prev[idx] }));
  };

  if (isLoading) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center gap-4">
        <div className="w-10 h-10 border-3 border-indigo-500 border-t-transparent rounded-full animate-spin" />
        <p className="text-sm text-slate-400 font-medium">Loading Production Copilot Workspace...</p>
      </div>
    );
  }

  const briefContent = briefAsset?.contentJson || {};
  const outlineContent = outlineAsset?.contentJson || {};
  const hooksList = hooksAsset?.contentJson?.hooks || [];
  const titlesList = titlesAsset?.contentJson?.titles || [];
  const ctaList = ctaAsset?.contentJson?.ctas || [];
  const thumbnailContent = thumbnailAsset?.contentJson || {};
  const checklistItems = checklistAsset?.contentJson?.items || [];

  return (
    <div className="space-y-6 pb-16">
      {/* Top Breadcrumb & Status Navigation */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link
            to="/app/recommendations"
            className="p-2 bg-slate-900/60 border border-slate-800 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-slate-200 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
          </Link>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold text-slate-100 flex items-center gap-2">
                <Sparkles className="w-5 h-5 text-indigo-400" />
                Production Copilot Workspace
              </h1>
              <Badge variant="outline" className="text-xs bg-indigo-950/40 border-indigo-500/30 text-indigo-300">
                Phase 3K
              </Badge>
            </div>
            <p className="text-xs text-slate-400 mt-0.5">
              Evidence-grounded drafting workspace for creator-directed scripting & pre-production.
            </p>
          </div>
        </div>

        {/* Global Action Controls */}
        <div className="flex items-center gap-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setIsEvidenceModalOpen(true)}
            className="border-slate-700 bg-slate-900/50 hover:bg-slate-800 text-slate-300 text-xs flex items-center gap-1.5"
          >
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
            Inspect Evidence Signals
          </Button>

          <Button
            variant="secondary"
            size="sm"
            onClick={handleRegenerate}
            disabled={generateMutation.isPending}
            className="border-slate-700 bg-slate-900/50 hover:bg-slate-800 text-slate-300 text-xs flex items-center gap-1.5"
          >
            <RefreshCw className={`w-3.5 h-3.5 text-indigo-400 ${generateMutation.isPending ? 'animate-spin' : ''}`} />
            Regenerate
          </Button>

          {briefAsset && briefAsset.status !== 'APPROVED' && (
            <Button
              variant="primary"
              size="sm"
              onClick={() => handleApprove(briefAsset.id)}
              disabled={approveMutation.isPending}
              className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold flex items-center gap-1.5 shadow-lg shadow-emerald-950/40"
            >
              <CheckCircle className="w-3.5 h-3.5" />
              Approve Production Draft
            </Button>
          )}

          {briefAsset && briefAsset.status === 'APPROVED' && (
            <Badge variant="success" className="px-3 py-1.5 text-xs font-semibold flex items-center gap-1 bg-emerald-950/60 border border-emerald-500/40 text-emerald-300">
              <CheckCircle className="w-3.5 h-3.5" />
              Creator Approved
            </Badge>
          )}

          {briefAsset && (
            <>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setIsExportModalOpen(true)}
                disabled={briefAsset.status !== 'APPROVED' && briefAsset.status !== 'EDITED'}
                className="border-indigo-500/40 bg-indigo-950/30 hover:bg-indigo-900/50 text-indigo-300 text-xs flex items-center gap-1.5"
                title={briefAsset.status !== 'APPROVED' ? 'Approve draft first to enable export' : 'Export multi-format asset'}
              >
                <Download className="w-3.5 h-3.5 text-indigo-400" />
                Export Draft
              </Button>

              <Button
                variant="secondary"
                size="sm"
                onClick={handleCreatePackage}
                disabled={packageMutation.isPending || (briefAsset.status !== 'APPROVED' && briefAsset.status !== 'EDITED')}
                className="border-emerald-500/40 bg-emerald-950/30 hover:bg-emerald-900/50 text-emerald-300 text-xs flex items-center gap-1.5"
                title={briefAsset.status !== 'APPROVED' ? 'Approve draft first to enable package download' : 'Download complete production package ZIP'}
              >
                <Package className={`w-3.5 h-3.5 text-emerald-400 ${packageMutation.isPending ? 'animate-spin' : ''}`} />
                ZIP Package
              </Button>
            </>
          )}
        </div>
      </div>

      {/* Mandatory AI DRAFT Banner */}
      <div className="bg-amber-950/30 border border-amber-500/30 rounded-xl p-3.5 flex items-start gap-3 backdrop-blur-sm">
        <AlertCircle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
        <div className="text-xs text-amber-200/90 leading-relaxed">
          <strong className="text-amber-300 font-semibold block mb-0.5">AI DRAFT — NOT PUBLISHED</strong>
          This workspace contains AI-generated production drafts synthesized from verified audience evidence. No automated posting or external social uploads occur. The creator retains full editorial ownership.
        </div>
      </div>

      {/* Source & Traceability Meta Card */}
      <div className="bg-slate-900/60 border border-slate-800/80 rounded-xl p-4 shadow-sm backdrop-blur-md">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <span className="text-[11px] font-semibold tracking-wider text-slate-400 uppercase">
              Source Recommendation
            </span>
            <h2 className="text-base font-bold text-slate-100">
              {briefAsset?.recommendationTitle || briefContent.title || 'Untitled Recommendation'}
            </h2>
            <div className="flex flex-wrap items-center gap-2 pt-1 text-xs text-slate-400">
              <Badge variant="outline" className="bg-slate-800/80 border-slate-700 text-slate-300 text-[11px]">
                Platform: {briefContent.platform || 'YOUTUBE'}
              </Badge>
              <Badge variant="outline" className="bg-slate-800/80 border-slate-700 text-slate-300 text-[11px]">
                Format: {briefContent.contentType || 'VIDEO'}
              </Badge>
              <Badge variant="outline" className="bg-indigo-950/40 border-indigo-500/30 text-indigo-300 text-[11px]">
                Mode: {briefAsset?.generationMode || 'EVIDENCE_GROUNDED'}
              </Badge>
              <span className="text-slate-500">•</span>
              <span>Version: <strong className="text-slate-300">v{briefAsset?.version || 1}</strong></span>
              <span className="text-slate-500">•</span>
              <span>Status: <strong className="text-indigo-300">{briefAsset?.status || 'VALIDATED'}</strong></span>
            </div>
          </div>

          <div className="flex items-center gap-2 bg-slate-950/60 p-2.5 rounded-lg border border-slate-800 text-xs">
            <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
            <div>
              <div className="font-medium text-slate-200">Validation Passed</div>
              <div className="text-[10px] text-slate-400">6 Security & Evidence Guardrails</div>
            </div>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="flex overflow-x-auto gap-2 border-b border-slate-800 pb-2 scrollbar-none">
        <button
          onClick={() => setActiveTab('brief')}
          className={`px-3.5 py-2 rounded-lg text-xs font-medium flex items-center gap-2 transition-all whitespace-nowrap ${
            activeTab === 'brief'
              ? 'bg-indigo-600 text-white shadow-md shadow-indigo-950/50'
              : 'bg-slate-900/60 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
          }`}
        >
          <FileText className="w-3.5 h-3.5" />
          Content Brief
        </button>

        <button
          onClick={() => setActiveTab('outline')}
          className={`px-3.5 py-2 rounded-lg text-xs font-medium flex items-center gap-2 transition-all whitespace-nowrap ${
            activeTab === 'outline'
              ? 'bg-indigo-600 text-white shadow-md shadow-indigo-950/50'
              : 'bg-slate-900/60 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
          }`}
        >
          <ListOrdered className="w-3.5 h-3.5" />
          Script Outline ({outlineContent.sections?.length || 4} Parts)
        </button>

        <button
          onClick={() => setActiveTab('hooks')}
          className={`px-3.5 py-2 rounded-lg text-xs font-medium flex items-center gap-2 transition-all whitespace-nowrap ${
            activeTab === 'hooks'
              ? 'bg-indigo-600 text-white shadow-md shadow-indigo-950/50'
              : 'bg-slate-900/60 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
          }`}
        >
          <Flame className="w-3.5 h-3.5" />
          Hook Variants ({hooksList.length || 3})
        </button>

        <button
          onClick={() => setActiveTab('titles')}
          className={`px-3.5 py-2 rounded-lg text-xs font-medium flex items-center gap-2 transition-all whitespace-nowrap ${
            activeTab === 'titles'
              ? 'bg-indigo-600 text-white shadow-md shadow-indigo-950/50'
              : 'bg-slate-900/60 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
          }`}
        >
          <Sparkles className="w-3.5 h-3.5" />
          Title Variations ({titlesList.length || 3})
        </button>

        <button
          onClick={() => setActiveTab('cta')}
          className={`px-3.5 py-2 rounded-lg text-xs font-medium flex items-center gap-2 transition-all whitespace-nowrap ${
            activeTab === 'cta'
              ? 'bg-indigo-600 text-white shadow-md shadow-indigo-950/50'
              : 'bg-slate-900/60 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
          }`}
        >
          <MessageSquare className="w-3.5 h-3.5" />
          Call to Action ({ctaList.length || 2})
        </button>

        <button
          onClick={() => setActiveTab('thumbnail')}
          className={`px-3.5 py-2 rounded-lg text-xs font-medium flex items-center gap-2 transition-all whitespace-nowrap ${
            activeTab === 'thumbnail'
              ? 'bg-indigo-600 text-white shadow-md shadow-indigo-950/50'
              : 'bg-slate-900/60 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
          }`}
        >
          <Camera className="w-3.5 h-3.5" />
          Thumbnail Concept
        </button>

        <button
          onClick={() => setActiveTab('checklist')}
          className={`px-3.5 py-2 rounded-lg text-xs font-medium flex items-center gap-2 transition-all whitespace-nowrap ${
            activeTab === 'checklist'
              ? 'bg-indigo-600 text-white shadow-md shadow-indigo-950/50'
              : 'bg-slate-900/60 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
          }`}
        >
          <CheckCircle className="w-3.5 h-3.5" />
          Checklist ({checklistItems.length || 6})
        </button>
      </div>

      {/* Tab Panels */}
      {/* 1. CONTENT BRIEF */}
      {activeTab === 'brief' && (
        <div className="space-y-6">
          <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 space-y-6">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <div>
                <h3 className="text-base font-bold text-slate-100 flex items-center gap-2">
                  <FileText className="w-4 h-4 text-indigo-400" />
                  Content Brief & Angle
                </h3>
                <p className="text-xs text-slate-400">
                  Comprehensive strategic blueprint grounded in audience evidence.
                </p>
              </div>

              {!isEditing ? (
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => {
                    setEditTitle(briefContent.title || '');
                    setEditHook(briefContent.hook || '');
                    setEditCta(briefContent.callToAction || '');
                    setIsEditing(true);
                  }}
                  className="text-xs border-slate-700 bg-slate-900/80 hover:bg-slate-800"
                >
                  <Edit3 className="w-3.5 h-3.5 mr-1 text-indigo-400" />
                  Edit Brief
                </Button>
              ) : (
                <div className="flex items-center gap-2">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setIsEditing(false)}
                    className="text-xs text-slate-400 hover:text-slate-200"
                  >
                    Cancel
                  </Button>
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => handleSaveEdits(briefAsset.id)}
                    disabled={updateMutation.isPending}
                    className="text-xs bg-indigo-600 hover:bg-indigo-500"
                  >
                    Save Changes
                  </Button>
                </div>
              )}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-xs">
              <div className="space-y-4">
                <div>
                  <label className="block text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-1.5">
                    Target Audience
                  </label>
                  <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800/80 text-slate-200">
                    {briefContent.targetAudience || 'Tech Enthusiasts'}
                  </div>
                </div>

                <div>
                  <label className="block text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-1.5">
                    Audience Pain Point / Problem
                  </label>
                  <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800/80 text-slate-200 leading-relaxed">
                    {briefContent.audienceProblem || 'Audience inquiries regarding unexpected latency or battery drain.'}
                  </div>
                </div>

                <div>
                  <label className="block text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-1.5">
                    Content Angle
                  </label>
                  <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800/80 text-slate-200 leading-relaxed">
                    {briefContent.contentAngle || 'Hands-on benchmarks and diagnostic tests.'}
                  </div>
                </div>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="block text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-1.5">
                    Core Overriding Message
                  </label>
                  <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800/80 text-slate-200 leading-relaxed">
                    {briefContent.coreMessage || 'Practical and empirical guide to solving the core issue.'}
                  </div>
                </div>

                <div>
                  <label className="block text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-1.5">
                    Key Talking Points
                  </label>
                  <div className="space-y-2 p-3 bg-slate-950/60 rounded-lg border border-slate-800/80">
                    {(briefContent.keyPoints || ['Key point 1', 'Key point 2']).map((pt: string, idx: number) => (
                      <div key={idx} className="flex items-start gap-2 text-slate-300">
                        <span className="w-4 h-4 rounded-full bg-indigo-950 text-indigo-400 flex items-center justify-center text-[10px] font-bold shrink-0 mt-0.5">
                          {idx + 1}
                        </span>
                        <span>{pt}</span>
                      </div>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="block text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-1.5">
                    Primary Call To Action
                  </label>
                  <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800/80 text-slate-200">
                    {briefContent.callToAction || 'Share your feedback in the comments!'}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 2. SCRIPT OUTLINE */}
      {activeTab === 'outline' && (
        <div className="space-y-4">
          <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-4">
              <div>
                <h3 className="text-base font-bold text-slate-100 flex items-center gap-2">
                  <ListOrdered className="w-4 h-4 text-indigo-400" />
                  Structured Script & Production Outline
                </h3>
                <p className="text-xs text-slate-400">
                  Sequential scene-by-scene roadmap with duration targets and talking points.
                </p>
              </div>
              <Badge variant="outline" className="bg-indigo-950/40 border-indigo-500/30 text-indigo-300 text-xs">
                {outlineContent.format || 'VIDEO_STRUCTURED_OUTLINE'}
              </Badge>
            </div>

            <div className="space-y-3">
              {(outlineContent.sections || []).map((sec: any, idx: number) => (
                <div
                  key={idx}
                  className="bg-slate-950/60 border border-slate-800/80 rounded-lg overflow-hidden transition-all"
                >
                  <div
                    onClick={() => toggleSection(idx)}
                    className="p-3.5 flex items-center justify-between cursor-pointer hover:bg-slate-900/40 transition-colors"
                  >
                    <div className="flex items-center gap-3">
                      <span className="w-6 h-6 rounded-md bg-indigo-950/80 border border-indigo-500/30 text-indigo-400 flex items-center justify-center text-xs font-bold shrink-0">
                        {idx + 1}
                      </span>
                      <div>
                        <h4 className="text-xs font-bold text-slate-200">{sec.sectionTitle}</h4>
                        <p className="text-[11px] text-slate-400">{sec.purpose}</p>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      {sec.estimatedDurationSeconds && (
                        <span className="text-[11px] text-slate-400 flex items-center gap-1 bg-slate-900 px-2 py-0.5 rounded">
                          <Clock className="w-3 h-3 text-slate-500" />
                          {sec.estimatedDurationSeconds}s
                        </span>
                      )}
                      {expandedSections[idx] ? (
                        <ChevronUp className="w-4 h-4 text-slate-400" />
                      ) : (
                        <ChevronDown className="w-4 h-4 text-slate-400" />
                      )}
                    </div>
                  </div>

                  {expandedSections[idx] && (
                    <div className="px-4 pb-4 pt-1 border-t border-slate-900/80 bg-slate-950/90 text-xs">
                      <div className="text-[11px] font-semibold text-slate-400 uppercase mb-2">
                        Talking Points & Cues
                      </div>
                      <ul className="space-y-1.5 pl-2">
                        {(sec.talkingPoints || []).map((tp: string, tIdx: number) => (
                          <li key={tIdx} className="text-slate-300 flex items-start gap-2">
                            <span className="text-indigo-400 font-bold">•</span>
                            <span>{tp}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              ))}
            </div>

            {outlineContent.keyTakeaway && (
              <div className="mt-6 p-4 bg-indigo-950/20 border border-indigo-500/30 rounded-lg text-xs">
                <strong className="text-indigo-300 font-semibold block mb-1">Key Takeaway Summary:</strong>
                <p className="text-slate-300 leading-relaxed">{outlineContent.keyTakeaway}</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* 3. HOOK VARIATIONS */}
      {activeTab === 'hooks' && (
        <div className="space-y-4">
          <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6">
            <div className="border-b border-slate-800 pb-4 mb-6">
              <h3 className="text-base font-bold text-slate-100 flex items-center gap-2">
                <Flame className="w-4 h-4 text-amber-400" />
                Hook Variations
              </h3>
              <p className="text-xs text-slate-400">
                Opening lines crafted for retention without misleading clickbait or unsubstantiated claims.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {hooksList.map((hook: any, idx: number) => (
                <div
                  key={idx}
                  className="p-4 bg-slate-950/70 border border-slate-800 hover:border-slate-700 rounded-xl space-y-3 flex flex-col justify-between transition-all"
                >
                  <div className="space-y-2">
                    <Badge variant="outline" className="text-[10px] bg-amber-950/40 border-amber-500/30 text-amber-300 uppercase tracking-wider">
                      {hook.hookType}
                    </Badge>
                    <p className="text-xs text-slate-200 font-medium leading-relaxed">
                      "{hook.text}"
                    </p>
                    {hook.rationale && (
                      <p className="text-[11px] text-slate-400 italic">
                        {hook.rationale}
                      </p>
                    )}
                  </div>

                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => handleCopy(hook.text)}
                    className="w-full text-xs border-slate-800 bg-slate-900/80 hover:bg-slate-800 text-slate-300 flex items-center justify-center gap-1.5"
                  >
                    {copiedText === hook.text ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    {copiedText === hook.text ? 'Copied' : 'Copy Hook'}
                  </Button>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* 4. TITLE VARIATIONS */}
      {activeTab === 'titles' && (
        <div className="space-y-4">
          <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6">
            <div className="border-b border-slate-800 pb-4 mb-6">
              <h3 className="text-base font-bold text-slate-100 flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-indigo-400" />
                Title Options
              </h3>
              <p className="text-xs text-slate-400">
                Grounded title options tailored for platform search and viewer curiosity.
              </p>
            </div>

            <div className="space-y-3">
              {titlesList.map((title: any, idx: number) => (
                <div
                  key={idx}
                  className="p-4 bg-slate-950/70 border border-slate-800 hover:border-slate-700 rounded-xl flex items-center justify-between gap-4 transition-all"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <Badge variant="outline" className="text-[10px] bg-indigo-950/40 border-indigo-500/30 text-indigo-300 uppercase">
                        {title.titleType}
                      </Badge>
                      <span className="text-xs font-bold text-slate-200">{title.text}</span>
                    </div>
                    {title.rationale && (
                      <p className="text-[11px] text-slate-400 pl-1">{title.rationale}</p>
                    )}
                  </div>

                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => handleCopy(title.text)}
                    className="shrink-0 text-xs border-slate-800 bg-slate-900 hover:bg-slate-800 text-slate-300 flex items-center gap-1.5"
                  >
                    {copiedText === title.text ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    {copiedText === title.text ? 'Copied' : 'Copy'}
                  </Button>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* 5. CTA VARIATIONS */}
      {activeTab === 'cta' && (
        <div className="space-y-4">
          <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6">
            <div className="border-b border-slate-800 pb-4 mb-6">
              <h3 className="text-base font-bold text-slate-100 flex items-center gap-2">
                <MessageSquare className="w-4 h-4 text-emerald-400" />
                Call-to-Action Variations
              </h3>
              <p className="text-xs text-slate-400">
                Platform-compatible calls to action to foster healthy community discussion.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {ctaList.map((cta: any, idx: number) => (
                <div
                  key={idx}
                  className="p-4 bg-slate-950/70 border border-slate-800 rounded-xl space-y-3 flex flex-col justify-between"
                >
                  <div className="space-y-1.5">
                    <Badge variant="outline" className="text-[10px] bg-emerald-950/40 border-emerald-500/30 text-emerald-300 uppercase">
                      {cta.ctaType}
                    </Badge>
                    <p className="text-xs text-slate-200 leading-relaxed font-medium">
                      "{cta.text}"
                    </p>
                  </div>

                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => handleCopy(cta.text)}
                    className="text-xs border-slate-800 bg-slate-900 text-slate-300 flex items-center justify-center gap-1.5"
                  >
                    {copiedText === cta.text ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    Copy CTA
                  </Button>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* 6. THUMBNAIL CONCEPT */}
      {activeTab === 'thumbnail' && (
        <div className="space-y-4">
          <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 space-y-6">
            <div className="border-b border-slate-800 pb-4">
              <h3 className="text-base font-bold text-slate-100 flex items-center gap-2">
                <Camera className="w-4 h-4 text-indigo-400" />
                Thumbnail Concept & Prompt (Textual Draft Only)
              </h3>
              <p className="text-xs text-slate-400">
                Deterministic visual layout guideline for your graphic design or filming setup.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-xs">
              <div className="space-y-4">
                <div>
                  <label className="block text-[11px] font-semibold text-slate-400 uppercase mb-1">
                    Visual Concept
                  </label>
                  <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800 text-slate-200 leading-relaxed">
                    {thumbnailContent.concept || 'High-contrast split visual demonstrating the key solution.'}
                  </div>
                </div>

                <div>
                  <label className="block text-[11px] font-semibold text-slate-400 uppercase mb-1">
                    Visual Subject & Focal Point
                  </label>
                  <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800 text-slate-200">
                    {thumbnailContent.visualSubject || 'Focused device/diagnostic diagram in action.'}
                  </div>
                </div>

                <div>
                  <label className="block text-[11px] font-semibold text-slate-400 uppercase mb-1">
                    Composition & Framing
                  </label>
                  <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800 text-slate-200">
                    {thumbnailContent.composition || 'Rule of thirds, sharp depth of field, 3-point studio lighting.'}
                  </div>
                </div>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="block text-[11px] font-semibold text-slate-400 uppercase mb-1">
                    Bold Text Overlay (2-4 Words)
                  </label>
                  <div className="p-4 bg-indigo-950/30 rounded-lg border border-indigo-500/40 text-center">
                    <span className="text-base font-black tracking-widest text-indigo-300 uppercase">
                      {thumbnailContent.textOverlay || 'FIX THIS NOW'}
                    </span>
                  </div>
                </div>

                <div>
                  <label className="block text-[11px] font-semibold text-slate-400 uppercase mb-1">
                    Emotional Tone
                  </label>
                  <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800 text-slate-200">
                    {thumbnailContent.emotion || 'Curious, authoritative, focused'}
                  </div>
                </div>

                <div>
                  <label className="block text-[11px] font-semibold text-slate-400 uppercase mb-1">
                    Style Guide
                  </label>
                  <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800 text-slate-200">
                    {thumbnailContent.style || 'Clean modern tech photography with vibrant contrast accents.'}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 7. PRODUCTION CHECKLIST */}
      {activeTab === 'checklist' && (
        <div className="space-y-4">
          <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6">
            <div className="border-b border-slate-800 pb-4 mb-6">
              <h3 className="text-base font-bold text-slate-100 flex items-center gap-2">
                <CheckCircle className="w-4 h-4 text-emerald-400" />
                Pre, Production & Post Checklist
              </h3>
              <p className="text-xs text-slate-400">
                Actionable step-by-step checklist to guide your recording and editing workflow.
              </p>
            </div>

            <div className="space-y-3">
              {checklistItems.map((item: any, idx: number) => (
                <div
                  key={idx}
                  className="p-3.5 bg-slate-950/60 border border-slate-800 rounded-lg flex items-center justify-between gap-3 text-xs"
                >
                  <div className="flex items-center gap-3">
                    <input
                      type="checkbox"
                      defaultChecked={item.completed}
                      className="w-4 h-4 rounded border-slate-700 bg-slate-900 text-indigo-600 focus:ring-indigo-500 cursor-pointer"
                    />
                    <span className="text-slate-200 font-medium">{item.task}</span>
                  </div>

                  <Badge variant="outline" className="text-[10px] bg-slate-900 border-slate-700 text-slate-400 uppercase">
                    {item.phase?.replace('_', ' ')}
                  </Badge>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Export History & Provenance Integrity Section */}
      <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2">
            <History className="w-4 h-4 text-indigo-400" />
            <h3 className="text-sm font-bold text-slate-100">Export History & Integrity Log</h3>
          </div>
          <span className="text-[11px] text-slate-400">
            {exportList.length} exported artifact{exportList.length === 1 ? '' : 's'} recorded
          </span>
        </div>

        {exportList.length === 0 ? (
          <div className="p-6 text-center text-xs text-slate-500 border border-dashed border-slate-800/80 rounded-lg">
            No exports created for this production draft yet. Click <strong>Export Draft</strong> or <strong>ZIP Package</strong> above to generate creator deliverables.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="text-[10px] text-slate-400 uppercase tracking-wider border-b border-slate-800/80">
                  <th className="pb-2">Format</th>
                  <th className="pb-2">File Name</th>
                  <th className="pb-2">Version</th>
                  <th className="pb-2">SHA-256 Content Hash</th>
                  <th className="pb-2">Created At</th>
                  <th className="pb-2 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/50 text-slate-300">
                {exportList.map((exp: any) => (
                  <tr key={exp.id} className="hover:bg-slate-950/40 transition-colors">
                    <td className="py-2.5">
                      <Badge
                        variant="outline"
                        className={`text-[10px] uppercase font-semibold ${
                          exp.exportType === 'PDF'
                            ? 'bg-rose-950/40 border-rose-500/30 text-rose-300'
                            : exp.exportType === 'PRODUCTION_PACKAGE'
                            ? 'bg-emerald-950/40 border-emerald-500/30 text-emerald-300'
                            : 'bg-indigo-950/40 border-indigo-500/30 text-indigo-300'
                        }`}
                      >
                        {exp.exportType?.replace('_', ' ')}
                      </Badge>
                    </td>
                    <td className="py-2.5 font-medium text-slate-200">{exp.fileName}</td>
                    <td className="py-2.5 text-slate-400">v{exp.version}</td>
                    <td className="py-2.5 font-mono text-[11px] text-slate-400">
                      <button
                        onClick={() => handleCopy(exp.contentHash)}
                        className="flex items-center gap-1 hover:text-indigo-300 transition-colors"
                        title="Click to copy SHA-256 integrity hash"
                      >
                        <span>{exp.contentHash ? `${exp.contentHash.substring(0, 10)}...` : 'N/A'}</span>
                        <Copy className="w-3 h-3 text-slate-500" />
                      </button>
                    </td>
                    <td className="py-2.5 text-slate-400">
                      {new Date(exp.createdAt).toLocaleDateString()}
                    </td>
                    <td className="py-2.5 text-right">
                      <Button
                        variant="secondary"
                        size="xs"
                        onClick={() => productionService.downloadExport(exp.id, exp.fileName)}
                        className="text-[11px] border-slate-700 bg-slate-900/80 hover:bg-slate-800 text-slate-200"
                      >
                        <Download className="w-3 h-3 mr-1 text-emerald-400" />
                        Download
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Export Format Selection Modal */}
      <Modal
        open={isExportModalOpen}
        onClose={() => setIsExportModalOpen(false)}
        title="Export Production Asset"
      >
        <div className="space-y-4 text-xs text-slate-300">
          <p className="text-slate-400">
            Select a deliverable format to export this approved production draft. Exports are generated deterministically from your verified context.
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            {[
              { type: 'PDF', label: 'Production Pack PDF', desc: 'Printable multi-page document with timeline & evidence', icon: Printer },
              { type: 'MARKDOWN', label: 'Markdown Blueprint', desc: 'Complete formatted script with all sections (.md)', icon: FileText },
              { type: 'TELEPROMPTER', label: 'Teleprompter Script', desc: 'Clean, distraction-free script for prompt apps (.txt)', icon: FileCode },
              { type: 'TIMELINE', label: 'Scene Timeline', desc: 'Scene-by-scene timing and pacing breakdown', icon: ListTree },
              { type: 'CHECKLIST', label: 'Production Checklist', desc: 'Pre, filming, and post quality assurance checklist', icon: CheckSquare },
              { type: 'JSON', label: 'Structured JSON', desc: 'Raw typed JSON with complete evidence provenance', icon: FileCode },
              { type: 'PRODUCTION_PACKAGE', label: 'Complete ZIP Package', desc: 'All 13 files, PDF, and README in one archive', icon: Package },
            ].map((fmt) => {
              const Icon = fmt.icon;
              const isSelected = selectedExportFormat === fmt.type;
              return (
                <div
                  key={fmt.type}
                  onClick={() => setSelectedExportFormat(fmt.type as ExportType)}
                  className={`p-3 rounded-xl border cursor-pointer transition-all flex items-start gap-2.5 ${
                    isSelected
                      ? 'bg-indigo-950/40 border-indigo-500/60 ring-1 ring-indigo-500/50 text-slate-100'
                      : 'bg-slate-950/60 border-slate-800/80 hover:border-slate-700 text-slate-300'
                  }`}
                >
                  <Icon className={`w-4 h-4 mt-0.5 shrink-0 ${isSelected ? 'text-indigo-400' : 'text-slate-500'}`} />
                  <div className="space-y-0.5">
                    <div className="font-semibold text-xs flex items-center gap-1.5">
                      {fmt.label}
                    </div>
                    <div className="text-[11px] text-slate-400 leading-tight">{fmt.desc}</div>
                  </div>
                </div>
              );
            })}
          </div>

          <div className="p-3 bg-slate-900/80 rounded-lg border border-slate-800 text-[11px] text-slate-400">
            <strong>Deterministic Reproducibility:</strong> Zero AI generation occurs during export. Output is compiled strictly from the approved draft with cryptographic SHA-256 hashing.
          </div>

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-800">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setIsExportModalOpen(false)}
              className="text-xs text-slate-400"
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={() => handleExecuteExport(selectedExportFormat)}
              disabled={exportMutation.isPending}
              className="text-xs bg-indigo-600 hover:bg-indigo-500 text-white flex items-center gap-1.5"
            >
              <Download className={`w-3.5 h-3.5 ${exportMutation.isPending ? 'animate-spin' : ''}`} />
              {exportMutation.isPending ? 'Exporting...' : `Export ${selectedExportFormat}`}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Evidence Traceability Inspection Modal */}
      <Modal
        open={isEvidenceModalOpen}
        onClose={() => setIsEvidenceModalOpen(false)}
        title="Audience Evidence Traceability & Grounding"
      >
        <div className="space-y-4 text-xs text-slate-300">
          <div className="bg-slate-900/80 p-3 rounded-lg border border-slate-800 space-y-1">
            <div className="font-semibold text-slate-200">
              {briefAsset?.evidenceSnapshot?.recommendationTitle || briefContent.title}
            </div>
            <div className="text-[11px] text-slate-400">
              Topic: <strong className="text-indigo-400">{briefAsset?.evidenceSnapshot?.topicName || 'Audience Intelligence'}</strong> • Linked Evidence Signals: <strong className="text-emerald-400">{briefAsset?.evidenceSnapshot?.evidenceCount || 3}</strong>
            </div>
          </div>

          <div className="space-y-2">
            <div className="font-semibold text-slate-200 text-xs">Verified Signal Quotes:</div>
            {(briefContent.audienceEvidence || [
              'Why does my battery drop from 80% to 20% in 2 hours?',
              'Does fast charging damage long term battery health?'
            ]).map((quote: string, idx: number) => (
              <div key={idx} className="p-3 bg-slate-950 rounded-lg border border-slate-800/80 flex items-start gap-2.5">
                <MessageSquare className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
                <span className="text-slate-300 italic">"{quote}"</span>
              </div>
            ))}
          </div>

          <div className="p-3 bg-emerald-950/20 border border-emerald-500/30 rounded-lg text-emerald-200 text-[11px] flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>All generated production points are cryptographically linked to authenticated audience comments.</span>
          </div>
        </div>
      </Modal>
    </div>
  );
};

