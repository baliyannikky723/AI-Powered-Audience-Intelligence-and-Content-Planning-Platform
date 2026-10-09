import React, { useState } from 'react';
import { type Comment, generateSmartReply } from '../data/mockData';
import { getPlatformIcon }  from '../lib/platform';
import { formatDateTime }   from '../lib/format';
import { Input }            from './ui/Input';
import { Button }           from './ui/Button';
import { SentimentBadge, PriorityBadge } from './ui/Badge';
import { Avatar }           from './ui/Avatar';
import { EmptyState }       from './ui/EmptyState';
import { ProgressBar }      from './ui/ProgressBar';
import {
  Search, Brain, Check, Send, Sparkles, AlertCircle, HelpCircle,
} from 'lucide-react';

// Re-export Select from ui/Input for convenience
export { Select } from './ui/Input';

interface CommentsViewProps {
  comments:           Comment[];
  connectedPlatforms: string[];
  onPostReply:        (commentId: string, replyText: string) => void;
}

export const CommentsView: React.FC<CommentsViewProps> = ({
  comments,
  connectedPlatforms,
  onPostReply,
}) => {
  // ── Filter state ────────────────────────────────────────────────────────
  const [selectedId,      setSelectedId]      = useState<string | null>(null);
  const [searchQuery,     setSearchQuery]      = useState('');
  const [platformFilter,  setPlatformFilter]   = useState('all');
  const [sentimentFilter, setSentimentFilter]  = useState<'all' | 'positive' | 'neutral' | 'negative'>('all');
  const [priorityFilter,  setPriorityFilter]   = useState<'all' | 'high' | 'medium' | 'low'>('all');

  // ── Reply composer state ────────────────────────────────────────────────
  const [selectedTone,     setSelectedTone]     = useState<'professional' | 'friendly' | 'witty' | 'apologetic'>('friendly');
  const [editedReplyText,  setEditedReplyText]  = useState('');
  const [isSubmitting,     setIsSubmitting]      = useState(false);
  const [justReplied,      setJustReplied]       = useState(false);

  const selectedComment = comments.find(c => c.id === selectedId);

  // ── Filtered comments ───────────────────────────────────────────────────
  const filtered = comments.filter(c => {
    if (!connectedPlatforms.includes(c.platform)) return false;
    const q = searchQuery.toLowerCase();
    const matchSearch = !q
      || c.text.toLowerCase().includes(q)
      || c.author.toLowerCase().includes(q)
      || c.postTitle.toLowerCase().includes(q)
      || c.tags.some(t => t.toLowerCase().includes(q));
    const matchPlatform  = platformFilter  === 'all' || c.platform  === platformFilter;
    const matchSentiment = sentimentFilter === 'all' || c.sentiment === sentimentFilter;
    const matchPriority  = priorityFilter  === 'all' || c.priority  === priorityFilter;
    return matchSearch && matchPlatform && matchSentiment && matchPriority;
  });

  // ── Handlers ────────────────────────────────────────────────────────────
  const handleSelect = (comment: Comment) => {
    setSelectedId(comment.id);
    setJustReplied(false);
    const init = comment.replied && comment.replyText
      ? comment.replyText
      : generateSmartReply(comment.text, selectedTone);
    setEditedReplyText(init);
  };

  const handleToneChange = (tone: typeof selectedTone) => {
    setSelectedTone(tone);
    if (selectedComment) {
      setEditedReplyText(generateSmartReply(selectedComment.text, tone));
    }
  };

  const handlePublishReply = () => {
    if (!selectedId || !editedReplyText.trim()) return;
    setIsSubmitting(true);
    setTimeout(() => {
      onPostReply(selectedId, editedReplyText);
      setIsSubmitting(false);
      setJustReplied(true);
    }, 1200);
  };

  // ── Sentiment meter value ────────────────────────────────────────────────
  const sentimentPct = selectedComment
    ? Math.round((selectedComment.sentimentScore + 1) * 50)
    : 50;

  return (
    <div className="space-y-4 animate-slide-up">

      {/* Page header */}
      <div>
        <h2 className="text-[22px] font-semibold text-ink">Comments workspace</h2>
        <p className="text-sm text-ink-3 mt-0.5">Analyze comments, filter by sentiment, and draft AI-suggested replies.</p>
      </div>

      {/* Filter bar */}
      <div className="panel p-3 flex flex-col sm:flex-row gap-2 items-stretch sm:items-center">
        <div className="flex-1">
          <Input
            leftElement={<Search size={14} />}
            placeholder="Search comments, authors, post titles, or #tags…"
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
          />
        </div>
        <div className="flex gap-2 flex-wrap sm:flex-nowrap">
          <select
            className="form-input h-9 px-3 pr-8 text-sm appearance-none cursor-pointer rounded-md"
            value={platformFilter}
            onChange={e => setPlatformFilter(e.target.value)}
            aria-label="Filter by platform"
          >
            <option value="all">All platforms</option>
            <option value="youtube">YouTube</option>
            <option value="instagram">Instagram</option>
            <option value="facebook">Facebook</option>
            {connectedPlatforms
              .filter(p => !['youtube', 'instagram', 'facebook'].includes(p))
              .map(p => <option key={p} value={p} className="capitalize">{p}</option>)}
          </select>
          <select
            className="form-input h-9 px-3 pr-8 text-sm appearance-none cursor-pointer rounded-md"
            value={sentimentFilter}
            onChange={e => setSentimentFilter(e.target.value as any)}
            aria-label="Filter by sentiment"
          >
            <option value="all">All sentiments</option>
            <option value="positive">Positive</option>
            <option value="neutral">Neutral</option>
            <option value="negative">Negative</option>
          </select>
          <select
            className="form-input h-9 px-3 pr-8 text-sm appearance-none cursor-pointer rounded-md"
            value={priorityFilter}
            onChange={e => setPriorityFilter(e.target.value as any)}
            aria-label="Filter by priority"
          >
            <option value="all">All priorities</option>
            <option value="high">High priority</option>
            <option value="medium">Medium</option>
            <option value="low">Low</option>
          </select>
        </div>
      </div>

      {/* Main workspace */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 items-start">

        {/* Comment feed */}
        <div className="lg:col-span-2 space-y-2 max-h-[680px] overflow-y-auto pr-1">
          {connectedPlatforms.length === 0 ? (
            <EmptyState
              icon={<AlertCircle size={24} />}
              title="No platforms connected"
              body="Link a YouTube, Instagram, or Facebook account in Integrations to load comments."
              className="panel py-16"
            />
          ) : filtered.length > 0 ? (
            filtered.map(comment => {
              const isSelected = selectedId === comment.id;
              return (
                <button
                  key={comment.id}
                  onClick={() => handleSelect(comment)}
                  aria-pressed={isSelected}
                  className={`w-full text-left panel p-4 transition-all ${
                    isSelected
                      ? 'border-accent ring-1 ring-accent/20'
                      : 'hover:border-border-strong'
                  }`}
                >
                  <div className="flex gap-3">
                    <Avatar src={comment.authorAvatar} name={comment.author} size="sm" className="flex-shrink-0 mt-0.5" />
                    <div className="flex-1 min-w-0">
                      {/* Header row */}
                      <div className="flex items-center justify-between gap-2 mb-1">
                        <div className="flex items-center gap-1.5 min-w-0">
                          <span className="text-sm font-semibold text-ink">{comment.author}</span>
                          <span className="flex-shrink-0">{getPlatformIcon(comment.platform, 12)}</span>
                          <span className="text-xs text-ink-3 truncate hidden sm:block">{comment.postTitle}</span>
                        </div>
                        <span className="text-xs text-ink-3 flex-shrink-0">
                          {formatDateTime(comment.publishedAt)}
                        </span>
                      </div>

                      {/* Text */}
                      <p className="text-sm text-ink-2 leading-relaxed mb-2">"{comment.text}"</p>

                      {/* Badges + tags */}
                      <div className="flex items-center justify-between gap-2 flex-wrap">
                        <div className="flex flex-wrap gap-1.5">
                          <SentimentBadge sentiment={comment.sentiment} />
                          <PriorityBadge  priority={comment.priority}  />
                          {comment.tags.map(tag => (
                            <span key={tag} className="text-[10px] px-1.5 py-0.5 rounded-full bg-accent-soft text-accent-ink font-medium">
                              #{tag}
                            </span>
                          ))}
                        </div>
                        {comment.replied && (
                          <span className="inline-flex items-center gap-1 text-xs text-success font-semibold">
                            <Check size={12} /> Replied
                          </span>
                        )}
                      </div>

                      {/* Replied snippet */}
                      {comment.replied && comment.replyText && (
                        <div className="mt-2 pl-3 border-l-2 border-success bg-success-soft rounded-r-md py-2 pr-2">
                          <span className="text-xs font-semibold text-success block mb-0.5">Your reply:</span>
                          <span className="text-xs text-ink-2">"{comment.replyText}"</span>
                        </div>
                      )}
                    </div>
                  </div>
                </button>
              );
            })
          ) : (
            <EmptyState
              icon={<HelpCircle size={24} />}
              title="No comments match"
              body="Try adjusting your search or widening your filter criteria."
              className="panel py-16"
            />
          )}
        </div>

        {/* AI Reply Copilot panel */}
        <div className="panel p-4 sticky top-6">
          {selectedComment ? (
            <div className="space-y-4 animate-slide-up">
              {/* Header */}
              <div className="border-b border-border pb-3">
                <div className="flex items-center gap-1.5 text-xs font-semibold text-accent uppercase tracking-wide mb-1">
                  <Brain size={13} /> AI sentiment analyzer
                </div>
                <h3 className="text-sm font-semibold text-ink">
                  Reply copilot for {selectedComment.author}
                </h3>
              </div>

              {/* Sentiment meter */}
              <div className="bg-surface-2 rounded-lg p-3 border border-border space-y-2">
                <div className="flex justify-between items-center text-xs">
                  <span className="text-ink-3">Semantic score</span>
                  <span className={`font-semibold ${
                    selectedComment.sentiment === 'positive' ? 'text-success' :
                    selectedComment.sentiment === 'negative' ? 'text-danger'  : 'text-ink-2'
                  }`}>
                    {selectedComment.sentimentScore > 0
                      ? `+${selectedComment.sentimentScore}`
                      : selectedComment.sentimentScore}
                  </span>
                </div>
                <ProgressBar
                  value={sentimentPct}
                  color={
                    selectedComment.sentiment === 'positive' ? 'success' :
                    selectedComment.sentiment === 'negative' ? 'danger'  : 'accent'
                  }
                  size="sm"
                />
                <div className="flex justify-between text-[9px] text-ink-3 uppercase tracking-wider font-semibold">
                  <span>Negative</span>
                  <span>Neutral</span>
                  <span>Positive</span>
                </div>
              </div>

              {/* Tone selector */}
              <div>
                <label className="text-xs font-semibold text-ink-3 uppercase tracking-wide flex items-center gap-1 mb-2">
                  <Sparkles size={11} /> Tone
                </label>
                <div className="grid grid-cols-4 gap-1 p-1 bg-surface-2 border border-border rounded-lg">
                  {(['friendly', 'professional', 'witty', 'apologetic'] as const).map(tone => (
                    <button
                      key={tone}
                      disabled={selectedComment.replied && !justReplied}
                      onClick={() => handleToneChange(tone)}
                      className={`py-1.5 rounded-md text-xs font-semibold capitalize transition-colors ${
                        selectedTone === tone
                          ? 'bg-accent text-white'
                          : 'text-ink-3 hover:text-ink hover:bg-surface disabled:opacity-40'
                      }`}
                    >
                      {tone}
                    </button>
                  ))}
                </div>
              </div>

              {/* Reply textarea */}
              <div className="relative">
                <label className="text-xs font-semibold text-ink-3 uppercase tracking-wide block mb-2">
                  Draft reply
                </label>
                <textarea
                  rows={5}
                  disabled={(selectedComment.replied && !justReplied) || isSubmitting}
                  value={editedReplyText}
                  onChange={e => setEditedReplyText(e.target.value)}
                  placeholder="Draft your reply…"
                  className="form-input w-full p-3 text-sm rounded-lg resize-none"
                />
                {isSubmitting && (
                  <div className="absolute inset-0 bg-surface/80 rounded-lg flex items-center justify-center gap-2 text-sm text-ink-2">
                    <svg className="animate-spin h-4 w-4 text-accent" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3"/>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                    </svg>
                    Publishing reply…
                  </div>
                )}
              </div>

              {/* Action */}
              {selectedComment.replied && !justReplied ? (
                <div className="flex items-center justify-center gap-1.5 py-2 text-sm text-success font-semibold bg-success-soft border border-success-border rounded-lg">
                  <Check size={14} /> Reply has been posted
                </div>
              ) : justReplied ? (
                <div className="flex items-center justify-center gap-1.5 py-2 text-sm text-success font-semibold bg-success-soft border border-success-border rounded-lg animate-slide-up">
                  <Check size={14} /> Reply posted successfully
                </div>
              ) : (
                <Button
                  variant="primary"
                  size="md"
                  loading={isSubmitting}
                  disabled={!editedReplyText.trim()}
                  icon={<Send size={13} />}
                  className="w-full"
                  onClick={handlePublishReply}
                >
                  Post reply to {selectedComment.platform}
                </Button>
              )}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center text-center py-12 space-y-3">
              <div className="h-12 w-12 rounded-xl bg-surface-2 border border-border flex items-center justify-center text-ink-3">
                <Brain size={20} />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-ink">AI copilot</h4>
                <p className="text-xs text-ink-3 mt-1 max-w-[180px] leading-relaxed">
                  Select a comment to run sentiment analysis and generate a smart reply.
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
