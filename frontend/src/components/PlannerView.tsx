import React, { useState } from 'react';
import { mockAiSuggestions, type AIContentSuggestion } from '../data/mockData';
import { getPlatformIcon }  from '../lib/platform';
import { Button }           from './ui/Button';
import { Badge }            from './ui/Badge';
import { Modal }            from './ui/Modal';
import {
  Plus, Sparkles, Trash2, ArrowLeft, ArrowRight,
  Calendar, Clock, CheckCircle, Compass, ChevronRight,
} from 'lucide-react';

interface KanbanCard {
  id:            string;
  title:         string;
  platform:      string;
  description:   string;
  status:        'ideas' | 'scripting' | 'production' | 'scheduled';
  audienceImpact?: number;
  outline:       string[];
}

const COLUMNS: { id: KanbanCard['status']; name: string; icon: React.ReactNode; accent: string }[] = [
  { id: 'ideas',      name: 'Ideas & queue', icon: <Compass size={13} />, accent: 'text-ink-3' },
  { id: 'scripting',  name: 'Scripting',     icon: <Clock size={13} className="text-warning" />, accent: 'text-warning' },
  { id: 'production', name: 'In production', icon: <Calendar size={13} className="text-accent" />, accent: 'text-accent' },
  { id: 'scheduled',  name: 'Scheduled',     icon: <CheckCircle size={13} className="text-success" />, accent: 'text-success' },
];

export const PlannerView: React.FC = () => {
  const [recommendations, setRecommendations] = useState<AIContentSuggestion[]>(mockAiSuggestions);
  const [cards, setCards] = useState<KanbanCard[]>([
    {
      id: 'card-1', title: 'Building a Tailwind UI Dashboard in Next.js',
      platform: 'youtube', status: 'production', audienceImpact: 45,
      description: 'A detailed walkthrough after 45 requests for the styling configs.',
      outline: ['Setup Next.js & Tailwind CSS', 'Design glassmorphism overlays', 'Implement responsive sidebar'],
    },
    {
      id: 'card-2', title: 'LLM Tokenizers in 60s',
      platform: 'instagram', status: 'ideas', audienceImpact: 120,
      description: 'Quick visual explainer on BPE vs WordPiece tokenizer methods.',
      outline: ['What is a token?', 'Visual splitter graphics', 'Byte Pair Encoding explanation'],
    },
  ]);
  const [showAddForm, setShowAddForm] = useState(false);
  const [newTitle,   setNewTitle]   = useState('');
  const [newDesc,    setNewDesc]    = useState('');
  const [newPlatform,setNewPlatform]= useState('youtube');
  const [newOutline, setNewOutline] = useState('');

  const handleAddSuggestion = (s: AIContentSuggestion) => {
    setCards(prev => [...prev, {
      id: `card-${Date.now()}`, title: s.title, platform: s.platform,
      description: s.sourceIssue, status: 'ideas',
      audienceImpact: s.audienceCount, outline: s.outline,
    }]);
    setRecommendations(prev => prev.filter(r => r.id !== s.id));
  };

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) return;
    const outline = newOutline.split('\n').map(l => l.trim()).filter(Boolean);
    setCards(prev => [...prev, {
      id: `card-${Date.now()}`, title: newTitle, platform: newPlatform,
      description: newDesc || 'Manually planned topic.', status: 'ideas',
      outline: outline.length > 0 ? outline : ['Introduction', 'Core details', 'Summary'],
    }]);
    setShowAddForm(false);
    setNewTitle(''); setNewDesc(''); setNewPlatform('youtube'); setNewOutline('');
  };

  const handleMove = (cardId: string, dir: 'forward' | 'backward') => {
    const STATUSES: KanbanCard['status'][] = ['ideas', 'scripting', 'production', 'scheduled'];
    setCards(prev => prev.map(c => {
      if (c.id !== cardId) return c;
      const i = STATUSES.indexOf(c.status);
      const next = dir === 'forward' ? Math.min(i + 1, 3) : Math.max(i - 1, 0);
      return { ...c, status: STATUSES[next] };
    }));
  };

  const handleDelete = (cardId: string) => setCards(prev => prev.filter(c => c.id !== cardId));

  return (
    <div className="space-y-6 animate-slide-up">

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-[22px] font-semibold text-ink">Content planner</h2>
          <p className="text-sm text-ink-3 mt-0.5">Translate audience issues into your scripting schedule.</p>
        </div>
        <Button variant="primary" size="sm" icon={<Plus size={14} />} onClick={() => setShowAddForm(true)}>
          Plan new topic
        </Button>
      </div>

      {/* Main grid */}
      <div className="grid grid-cols-1 xl:grid-cols-4 gap-4 items-start">

        {/* AI suggestions sidebar */}
        <div className="panel p-4 space-y-4">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-accent uppercase tracking-wide">
            <Sparkles size={13} /> AI suggestions
          </div>
          <p className="text-xs text-ink-3 leading-relaxed">
            Topics proposed by semantic algorithms grouping common user questions and bugs.
          </p>
          <div className="space-y-3 max-h-[460px] overflow-y-auto pr-1">
            {recommendations.length > 0 ? recommendations.map(s => (
              <div key={s.id} className="panel-flat p-3 space-y-2.5 hover:border-border-strong transition-colors">
                <div className="flex items-center justify-between">
                  <span className="flex-shrink-0">{getPlatformIcon(s.platform, 14)}</span>
                  <Badge variant="accent" size="xs">+{s.audienceCount} reach</Badge>
                </div>
                <div>
                  <h4 className="text-sm font-semibold text-ink leading-snug">{s.title}</h4>
                  <p className="text-xs text-ink-3 mt-0.5 italic leading-normal">"{s.sourceIssue}"</p>
                </div>
                <ul className="text-xs text-ink-3 list-disc pl-4 space-y-0.5">
                  {s.outline.slice(0, 3).map((o, i) => <li key={i} className="truncate">{o}</li>)}
                </ul>
                <Button
                  variant="secondary"
                  size="xs"
                  className="w-full"
                  iconRight={<ChevronRight size={11} />}
                  onClick={() => handleAddSuggestion(s)}
                >
                  Add to planner
                </Button>
              </div>
            )) : (
              <p className="text-xs text-ink-3 text-center py-4 border border-dashed border-border rounded-lg">
                No new recommendations right now.
              </p>
            )}
          </div>
        </div>

        {/* Kanban board */}
        <div className="xl:col-span-3 grid grid-cols-1 md:grid-cols-4 gap-3 items-start">
          {COLUMNS.map(col => {
            const colCards = cards.filter(c => c.status === col.id);
            return (
              <div key={col.id} className="panel p-3 flex flex-col max-h-[600px] overflow-hidden">
                {/* Column header */}
                <div className="flex items-center justify-between pb-2.5 border-b border-border mb-3">
                  <div className="flex items-center gap-1.5">
                    <span className={col.accent}>{col.icon}</span>
                    <h3 className="text-xs font-semibold text-ink uppercase tracking-wide">{col.name}</h3>
                  </div>
                  <span className="text-[10px] font-semibold bg-surface-2 border border-border text-ink-3 px-1.5 py-0.5 rounded-full">
                    {colCards.length}
                  </span>
                </div>

                {/* Cards */}
                <div className="flex-1 overflow-y-auto space-y-2.5 min-h-[60px]">
                  {colCards.length > 0 ? colCards.map(card => (
                    <div key={card.id} className="panel-flat p-3 space-y-2.5 hover:border-border-strong transition-colors group">
                      {/* Platform + delete */}
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-1.5">
                          {getPlatformIcon(card.platform, 12)}
                          <span className="text-[10px] text-ink-3 font-medium capitalize">{card.platform}</span>
                        </div>
                        <button
                          onClick={() => handleDelete(card.id)}
                          aria-label="Delete card"
                          className="opacity-0 group-hover:opacity-100 h-5 w-5 rounded flex items-center justify-center text-ink-3 hover:text-danger hover:bg-danger-soft transition-all"
                        >
                          <Trash2 size={11} />
                        </button>
                      </div>

                      {/* Title + description */}
                      <div>
                        <h4 className="text-sm font-semibold text-ink leading-snug">{card.title}</h4>
                        <p className="text-xs text-ink-3 mt-0.5 leading-normal">{card.description}</p>
                      </div>

                      {/* Outline */}
                      {card.outline.length > 0 && (
                        <div className="bg-surface-2 rounded-md p-2 text-[10px] text-ink-3">
                          <span className="font-semibold text-ink-3 uppercase tracking-wide block mb-1">Outline</span>
                          <ul className="list-disc pl-3.5 space-y-0.5">
                            {card.outline.map((o, i) => <li key={i} className="truncate">{o}</li>)}
                          </ul>
                        </div>
                      )}

                      {/* Move controls */}
                      <div className="flex items-center justify-between pt-1.5 border-t border-border">
                        <button
                          disabled={card.status === 'ideas'}
                          onClick={() => handleMove(card.id, 'backward')}
                          aria-label="Move back"
                          className="h-6 w-6 rounded border border-border flex items-center justify-center text-ink-3 hover:bg-surface-2 hover:text-ink disabled:opacity-30 transition-colors"
                        >
                          <ArrowLeft size={11} />
                        </button>
                        {card.audienceImpact && (
                          <span className="text-[10px] font-semibold text-accent">
                            +{card.audienceImpact}
                          </span>
                        )}
                        <button
                          disabled={card.status === 'scheduled'}
                          onClick={() => handleMove(card.id, 'forward')}
                          aria-label="Move forward"
                          className="h-6 w-6 rounded border border-border flex items-center justify-center text-ink-3 hover:bg-surface-2 hover:text-ink disabled:opacity-30 transition-colors"
                        >
                          <ArrowRight size={11} />
                        </button>
                      </div>
                    </div>
                  )) : (
                    <div className="text-center py-8 text-[10px] text-ink-3 border border-dashed border-border rounded-lg">
                      No cards here yet
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Add card modal */}
      <Modal
        open={showAddForm}
        onClose={() => setShowAddForm(false)}
        title="Plan new content"
        size="md"
        footer={
          <>
            <Button variant="secondary" size="sm" onClick={() => setShowAddForm(false)}>Cancel</Button>
            <Button variant="primary" size="sm" form="plan-form" type="submit">Add topic card</Button>
          </>
        }
      >
        <form id="plan-form" onSubmit={handleCreate} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-ink-3 uppercase tracking-wide">
              Video / post title <span className="text-danger">*</span>
            </label>
            <input
              required
              className="form-input w-full px-3 py-2 text-sm rounded-md"
              placeholder="e.g. Next.js App Router Authentication Guide"
              value={newTitle}
              onChange={e => setNewTitle(e.target.value)}
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-ink-3 uppercase tracking-wide">Platform</label>
              <select
                className="form-input w-full px-3 py-2 text-sm rounded-md appearance-none"
                value={newPlatform}
                onChange={e => setNewPlatform(e.target.value)}
              >
                <option value="youtube">YouTube</option>
                <option value="instagram">Instagram</option>
                <option value="facebook">Facebook</option>
                <option value="linkedin">LinkedIn</option>
                <option value="tiktok">TikTok</option>
                <option value="twitter">Twitter / X</option>
              </select>
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-ink-3 uppercase tracking-wide">Stage</label>
              <input
                disabled value="Ideas & queue"
                className="form-input w-full px-3 py-2 text-sm rounded-md opacity-50"
              />
            </div>
          </div>
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-ink-3 uppercase tracking-wide">Concept summary</label>
            <textarea
              rows={2}
              className="form-input w-full px-3 py-2 text-sm rounded-md resize-none"
              placeholder="Briefly describe goals and what audience issue this addresses…"
              value={newDesc}
              onChange={e => setNewDesc(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-ink-3 uppercase tracking-wide flex justify-between">
              <span>Topic outline <span className="normal-case font-normal text-ink-3">(one per line)</span></span>
              <span className="text-ink-3 font-normal normal-case">Optional</span>
            </label>
            <textarea
              rows={3}
              className="form-input w-full px-3 py-2 text-sm rounded-md resize-none font-mono"
              placeholder={"Introduction to Auth\nConnecting middleware.ts\nTesting JWT sessions"}
              value={newOutline}
              onChange={e => setNewOutline(e.target.value)}
            />
          </div>
        </form>
      </Modal>
    </div>
  );
};
