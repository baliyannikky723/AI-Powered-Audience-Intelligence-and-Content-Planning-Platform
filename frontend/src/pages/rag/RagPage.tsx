import React, { useState } from 'react';
import { ragService } from '../../services/ragService';
import type {
  RagMode,
  RagQueryResponse,
  RagEvidenceItem,
  RagRelevance,
  RagCorrectness,
} from '../../types/rag';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import {
  Brain,
  Sparkles,
  Search,
  Layers,
  HelpCircle,
  MessageSquare,
  History,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  FileCheck,
  Database,
  ExternalLink,
} from 'lucide-react';

const PRESET_QUERIES = [
  'What are the strongest topics and recurring inquiries in my audience memory?',
  'What specific technical pain points do users complain about in recent comments?',
  'Which audience interests are strengthening vs weakening over time?',
  'Give me evidence-grounded content ideas addressing top questions.',
];

export const RagPage: React.FC = () => {
  const [queryText, setQueryText] = useState('');
  const [mode, setMode] = useState<RagMode>('FULL_EVIDENCE_GROUNDED');
  const [isLoading, setIsLoading] = useState(false);
  const [response, setResponse] = useState<RagQueryResponse | null>(null);
  const [selectedEvidence, setSelectedEvidence] = useState<RagEvidenceItem | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Annotation Modal State
  const [annotationModalOpen, setAnnotationModalOpen] = useState(false);
  const [annotatingItem, setAnnotatingItem] = useState<RagEvidenceItem | null>(null);
  const [relevance, setRelevance] = useState<RagRelevance>('RELEVANT');
  const [correctness, setCorrectness] = useState<RagCorrectness>('SUPPORTED');
  const [notes, setNotes] = useState('');
  const [annotationSuccess, setAnnotationSuccess] = useState(false);

  const handleSearch = async (textToSearch?: string) => {
    const q = textToSearch !== undefined ? textToSearch : queryText;
    if (!q.trim()) return;

    setIsLoading(true);
    setError(null);
    try {
      const res = await ragService.executeQuery({
        query: q,
        generationMode: mode,
        maxEvidence: 12,
      });
      setResponse(res);
      if (res.evidence && res.evidence.length > 0) {
        setSelectedEvidence(res.evidence[0]);
      }
    } catch (err: any) {
      console.error('RAG query failed', err);
      setError(err.response?.data?.message || 'RAG query failed. Please check network or service availability.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleOpenAnnotation = (item: RagEvidenceItem) => {
    setAnnotatingItem(item);
    setAnnotationSuccess(false);
    setNotes('');
    setAnnotationModalOpen(true);
  };

  const handleSaveAnnotation = async () => {
    if (!annotatingItem) return;
    try {
      await ragService.createAnnotation({
        queryId: `query-${Date.now()}`,
        evidenceId: annotatingItem.evidenceId,
        relevance,
        correctness,
        notes,
      });
      setAnnotationSuccess(true);
      setTimeout(() => {
        setAnnotationModalOpen(false);
      }, 1000);
    } catch (err) {
      console.error('Failed to save annotation', err);
    }
  };

  return (
    <div className="space-y-8 p-4 sm:p-6 max-w-7xl mx-auto">
      {/* 1. Header Banner */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 text-white p-6 sm:p-8 rounded-3xl shadow-xl border border-indigo-900/40">
        <div className="space-y-2">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-indigo-500/20 border border-indigo-500/30 rounded-xl">
              <Sparkles className="w-6 h-6 text-indigo-400" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
                Audience Intelligence RAG & Evidence Grounding
              </h1>
              <p className="text-xs sm:text-sm text-indigo-200/80">
                Multi-source evidence fusion: PostgreSQL Vector + Neo4j Graph Memory + Structured Audience Signals.
              </p>
            </div>
          </div>
        </div>

        {/* Mode Selector */}
        <div className="flex flex-wrap items-center bg-slate-800/90 p-1.5 rounded-xl border border-slate-700 text-xs font-medium">
          <button
            onClick={() => setMode('FULL_EVIDENCE_GROUNDED')}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              mode === 'FULL_EVIDENCE_GROUNDED'
                ? 'bg-indigo-600 text-white shadow-xs'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Full Evidence-Grounded
          </button>
          <button
            onClick={() => setMode('GRAPH_AUGMENTED')}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              mode === 'GRAPH_AUGMENTED'
                ? 'bg-indigo-600 text-white shadow-xs'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Graph Augmented
          </button>
          <button
            onClick={() => setMode('VECTOR_ONLY')}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              mode === 'VECTOR_ONLY'
                ? 'bg-indigo-600 text-white shadow-xs'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Vector Only
          </button>
          <button
            onClick={() => setMode('BASELINE')}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              mode === 'BASELINE'
                ? 'bg-slate-700 text-white shadow-xs'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Baseline
          </button>
        </div>
      </div>

      {/* 2. Query Input & Preset Pills */}
      <Card className="p-6 space-y-4 border-slate-200 shadow-sm">
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          <div className="relative flex-1">
            <Search className="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={queryText}
              onChange={e => setQueryText(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleSearch()}
              placeholder="Ask a question grounded in your audience memory and comments..."
              className="w-full pl-11 pr-4 py-2.5 rounded-xl border border-slate-200 bg-slate-50/50 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all"
            />
          </div>
          <Button
            variant="primary"
            onClick={() => handleSearch()}
            disabled={isLoading || !queryText.trim()}
            className="bg-indigo-600 hover:bg-indigo-700 text-white px-6"
          >
            {isLoading ? 'Retrieving & Generating...' : 'Ask Audience RAG'}
          </Button>
        </div>

        {/* Preset Chips */}
        <div className="flex flex-wrap items-center gap-2 pt-1">
          <span className="text-xs text-slate-500 font-medium flex items-center gap-1">
            <Search className="w-3.5 h-3.5" /> Presets:
          </span>
          {PRESET_QUERIES.map((preset, idx) => (
            <button
              key={idx}
              onClick={() => {
                setQueryText(preset);
                handleSearch(preset);
              }}
              className="text-xs px-3 py-1 rounded-full bg-slate-100 hover:bg-indigo-50 hover:text-indigo-600 text-slate-600 transition-colors border border-slate-200/80"
            >
              {preset}
            </button>
          ))}
        </div>
      </Card>

      {error && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-700 flex items-center gap-2">
          <AlertCircle className="w-4 h-4 flex-shrink-0 text-rose-600" />
          <span>{error}</span>
        </div>
      )}

      {/* 3. Results Section */}
      {response && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start animate-fadeIn">
          {/* Answer & Validation Column (7 cols) */}
          <div className="lg:col-span-7 space-y-6">
            <Card className="p-6 space-y-5 border-slate-200 shadow-sm">
              <div className="flex items-center justify-between border-b border-slate-100 pb-4">
                <div className="flex items-center gap-2">
                  <Brain className="w-5 h-5 text-indigo-600" />
                  <h3 className="text-base font-bold text-slate-900">Evidence-Grounded Synthesis</h3>
                </div>
                <div className="flex items-center gap-2 text-xs text-slate-500">
                  <Badge variant="accent" size="xs">
                    {response.retrievalMetadata.retrievalVersion}
                  </Badge>
                  <span>{response.generationLatencyMs}ms</span>
                </div>
              </div>

              {/* Formatted Answer */}
              <div className="prose prose-sm text-slate-800 leading-relaxed max-w-none whitespace-pre-line text-sm">
                {response.answer}
              </div>

              {/* Citations List Bar */}
              {response.citations && response.citations.length > 0 && (
                <div className="pt-4 border-t border-slate-100 space-y-2">
                  <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
                    <FileCheck className="w-4 h-4 text-emerald-600" />
                    Verified Evidence Citations:
                  </h4>
                  <div className="flex flex-wrap gap-2">
                    {response.citations.map(c => (
                      <button
                        key={c.citationId}
                        onClick={() => {
                          const matched = response.evidence.find(e => e.citationId === c.citationId);
                          if (matched) setSelectedEvidence(matched);
                        }}
                        className="px-3 py-1.5 rounded-lg bg-indigo-50 hover:bg-indigo-100 border border-indigo-200 text-xs text-indigo-900 font-semibold flex items-center gap-1.5 transition-all"
                      >
                        <span className="font-mono text-indigo-600">{c.citationId}</span>
                        <span className="font-normal text-slate-600 truncate max-w-[180px]">
                          {c.snippet}
                        </span>
                        <ExternalLink className="w-3 h-3 text-indigo-500 ml-1" />
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </Card>

            {/* Validation & Safety Card */}
            <Card className="p-6 space-y-4 border-slate-200 shadow-sm bg-slate-50/50">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-emerald-600" />
                  <h4 className="text-sm font-bold text-slate-900">Citation & Safety Verification</h4>
                </div>
                <Badge
                  variant={response.validation.valid ? 'success' : 'danger'}
                  size="xs"
                >
                  {response.validation.valid ? 'PASS (100% Grounded)' : 'VALIDATION ALERT'}
                </Badge>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                {response.validation.checks.map(chk => (
                  <div
                    key={chk.checkName}
                    className={`p-3 rounded-xl border ${
                      chk.passed
                        ? 'bg-white border-emerald-200 text-slate-700'
                        : 'bg-rose-50 border-rose-200 text-rose-800'
                    }`}
                  >
                    <div className="flex items-center gap-2 font-bold mb-1">
                      {chk.passed ? (
                        <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                      ) : (
                        <AlertCircle className="w-4 h-4 text-rose-600" />
                      )}
                      <span>{chk.checkName}</span>
                    </div>
                    <p className="text-[11px] text-slate-500 leading-snug">{chk.reason}</p>
                  </div>
                ))}
              </div>
            </Card>
          </div>

          {/* Evidence Drawer Column (5 cols) */}
          <div className="lg:col-span-5 space-y-6">
            <Card className="p-6 space-y-4 border-slate-200 shadow-sm sticky top-6">
              <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                <div className="flex items-center gap-2">
                  <Database className="w-5 h-5 text-indigo-600" />
                  <h3 className="text-base font-bold text-slate-900">Fused Evidence Context</h3>
                </div>
                <Badge variant="accent" size="xs">
                  {response.evidence.length} Items Fused
                </Badge>
              </div>

              {/* Selected Evidence Inspector */}
              {selectedEvidence ? (
                <div className="p-4 rounded-xl bg-indigo-50/40 border border-indigo-100 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-xs font-bold text-indigo-700 px-2 py-0.5 bg-indigo-100 rounded">
                      {selectedEvidence.citationId}
                    </span>
                    <Badge variant="accent" size="xs">
                      {selectedEvidence.sourceType}
                    </Badge>
                  </div>

                  <p className="text-xs text-slate-800 leading-relaxed font-medium">
                    "{selectedEvidence.text}"
                  </p>

                  <div className="grid grid-cols-2 gap-2 text-[11px] pt-2 border-t border-indigo-100 text-slate-600">
                    <div>
                      <span className="text-slate-400">Fused Score: </span>
                      <span className="font-bold text-indigo-600">
                        {(selectedEvidence.evidenceScore * 100).toFixed(0)}%
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400">Similarity: </span>
                      <span className="font-medium">
                        {(selectedEvidence.similarity * 100).toFixed(0)}%
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400">Recency: </span>
                      <span className="font-medium">
                        {(selectedEvidence.recency * 100).toFixed(0)}%
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400">Quality: </span>
                      <span className="font-medium">
                        {(selectedEvidence.quality * 100).toFixed(0)}%
                      </span>
                    </div>
                  </div>

                  <div className="pt-2 flex justify-end">
                    <Button
                      variant="secondary"
                      size="xs"
                      onClick={() => handleOpenAnnotation(selectedEvidence)}
                      className="text-indigo-700 bg-white hover:bg-indigo-50 border border-indigo-200 text-xs"
                    >
                      Annotate Relevance for Research
                    </Button>
                  </div>
                </div>
              ) : (
                <div className="p-6 text-center text-xs text-slate-400">
                  Select a citation above to inspect full evidence metrics.
                </div>
              )}

              {/* All Evidence List */}
              <div className="space-y-2 max-h-[360px] overflow-y-auto pr-1">
                {response.evidence.map(item => (
                  <div
                    key={item.evidenceId}
                    onClick={() => setSelectedEvidence(item)}
                    className={`p-3 rounded-xl border transition-all cursor-pointer ${
                      selectedEvidence?.evidenceId === item.evidenceId
                        ? 'border-indigo-500 bg-white shadow-xs'
                        : 'border-slate-200 bg-slate-50/50 hover:bg-white'
                    }`}
                  >
                    <div className="flex items-center justify-between text-xs mb-1">
                      <span className="font-mono font-bold text-indigo-600">{item.citationId}</span>
                      <div className="flex items-center gap-1.5 text-slate-500 text-[11px]">
                        {item.sourceType === 'COMMENT' && <MessageSquare className="w-3.5 h-3.5 text-sky-500" />}
                        {item.sourceType === 'TOPIC' && <Layers className="w-3.5 h-3.5 text-emerald-500" />}
                        {item.sourceType === 'QUESTION' && <HelpCircle className="w-3.5 h-3.5 text-amber-500" />}
                        {item.sourceType === 'MEMORY' && <Brain className="w-3.5 h-3.5 text-purple-500" />}
                        {item.sourceType === 'CONTENT_HISTORY' && <History className="w-3.5 h-3.5 text-slate-500" />}
                        <span>{item.sourceType}</span>
                      </div>
                    </div>
                    <p className="text-xs text-slate-700 line-clamp-2">{item.text}</p>
                  </div>
                ))}
              </div>
            </Card>
          </div>
        </div>
      )}

      {/* 4. Human Relevance Annotation Modal */}
      <Modal
        open={annotationModalOpen}
        onClose={() => setAnnotationModalOpen(false)}
        title="Human Relevance Annotation (Paper 2)"
      >
        <div className="space-y-4">
          <p className="text-xs text-slate-600 leading-relaxed">
            Record ground-truth human annotations for retrieval precision/recall evaluation.
          </p>

          {annotatingItem && (
            <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs space-y-1">
              <span className="font-mono font-bold text-indigo-600">{annotatingItem.citationId}</span>
              <p className="text-slate-700">{annotatingItem.text}</p>
            </div>
          )}

          <div className="space-y-3 text-xs">
            <div>
              <label className="font-bold text-slate-700 block mb-1">Relevance Rating:</label>
              <div className="flex gap-2">
                {(['RELEVANT', 'PARTIALLY_RELEVANT', 'IRRELEVANT'] as RagRelevance[]).map(r => (
                  <button
                    key={r}
                    onClick={() => setRelevance(r)}
                    className={`px-3 py-1.5 rounded-lg border text-xs font-semibold ${
                      relevance === r
                        ? 'bg-indigo-600 text-white border-indigo-600'
                        : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    {r}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="font-bold text-slate-700 block mb-1">Evidence Support Correctness:</label>
              <div className="flex gap-2">
                {(['SUPPORTED', 'PARTIALLY_SUPPORTED', 'UNSUPPORTED'] as RagCorrectness[]).map(c => (
                  <button
                    key={c}
                    onClick={() => setCorrectness(c)}
                    className={`px-3 py-1.5 rounded-lg border text-xs font-semibold ${
                      correctness === c
                        ? 'bg-indigo-600 text-white border-indigo-600'
                        : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    {c}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="font-bold text-slate-700 block mb-1">Researcher Notes (Optional):</label>
              <input
                type="text"
                value={notes}
                onChange={e => setNotes(e.target.value)}
                placeholder="Specific context notes..."
                className="w-full px-3 py-2 border border-slate-200 rounded-lg text-xs"
              />
            </div>
          </div>

          {annotationSuccess && (
            <div className="p-3 bg-emerald-50 text-emerald-800 rounded-xl text-xs flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              <span>Annotation successfully recorded in database!</span>
            </div>
          )}

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setAnnotationModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={handleSaveAnnotation}
              className="bg-indigo-600 hover:bg-indigo-700 text-white"
            >
              Save Annotation
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
