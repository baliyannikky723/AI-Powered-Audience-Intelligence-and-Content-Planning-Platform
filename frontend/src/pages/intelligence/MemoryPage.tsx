import React, { useState, useEffect, useCallback } from 'react';
import { audienceMemoryService } from '../../services/audienceMemoryService';
import type {
  AudienceInterest,
  AudienceQuestion,
  RelatedTopic,
  TopicContentIdea,
  AudienceMemorySummary,
  MemoryRebuildResult,
} from '../../types/memory';
import { KnowledgeGraphVisualizer } from '../../components/memory/KnowledgeGraphVisualizer';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Modal } from '../../components/ui/Modal';
import { ProgressBar } from '../../components/ui/ProgressBar';
import { EmptyState } from '../../components/ui/EmptyState';
import {
  Brain,
  Layers,
  HelpCircle,
  Lightbulb,
  RefreshCw,
  TrendingUp,
  TrendingDown,
  ShieldCheck,
  AlertCircle,
  ArrowRight,
  Sparkles,
  GitGraph,
} from 'lucide-react';

export const MemoryPage: React.FC = () => {
  const [summary, setSummary] = useState<AudienceMemorySummary | null>(null);
  const [activeInterests, setActiveInterests] = useState<AudienceInterest[]>([]);
  const [weakeningInterests, setWeakeningInterests] = useState<AudienceInterest[]>([]);
  const [questions, setQuestions] = useState<AudienceQuestion[]>([]);
  const [selectedTopicId, setSelectedTopicId] = useState<string | null>(null);
  const [relatedTopics, setRelatedTopics] = useState<RelatedTopic[]>([]);
  const [contentIdeas, setContentIdeas] = useState<TopicContentIdea[]>([]);

  const [isLoading, setIsLoading] = useState(true);
  const [isRebuilding, setIsRebuilding] = useState(false);
  const [rebuildModalOpen, setRebuildModalOpen] = useState(false);
  const [rebuildResult, setRebuildResult] = useState<MemoryRebuildResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const fetchMemoryData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const summaryData = await audienceMemoryService.getSummary();
      setSummary(summaryData);
      setActiveInterests(summaryData.activeInterests || []);
      setWeakeningInterests(summaryData.weakeningInterests || []);
      setQuestions(summaryData.recurringQuestions || []);

      if (summaryData.activeInterests && summaryData.activeInterests.length > 0) {
        const firstTopicId = summaryData.activeInterests[0].topicId;
        setSelectedTopicId(firstTopicId);
        try {
          const [rel, ideas] = await Promise.all([
            audienceMemoryService.getRelatedTopics(firstTopicId),
            audienceMemoryService.getTopicContentIdeas(firstTopicId),
          ]);
          setRelatedTopics(rel || []);
          setContentIdeas(ideas || []);
        } catch (e) {
          console.warn('Could not fetch topic details', e);
        }
      }
    } catch (err: any) {
      console.error('Failed to load audience memory', err);
      setError(err.response?.data?.message || 'Failed to load audience memory knowledge graph.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMemoryData();
  }, [fetchMemoryData]);

  const handleSelectTopic = async (topicId: string) => {
    setSelectedTopicId(topicId);
    try {
      const [rel, ideas] = await Promise.all([
        audienceMemoryService.getRelatedTopics(topicId),
        audienceMemoryService.getTopicContentIdeas(topicId),
      ]);
      setRelatedTopics(rel || []);
      setContentIdeas(ideas || []);
    } catch (e) {
      console.warn('Error fetching related topics for ' + topicId, e);
    }
  };

  const handleRebuild = async () => {
    setIsRebuilding(true);
    try {
      const result = await audienceMemoryService.rebuildKnowledgeGraph();
      setRebuildResult(result);
      await fetchMemoryData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Graph rebuild failed. Please try again.');
    } finally {
      setIsRebuilding(false);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6 animate-pulse p-6">
        <div className="h-20 bg-slate-100 rounded-2xl" />
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="h-28 bg-slate-100 rounded-xl" />
          ))}
        </div>
        <div className="h-96 bg-slate-900/10 rounded-2xl" />
      </div>
    );
  }

  return (
    <div className="space-y-8 p-4 sm:p-6 max-w-7xl mx-auto">
      {/* 1. Header & Rebuild Action */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 text-white p-6 sm:p-8 rounded-3xl shadow-xl border border-indigo-900/40">
        <div className="space-y-2">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-indigo-500/20 border border-indigo-500/30 rounded-xl">
              <Brain className="w-6 h-6 text-indigo-400" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
                Audience Memory & Knowledge Graph
              </h1>
              <p className="text-xs sm:text-sm text-indigo-200/80">
                Persistent, time-decayed audience knowledge graph projected to Neo4j 5.x from PostgreSQL.
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="secondary"
            size="sm"
            onClick={fetchMemoryData}
            className="border-slate-700 bg-slate-800/80 text-white hover:bg-slate-700"
          >
            <RefreshCw className="w-4 h-4 mr-1.5" />
            Refresh
          </Button>

          <Button
            variant="primary"
            size="sm"
            onClick={() => setRebuildModalOpen(true)}
            className="bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-600/30"
          >
            <GitGraph className="w-4 h-4 mr-1.5" />
            Rebuild Knowledge Graph
          </Button>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-700 flex items-center gap-2">
          <AlertCircle className="w-4 h-4 flex-shrink-0 text-rose-600" />
          <span>{error}</span>
        </div>
      )}

      {/* 2. Top Summary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-5 border-slate-200 hover:border-emerald-300 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Active Interests
            </span>
            <div className="p-2 rounded-lg bg-emerald-50 text-emerald-600">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-slate-900">{activeInterests.length}</span>
            <span className="text-xs text-emerald-600 font-medium">Confidence &ge; 60%</span>
          </div>
          <p className="text-[11px] text-slate-400 mt-1">High observation consistency & recency</p>
        </Card>

        <Card className="p-5 border-slate-200 hover:border-amber-300 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Weakening Signals
            </span>
            <div className="p-2 rounded-lg bg-amber-50 text-amber-600">
              <TrendingDown className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-slate-900">{weakeningInterests.length}</span>
            <span className="text-xs text-amber-600 font-medium">Half-life decaying</span>
          </div>
          <p className="text-[11px] text-slate-400 mt-1">Observed &gt; 45 days ago without reinforcement</p>
        </Card>

        <Card className="p-5 border-slate-200 hover:border-sky-300 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Recurring Questions
            </span>
            <div className="p-2 rounded-lg bg-sky-50 text-sky-600">
              <HelpCircle className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-slate-900">{questions.length}</span>
            <span className="text-xs text-sky-600 font-medium">Deduplicated</span>
          </div>
          <p className="text-[11px] text-slate-400 mt-1">SHA-256 normalized audience inquiries</p>
        </Card>

        <Card className="p-5 border-slate-200 hover:border-purple-300 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Content Ideas Linked
            </span>
            <div className="p-2 rounded-lg bg-purple-50 text-purple-600">
              <Lightbulb className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-slate-900">{summary?.contentIdeaCount || 0}</span>
            <span className="text-xs text-purple-600 font-medium">Evidence-Grounded</span>
          </div>
          <p className="text-[11px] text-slate-400 mt-1">Targeted directly to verified interests</p>
        </Card>
      </div>

      {/* 3. Interactive Knowledge Graph Visualization */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-indigo-600" />
            Knowledge Graph Visualization
          </h2>
          <span className="text-xs text-slate-500">
            Node-Link Projection: Audience &rarr; Topics &rarr; Questions &rarr; Ideas
          </span>
        </div>

        <KnowledgeGraphVisualizer
          activeInterests={activeInterests}
          weakeningInterests={weakeningInterests}
          questions={questions}
          relatedTopics={relatedTopics}
          contentIdeas={contentIdeas}
        />
      </div>

      {/* 4. Active & Weakening Audience Interests Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Active Interests Card */}
        <Card className="p-6 space-y-4 border-slate-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
              <h3 className="text-base font-bold text-slate-900">Active Audience Interests</h3>
            </div>
            <Badge variant="success" size="xs">
              {activeInterests.length} Topics
            </Badge>
          </div>
          <p className="text-xs text-slate-500 leading-relaxed">
            High-confidence topics calculated via canonical formula: (1 - e^(−n/200)) &times; consistency.
          </p>

          {activeInterests.length === 0 ? (
            <EmptyState
              title="No Active Interests Yet"
              body="Run clustering or ingest audience comments to build long-term memory."
            />
          ) : (
            <div className="space-y-3 max-h-[380px] overflow-y-auto pr-1">
              {activeInterests.map(interest => {
                const label = interest.label || interest.topicName || interest.topicId;
                return (
                  <div
                    key={interest.topicId}
                    onClick={() => handleSelectTopic(interest.topicId)}
                    className={`p-4 rounded-xl border transition-all cursor-pointer ${
                      selectedTopicId === interest.topicId
                        ? 'border-indigo-500 bg-indigo-50/50 shadow-xs'
                        : 'border-slate-200 bg-white hover:border-slate-300'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="space-y-1">
                        <h4 className="text-sm font-bold text-slate-900">{label}</h4>
                        <div className="flex items-center gap-3 text-xs text-slate-500">
                          <span>{interest.evidenceCount} evidence items</span>
                          <span>•</span>
                          <span>Seen {interest.lastSeenAt ? new Date(interest.lastSeenAt).toLocaleDateString() : 'Recently'}</span>
                        </div>
                      </div>
                      <Badge variant="success" size="xs">
                        {(interest.confidence * 100).toFixed(0)}% Conf
                      </Badge>
                    </div>

                    <div className="mt-3">
                      <ProgressBar
                        value={interest.confidence * 100}
                        color="success"
                        size="sm"
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </Card>

        {/* Weakening Interests Card */}
        <Card className="p-6 space-y-4 border-slate-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="w-2.5 h-2.5 rounded-full bg-amber-500" />
              <h3 className="text-base font-bold text-slate-900">Weakening Signals (Decaying)</h3>
            </div>
            <Badge variant="warning" size="xs">
              {weakeningInterests.length} Topics
            </Badge>
          </div>
          <p className="text-xs text-slate-500 leading-relaxed">
            Exponential half-life decay (t<sub>1/2</sub> = 45 days) applied to unreinforced topics.
          </p>

          {weakeningInterests.length === 0 ? (
            <div className="p-8 rounded-xl bg-slate-50 border border-slate-100 text-center text-xs text-slate-400">
              No decaying topics currently detected. All active signals remain fresh.
            </div>
          ) : (
            <div className="space-y-3 max-h-[380px] overflow-y-auto pr-1">
              {weakeningInterests.map(interest => {
                const label = interest.label || interest.topicName || interest.topicId;
                return (
                  <div
                    key={interest.topicId}
                    onClick={() => handleSelectTopic(interest.topicId)}
                    className={`p-4 rounded-xl border transition-all cursor-pointer ${
                      selectedTopicId === interest.topicId
                        ? 'border-amber-500 bg-amber-50/50 shadow-xs'
                        : 'border-slate-200 bg-white hover:border-slate-300'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="space-y-1">
                        <h4 className="text-sm font-bold text-slate-900">{label}</h4>
                        <div className="flex items-center gap-3 text-xs text-slate-500">
                          <span>{interest.evidenceCount} evidence observations</span>
                          <span>•</span>
                          <span>Last seen {interest.lastSeenAt ? new Date(interest.lastSeenAt).toLocaleDateString() : 'N/A'}</span>
                        </div>
                      </div>
                      <Badge variant="warning" size="xs">
                        {(interest.confidence * 100).toFixed(0)}% Conf
                      </Badge>
                    </div>

                    <div className="mt-3">
                      <ProgressBar
                        value={interest.confidence * 100}
                        color="warning"
                        size="sm"
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </Card>
      </div>

      {/* 5. Recurring Questions & Topic Associations Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recurring Questions */}
        <Card className="p-6 space-y-4 border-slate-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <HelpCircle className="w-5 h-5 text-sky-600" />
              <h3 className="text-base font-bold text-slate-900">Recurring Audience Questions</h3>
            </div>
            <Badge variant="accent" size="xs">
              {questions.length} Questions
            </Badge>
          </div>
          <p className="text-xs text-slate-500">
            Consolidated recurring questions automatically redacted for privacy and mapped to topics.
          </p>

          <div className="space-y-3 max-h-[340px] overflow-y-auto pr-1">
            {questions.length === 0 ? (
              <div className="p-6 text-center text-xs text-slate-400 bg-slate-50 rounded-xl">
                No recurring questions recorded yet.
              </div>
            ) : (
              questions.map(q => (
                <div key={q.questionHash} className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl space-y-2">
                  <div className="flex items-start justify-between gap-2">
                    <p className="text-xs font-semibold text-slate-800 leading-snug">
                      "{q.normalizedText}"
                    </p>
                    <Badge variant="accent" size="xs">
                      {q.evidenceCount}x
                    </Badge>
                  </div>
                  <div className="flex items-center justify-between text-[11px] text-slate-500 pt-1 border-t border-slate-200/60">
                    <span>Hash: {q.questionHash.slice(0, 12)}...</span>
                    {(q.topicLabel || q.topicName) && (
                      <span className="text-indigo-600 font-medium">Topic: {q.topicLabel || q.topicName}</span>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        </Card>

        {/* Topic Context: Related Topics & Content Ideas */}
        <Card className="p-6 space-y-4 border-slate-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Layers className="w-5 h-5 text-indigo-600" />
              <h3 className="text-base font-bold text-slate-900">Topic Graph Context</h3>
            </div>
            <span className="text-xs text-slate-400 font-mono">
              Selected Topic: {activeInterests.find(t => t.topicId === selectedTopicId)?.label || 'None'}
            </span>
          </div>

          <div className="space-y-4">
            <div>
              <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                Related Topics (Co-occurrences)
              </h4>
              {relatedTopics.length === 0 ? (
                <p className="text-xs text-slate-400 italic">No topic co-occurrences found.</p>
              ) : (
                <div className="flex flex-wrap gap-2">
                  {relatedTopics.map(rel => (
                    <div
                      key={rel.targetTopicId}
                      className="px-3 py-1.5 bg-indigo-50 border border-indigo-100 rounded-lg text-xs text-indigo-900 flex items-center gap-1.5"
                    >
                      <ArrowRight className="w-3 h-3 text-indigo-500" />
                      <span className="font-medium">{rel.targetTopicLabel || rel.targetTopicName || rel.targetTopicId}</span>
                      {rel.similarity !== undefined && (
                        <span className="text-[10px] text-indigo-500">({(rel.similarity * 100).toFixed(0)}%)</span>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="pt-2 border-t border-slate-100">
              <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                Linked Content Ideas & Drafts
              </h4>
              {contentIdeas.length === 0 ? (
                <p className="text-xs text-slate-400 italic">No content recommendations generated for this topic yet.</p>
              ) : (
                <div className="space-y-2 max-h-[160px] overflow-y-auto pr-1">
                  {contentIdeas.map(idea => (
                    <div
                      key={idea.contentIdeaId || idea.id || idea.title}
                      className="p-2.5 bg-purple-50/60 border border-purple-100 rounded-lg text-xs flex items-center justify-between"
                    >
                      <span className="font-medium text-purple-950 truncate max-w-[240px]">
                        {idea.title}
                      </span>
                      <Badge variant="accent" size="xs">
                        {idea.status}
                      </Badge>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </Card>
      </div>

      {/* 6. Knowledge Graph Rebuild Modal */}
      <Modal
        open={rebuildModalOpen}
        onClose={() => setRebuildModalOpen(false)}
        title="Rebuild User Knowledge Graph"
      >
        <div className="space-y-4">
          <p className="text-xs text-slate-600 leading-relaxed">
            This operation will clear your user-scoped projection in Neo4j and completely reconstruct all nodes (Audience, Topics, Questions, Content Ideas) and relationships from PostgreSQL authoritative data.
          </p>

          <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl space-y-2 text-xs">
            <div className="flex items-center gap-2 text-slate-700 font-semibold">
              <ShieldCheck className="w-4 h-4 text-emerald-600" />
              Tenant Isolation & Idempotency Guarantee
            </div>
            <p className="text-slate-500 text-[11px]">
              Cypher execution uses strictly parameterized queries with user-scoped constraints. Only your data projection is modified.
            </p>
          </div>

          {rebuildResult && (
            <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-800 space-y-1">
              <div className="font-bold">Rebuild Successful!</div>
              <div>Duration: {rebuildResult.durationMs}ms</div>
              <div>Topics projected: {rebuildResult.topicsProjected ?? rebuildResult.topicCount ?? 0}</div>
              <div>Questions projected: {rebuildResult.questionsProjected ?? rebuildResult.questionCount ?? 0}</div>
              <div>Relationships created: {rebuildResult.relationshipsCreated ?? rebuildResult.relationshipCount ?? 0}</div>
            </div>
          )}

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setRebuildModalOpen(false)}
              disabled={isRebuilding}
            >
              Close
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={handleRebuild}
              disabled={isRebuilding}
              className="bg-indigo-600 hover:bg-indigo-700"
            >
              {isRebuilding ? (
                <>
                  <RefreshCw className="w-3.5 h-3.5 mr-1.5 animate-spin" />
                  Rebuilding Graph...
                </>
              ) : (
                'Start Full Rebuild'
              )}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
