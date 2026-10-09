import React, { useState, useMemo } from 'react';
import type {
  AudienceInterest,
  AudienceQuestion,
  RelatedTopic,
  TopicContentIdea,
} from '../../types/memory';
import {
  Brain,
  Layers,
  HelpCircle,
  Lightbulb,
  ZoomIn,
  ZoomOut,
  RotateCcw,
  Sparkles,
  Info,
} from 'lucide-react';
import { Badge } from '../ui/Badge';

interface GraphVisualizerProps {
  activeInterests: AudienceInterest[];
  weakeningInterests: AudienceInterest[];
  questions: AudienceQuestion[];
  relatedTopics: RelatedTopic[];
  contentIdeas: TopicContentIdea[];
}

interface NodeDetail {
  id: string;
  type: 'audience' | 'topic' | 'question' | 'idea';
  title: string;
  subtitle?: string;
  confidence?: number;
  status?: string;
  evidenceCount?: number;
  lastSeen?: string;
  extra?: Record<string, any>;
}

export const KnowledgeGraphVisualizer: React.FC<GraphVisualizerProps> = ({
  activeInterests,
  weakeningInterests,
  questions,
  relatedTopics,
  contentIdeas,
}) => {
  const [zoom, setZoom] = useState(1);
  const [filter, setFilter] = useState<'ALL' | 'ACTIVE' | 'WEAKENING'>('ALL');
  const [selectedNode, setSelectedNode] = useState<NodeDetail | null>(null);

  const displayTopics = useMemo(() => {
    if (filter === 'ACTIVE') return activeInterests;
    if (filter === 'WEAKENING') return weakeningInterests;
    return [...activeInterests, ...weakeningInterests];
  }, [filter, activeInterests, weakeningInterests]);

  const centerX = 400;
  const centerY = 250;

  // Calculate layout coordinates
  const topicNodes = useMemo(() => {
    const total = Math.max(displayTopics.length, 1);
    const radius = 170;
    return displayTopics.map((topic, i) => {
      const angle = (i / total) * 2 * Math.PI - Math.PI / 2;
      const x = centerX + radius * Math.cos(angle);
      const y = centerY + radius * Math.sin(angle);
      const displayLabel = topic.label || topic.topicName || topic.topicId;
      return {
        ...topic,
        displayLabel,
        x,
        y,
      };
    });
  }, [displayTopics]);

  // Questions linked around perimeter
  const questionNodes = useMemo(() => {
    const limited = questions.slice(0, 6);
    const total = Math.max(limited.length, 1);
    const radius = 270;
    return limited.map((q, i) => {
      const angle = (i / total) * 2 * Math.PI - Math.PI / 3;
      const x = centerX + radius * Math.cos(angle);
      const y = centerY + radius * Math.sin(angle);
      return {
        ...q,
        x,
        y,
      };
    });
  }, [questions]);

  // Content Ideas
  const ideaNodes = useMemo(() => {
    const limited = contentIdeas.slice(0, 4);
    const total = Math.max(limited.length, 1);
    const radius = 280;
    return limited.map((idea, i) => {
      const angle = (i / total) * 2 * Math.PI + Math.PI / 6;
      const x = centerX + radius * Math.cos(angle);
      const y = centerY + radius * Math.sin(angle);
      return {
        ...idea,
        x,
        y,
      };
    });
  }, [contentIdeas]);

  return (
    <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
      {/* Controls Bar */}
      <div className="flex flex-wrap items-center justify-between p-4 border-b border-slate-100 bg-slate-50/70 gap-3">
        <div className="flex items-center gap-2">
          <Brain className="w-5 h-5 text-indigo-600" />
          <span className="text-sm font-semibold text-slate-800">
            Interactive Knowledge Graph Projection
          </span>
          <Badge variant="accent" size="xs">
            Neo4j 5.x
          </Badge>
        </div>

        <div className="flex items-center gap-3">
          {/* Filter toggle */}
          <div className="flex bg-white rounded-lg border border-slate-200 p-0.5 text-xs font-medium text-slate-600">
            <button
              onClick={() => setFilter('ALL')}
              className={`px-2.5 py-1 rounded-md transition-all ${
                filter === 'ALL'
                  ? 'bg-indigo-600 text-white shadow-xs'
                  : 'hover:text-slate-900'
              }`}
            >
              All Signals
            </button>
            <button
              onClick={() => setFilter('ACTIVE')}
              className={`px-2.5 py-1 rounded-md transition-all ${
                filter === 'ACTIVE'
                  ? 'bg-emerald-600 text-white shadow-xs'
                  : 'hover:text-slate-900'
              }`}
            >
              Active
            </button>
            <button
              onClick={() => setFilter('WEAKENING')}
              className={`px-2.5 py-1 rounded-md transition-all ${
                filter === 'WEAKENING'
                  ? 'bg-amber-600 text-white shadow-xs'
                  : 'hover:text-slate-900'
              }`}
            >
              Weakening
            </button>
          </div>

          {/* Zoom controls */}
          <div className="flex items-center gap-1 bg-white border border-slate-200 rounded-lg p-1">
            <button
              onClick={() => setZoom(z => Math.max(0.6, z - 0.15))}
              className="p-1 hover:bg-slate-100 rounded text-slate-600 transition-colors"
              title="Zoom Out"
            >
              <ZoomOut className="w-4 h-4" />
            </button>
            <span className="text-xs px-1 text-slate-500 font-mono w-10 text-center">
              {(zoom * 100).toFixed(0)}%
            </span>
            <button
              onClick={() => setZoom(z => Math.min(1.5, z + 0.15))}
              className="p-1 hover:bg-slate-100 rounded text-slate-600 transition-colors"
              title="Zoom In"
            >
              <ZoomIn className="w-4 h-4" />
            </button>
            <button
              onClick={() => setZoom(1)}
              className="p-1 hover:bg-slate-100 rounded text-slate-600 transition-colors"
              title="Reset Zoom"
            >
              <RotateCcw className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* SVG Canvas and Node Details Panel */}
      <div className="relative flex flex-col lg:flex-row bg-slate-950 min-h-[460px] overflow-hidden">
        {/* Canvas */}
        <div className="flex-1 relative overflow-auto flex items-center justify-center p-4">
          <svg
            viewBox="0 0 800 500"
            className="w-full max-w-[850px] transition-transform duration-200"
            style={{ transform: `scale(${zoom})` }}
          >
            {/* Background Grid */}
            <defs>
              <pattern id="graph-grid" width="30" height="30" patternUnits="userSpaceOnUse">
                <path d="M 30 0 L 0 0 0 30" fill="none" stroke="rgba(255,255,255,0.04)" strokeWidth="1" />
              </pattern>
              <radialGradient id="glow-indigo" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stopColor="#6366f1" stopOpacity="0.4" />
                <stop offset="100%" stopColor="#6366f1" stopOpacity="0" />
              </radialGradient>
            </defs>
            <rect width="800" height="500" fill="url(#graph-grid)" />

            {/* Glowing Center Ring */}
            <circle cx={centerX} cy={centerY} r="180" fill="url(#glow-indigo)" />

            {/* Links: Audience -> Topics */}
            {topicNodes.map(t => (
              <line
                key={`link-aud-${t.topicId}`}
                x1={centerX}
                y1={centerY}
                x2={t.x}
                y2={t.y}
                stroke={t.status === 'ACTIVE' ? '#10b981' : '#f59e0b'}
                strokeWidth={Math.max(1.5, t.confidence * 3)}
                strokeOpacity={0.4}
                strokeDasharray={t.status === 'WEAKENING' ? '4 4' : undefined}
              />
            ))}

            {/* Links: Topics -> Questions */}
            {questionNodes.map(q => {
              const matchedTopic = topicNodes.find(t => t.topicId === q.topicId) || topicNodes[0];
              if (!matchedTopic) return null;
              return (
                <line
                  key={`link-q-${q.questionHash}`}
                  x1={matchedTopic.x}
                  y1={matchedTopic.y}
                  x2={q.x}
                  y2={q.y}
                  stroke="#38bdf8"
                  strokeWidth="1.5"
                  strokeOpacity="0.35"
                />
              );
            })}

            {/* Links: Topics -> Ideas */}
            {ideaNodes.map(idea => {
              const matchedTopic = topicNodes.find(t => t.topicId === idea.topicId) || topicNodes[0];
              if (!matchedTopic) return null;
              return (
                <line
                  key={`link-idea-${idea.contentIdeaId || idea.title}`}
                  x1={matchedTopic.x}
                  y1={matchedTopic.y}
                  x2={idea.x}
                  y2={idea.y}
                  stroke="#a855f7"
                  strokeWidth="1.5"
                  strokeOpacity="0.35"
                />
              );
            })}

            {/* Center Audience Node */}
            <g
              transform={`translate(${centerX}, ${centerY})`}
              className="cursor-pointer"
              onClick={() =>
                setSelectedNode({
                  id: 'audience-root',
                  type: 'audience',
                  title: 'Audience Root Memory',
                  subtitle: 'Central Tenant Audience Entity',
                  extra: {
                    activeCount: activeInterests.length,
                    weakeningCount: weakeningInterests.length,
                    questionCount: questions.length,
                    relatedTopicsCount: relatedTopics.length,
                  },
                })
              }
            >
              <circle r="36" fill="#1e1b4b" stroke="#818cf8" strokeWidth="2.5" />
              <circle r="44" fill="none" stroke="#6366f1" strokeWidth="1" strokeDasharray="3 3" opacity="0.6" />
              <text textAnchor="middle" dy="-4" fill="#ffffff" fontSize="11" fontWeight="bold">
                Audience
              </text>
              <text textAnchor="middle" dy="12" fill="#a5b4fc" fontSize="9">
                Memory Hub
              </text>
            </g>

            {/* Topic Nodes */}
            {topicNodes.map(t => (
              <g
                key={`node-t-${t.topicId}`}
                transform={`translate(${t.x}, ${t.y})`}
                className="cursor-pointer group"
                onClick={() =>
                  setSelectedNode({
                    id: t.topicId,
                    type: 'topic',
                    title: t.displayLabel,
                    subtitle: `Status: ${t.status}`,
                    confidence: t.confidence,
                    status: t.status,
                    evidenceCount: t.evidenceCount,
                    lastSeen: t.lastSeenAt,
                    extra: {
                      trend: t.trend,
                      daysSinceLastSeen: t.daysSinceLastSeen,
                    },
                  })
                }
              >
                <circle
                  r="24"
                  fill={t.status === 'ACTIVE' ? '#064e3b' : '#78350f'}
                  stroke={t.status === 'ACTIVE' ? '#34d399' : '#fbbf24'}
                  strokeWidth="2"
                  className="transition-all group-hover:scale-110"
                />
                <text textAnchor="middle" dy="4" fill="#ffffff" fontSize="10" fontWeight="bold">
                  {t.displayLabel.slice(0, 10)}
                </text>
                {/* Confidence Badge */}
                <rect x="-18" y="28" width="36" height="14" rx="7" fill="#0f172a" stroke="#334155" />
                <text textAnchor="middle" x="0" y="38" fill="#e2e8f0" fontSize="8" fontWeight="bold">
                  {(t.confidence * 100).toFixed(0)}%
                </text>
              </g>
            ))}

            {/* Question Nodes */}
            {questionNodes.map(q => (
              <g
                key={`node-q-${q.questionHash}`}
                transform={`translate(${q.x}, ${q.y})`}
                className="cursor-pointer group"
                onClick={() =>
                  setSelectedNode({
                    id: q.questionHash,
                    type: 'question',
                    title: q.normalizedText,
                    subtitle: `Question Hash: ${q.questionHash.slice(0, 10)}...`,
                    confidence: q.confidence,
                    evidenceCount: q.evidenceCount,
                    lastSeen: q.lastSeenAt,
                    extra: {
                      topic: q.topicLabel || q.topicName,
                    },
                  })
                }
              >
                <circle
                  r="18"
                  fill="#0c4a6e"
                  stroke="#38bdf8"
                  strokeWidth="1.5"
                  className="transition-all group-hover:scale-110"
                />
                <text textAnchor="middle" dy="3" fill="#e0f2fe" fontSize="9" fontWeight="medium">
                  ?
                </text>
                <text textAnchor="middle" dy="28" fill="#94a3b8" fontSize="7.5">
                  {q.normalizedText.slice(0, 12)}...
                </text>
              </g>
            ))}

            {/* Content Idea Nodes */}
            {ideaNodes.map(idea => (
              <g
                key={`node-idea-${idea.contentIdeaId || idea.title}`}
                transform={`translate(${idea.x}, ${idea.y})`}
                className="cursor-pointer group"
                onClick={() =>
                  setSelectedNode({
                    id: idea.contentIdeaId || idea.title,
                    type: 'idea',
                    title: idea.title,
                    subtitle: `Angle: ${idea.angle} • Status: ${idea.status}`,
                    extra: {
                      createdAt: idea.createdAt,
                      scheduledDate: idea.scheduledDate,
                    },
                  })
                }
              >
                <circle
                  r="18"
                  fill="#581c87"
                  stroke="#c084fc"
                  strokeWidth="1.5"
                  className="transition-all group-hover:scale-110"
                />
                <text textAnchor="middle" dy="3" fill="#f3e8ff" fontSize="8" fontWeight="bold">
                  💡
                </text>
                <text textAnchor="middle" dy="28" fill="#cbd5e1" fontSize="7.5">
                  {idea.title.slice(0, 12)}...
                </text>
              </g>
            ))}
          </svg>
        </div>

        {/* Selected Node Details Drawer */}
        {selectedNode && (
          <div className="w-full lg:w-80 bg-slate-900 border-t lg:border-t-0 lg:border-l border-slate-800 p-5 text-white flex flex-col justify-between animate-fadeIn">
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-xs uppercase tracking-wider text-slate-400 font-bold flex items-center gap-1.5">
                  {selectedNode.type === 'topic' && <Layers className="w-4 h-4 text-emerald-400" />}
                  {selectedNode.type === 'question' && <HelpCircle className="w-4 h-4 text-sky-400" />}
                  {selectedNode.type === 'idea' && <Lightbulb className="w-4 h-4 text-purple-400" />}
                  {selectedNode.type === 'audience' && <Brain className="w-4 h-4 text-indigo-400" />}
                  {selectedNode.type.toUpperCase()} NODE
                </span>
                <button
                  onClick={() => setSelectedNode(null)}
                  className="text-slate-400 hover:text-white text-xs px-2 py-1 bg-slate-800 rounded"
                >
                  ✕ Close
                </button>
              </div>

              <div>
                <h4 className="text-sm font-bold text-white leading-snug">{selectedNode.title}</h4>
                {selectedNode.subtitle && (
                  <p className="text-xs text-slate-400 mt-1">{selectedNode.subtitle}</p>
                )}
              </div>

              <div className="space-y-2 text-xs border-t border-slate-800 pt-3">
                {selectedNode.confidence !== undefined && (
                  <div className="flex justify-between py-1">
                    <span className="text-slate-400">Memory Confidence:</span>
                    <span className="font-semibold text-emerald-400">
                      {(selectedNode.confidence * 100).toFixed(0)}%
                    </span>
                  </div>
                )}
                {selectedNode.status && (
                  <div className="flex justify-between py-1">
                    <span className="text-slate-400">Status:</span>
                    <span
                      className={`font-semibold ${
                        selectedNode.status === 'ACTIVE'
                          ? 'text-emerald-400'
                          : selectedNode.status === 'WEAKENING'
                          ? 'text-amber-400'
                          : 'text-slate-400'
                      }`}
                    >
                      {selectedNode.status}
                    </span>
                  </div>
                )}
                {selectedNode.evidenceCount !== undefined && (
                  <div className="flex justify-between py-1">
                    <span className="text-slate-400">Evidence Observations:</span>
                    <span className="font-mono text-slate-200">{selectedNode.evidenceCount}</span>
                  </div>
                )}
                {selectedNode.lastSeen && (
                  <div className="flex justify-between py-1">
                    <span className="text-slate-400">Last Seen:</span>
                    <span className="text-slate-300">
                      {new Date(selectedNode.lastSeen).toLocaleDateString()}
                    </span>
                  </div>
                )}
                {selectedNode.extra &&
                  Object.entries(selectedNode.extra).map(([k, v]) => (
                    <div key={k} className="flex justify-between py-1">
                      <span className="text-slate-400 capitalize">{k}:</span>
                      <span className="text-slate-200 font-mono">{String(v)}</span>
                    </div>
                  ))}
              </div>
            </div>

            <div className="pt-4 border-t border-slate-800 text-[11px] text-slate-500 flex items-center gap-1">
              <Info className="w-3.5 h-3.5" />
              <span>Authoritative source records reside in PostgreSQL.</span>
            </div>
          </div>
        )}
      </div>

      {/* Legend */}
      <div className="p-3 bg-slate-900 border-t border-slate-800 flex flex-wrap items-center justify-between text-xs text-slate-400 gap-4">
        <div className="flex items-center gap-5">
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded-full bg-emerald-500 inline-block" />
            <span>Active Topic (conf &ge; 0.60)</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded-full bg-amber-500 inline-block" />
            <span>Weakening Topic (&lt; 0.60)</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded-full bg-sky-400 inline-block" />
            <span>Recurring Question</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded-full bg-purple-500 inline-block" />
            <span>Content Idea</span>
          </div>
        </div>

        <div className="text-[11px] text-slate-400 flex items-center gap-1">
          <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
          <span>Click any node to view entity graph attributes</span>
        </div>
      </div>
    </div>
  );
};
