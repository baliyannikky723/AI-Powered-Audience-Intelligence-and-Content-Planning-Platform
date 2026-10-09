import React from 'react';
import { type ConnectedAccount, type Comment } from '../data/mockData';
import {
  ResponsiveContainer, AreaChart, Area,
  XAxis, YAxis, Tooltip, PieChart, Pie, Cell, BarChart, Bar
} from 'recharts';
import { getPlatformIcon }         from '../lib/platform';
import { formatCompact }           from '../lib/format';
import { StatCard }                from './ui/Card';
import { Button }                  from './ui/Button';
import { Badge }                    from './ui/Badge';
import { Avatar }                  from './ui/Avatar';
import { EmptyState }              from './ui/EmptyState';
import { ChartCard }               from './ui/ChartCard';
import {
  Users, MessageSquare, Smile, AlertCircle,
  ArrowUpRight, BrainCircuit, CornerUpRight,
} from 'lucide-react';

interface DashboardViewProps {
  accounts:   ConnectedAccount[];
  comments:   Comment[];
  onNavigate: (tab: string) => void;
}

export const DashboardView: React.FC<DashboardViewProps> = ({ accounts, comments, onNavigate }) => {

  // ── Metrics ─────────────────────────────────────────────────────────────
  const activePlatforms   = accounts.map(a => a.platform);
  const filteredComments  = comments.filter(c => activePlatforms.includes(c.platform));
  const totalFollowers    = accounts.reduce((s, a) => s + a.followerCount, 0);
  const totalComments     = filteredComments.length;
  const pendingUrgent     = filteredComments.filter(c => c.priority === 'high' && !c.replied).length;
  const totalScore        = filteredComments.reduce((s, c) => s + c.sentimentScore, 0);
  const avgSentimentIndex = totalComments > 0
    ? Math.round(((totalScore / totalComments) + 1) * 50)
    : 0;

  const positiveCount = filteredComments.filter(c => c.sentiment === 'positive').length;
  const neutralCount  = filteredComments.filter(c => c.sentiment === 'neutral').length;
  const negativeCount = filteredComments.filter(c => c.sentiment === 'negative').length;

  const sentimentPieData = [
    { name: 'Positive', value: positiveCount, color: '#15803D' },
    { name: 'Neutral',  value: neutralCount,  color: '#D6D3CC' },
    { name: 'Negative', value: negativeCount,  color: '#B91C1C' },
  ].filter(d => d.value > 0);

  const finalPieData = sentimentPieData.length > 0
    ? sentimentPieData
    : [{ name: 'No data', value: 1, color: '#E7E5E0' }];

  const trendData = [
    { day: 'Aug 15', Positive: 5, Neutral: 3, Negative: 2 },
    { day: 'Aug 16', Positive: 8, Neutral: 5, Negative: 1 },
    { day: 'Aug 17', Positive: 6, Neutral: 4, Negative: 3 },
    { day: 'Aug 18', Positive: 12, Neutral: 6, Negative: 2 },
    { day: 'Aug 19', Positive: 15, Neutral: 7, Negative: 4 },
    { day: 'Aug 20', Positive: 18, Neutral: 10, Negative: 3 },
  ];

  const platformBarData = [
    { name: 'YouTube',   Comments: filteredComments.filter(c => c.platform === 'youtube').length,   color: '#FF0000' },
    { name: 'Instagram', Comments: filteredComments.filter(c => c.platform === 'instagram').length, color: '#E1306C' },
    { name: 'Facebook',  Comments: filteredComments.filter(c => c.platform === 'facebook').length,  color: '#1877F2' },
  ].filter(d => activePlatforms.includes(d.name.toLowerCase()));

  // ── AI Bulletin ──────────────────────────────────────────────────────────
  const generateAiSummary = () => {
    if (accounts.length === 0)
      return 'No connected platforms detected. Connect your social channels in the Integrations tab to enable AI audience summaries.';

    let text = `Across ${accounts.length} connected platform${accounts.length > 1 ? 's' : ''}, the overall audience reception is `;
    if (avgSentimentIndex > 70)      text += 'highly enthusiastic and positive. ';
    else if (avgSentimentIndex > 45) text += 'generally supportive, with constructive technical feedback. ';
    else                              text += 'mixed, with some platform errors requiring attention. ';

    const issues: string[] = [];
    if (filteredComments.some(c => c.tags.includes('audio-issue')))
      issues.push('audio quality normalizations on recent uploads');
    if (filteredComments.some(c => c.tags.includes('code-error') || c.tags.includes('bug')))
      issues.push('broken code snippets from third-party updates');
    if (filteredComments.some(c => c.tags.includes('pricing') || c.tags.includes('course')))
      issues.push('course launch pricing inquiries');

    if (issues.length > 0)
      text += `Key topics requiring action: ${issues.join(', ')}. `;

    text += 'Recommendation: draft a dedicated response to audio feedback and push code updates to address developer queries.';
    return text;
  };

  const criticalComments = filteredComments
    .filter(c => !c.replied && (c.priority === 'high' || c.sentiment === 'negative'))
    .slice(0, 3);

  const tooltipStyle = {
    contentStyle: {
      background: '#fff',
      border: '1px solid #E7E5E0',
      borderRadius: 8,
      fontSize: 12,
      boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
    },
  };

  return (
    <div className="space-y-6 animate-slide-up">

      {/* ── Page header ───────────────────────────────────────────────── */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-[22px] font-semibold text-ink leading-tight">Audience intelligence</h2>
          <p className="text-sm text-ink-3 mt-0.5">Real-time sentiment and comment trends across connected channels.</p>
        </div>
        <Button
          variant="primary"
          size="sm"
          icon={<BrainCircuit size={14} />}
          onClick={() => onNavigate('chat')}
        >
          Ask AI
        </Button>
      </div>

      {/* ── KPI Cards ─────────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          label="Total followers"
          value={totalFollowers > 0 ? formatCompact(totalFollowers) : '—'}
          change={totalFollowers > 0 ? 4.2 : undefined}
          caption="Active audience across channels"
          icon={<Users size={16} />}
          iconColor="bg-accent-soft"
        />
        <StatCard
          label="Comments synced"
          value={totalComments}
          caption="Aggregated social comment data"
          icon={<MessageSquare size={16} />}
          iconColor="bg-info-soft"
        />
        <StatCard
          label="Sentiment score"
          value={avgSentimentIndex > 0 ? `${avgSentimentIndex}%` : '—'}
          caption={
            avgSentimentIndex > 65 ? 'Mostly positive'
            : avgSentimentIndex > 40 ? 'Mixed / neutral'
            : accounts.length > 0 ? 'Negative alerts'
            : 'No channels connected'
          }
          icon={<Smile size={16} />}
          iconColor="bg-success-soft"
        />
        <StatCard
          label="Pending urgents"
          value={pendingUrgent}
          caption="High-priority unreplied comments"
          icon={<AlertCircle size={16} />}
          iconColor={pendingUrgent > 0 ? 'bg-danger-soft' : 'bg-surface-2'}
        />
      </div>

      {/* ── AI Bulletin ───────────────────────────────────────────────── */}
      <div className="panel p-4">
        <div className="flex items-start gap-3">
          <div className="h-8 w-8 rounded-lg bg-accent-soft border border-accent/20 flex items-center justify-center flex-shrink-0">
            <BrainCircuit size={16} className="text-accent" />
          </div>
          <div>
            <div className="flex items-center gap-2 mb-1">
              <h3 className="text-sm font-semibold text-ink">AI audience bulletin</h3>
              <Badge variant="accent" size="xs">Live summary</Badge>
            </div>
            <p className="text-sm text-ink-2 leading-relaxed">{generateAiSummary()}</p>
          </div>
        </div>
      </div>

      {/* ── Charts ────────────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">

        {/* Trend area chart */}
        <ChartCard
          title="Sentiment dynamics"
          subtitle="Daily breakdown over the last 6 days"
          height={220}
          empty={accounts.length === 0}
          className="lg:col-span-2"
        >
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={trendData} margin={{ top: 8, right: 8, left: -24, bottom: 0 }}>
              <defs>
                <linearGradient id="posGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor="#15803D" stopOpacity={0.12}/>
                  <stop offset="95%" stopColor="#15803D" stopOpacity={0}/>
                </linearGradient>
                <linearGradient id="neuGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor="#8A857E" stopOpacity={0.12}/>
                  <stop offset="95%" stopColor="#8A857E" stopOpacity={0}/>
                </linearGradient>
                <linearGradient id="negGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor="#B91C1C" stopOpacity={0.12}/>
                  <stop offset="95%" stopColor="#B91C1C" stopOpacity={0}/>
                </linearGradient>
              </defs>
              <XAxis dataKey="day" stroke="#8A857E" fontSize={11} tickLine={false} axisLine={false}/>
              <YAxis stroke="#8A857E" fontSize={11} tickLine={false} axisLine={false}/>
              <Tooltip {...tooltipStyle}/>
              <Area type="monotone" dataKey="Positive" stroke="#15803D" strokeWidth={1.5} fillOpacity={1} fill="url(#posGrad)"/>
              <Area type="monotone" dataKey="Neutral"  stroke="#8A857E" strokeWidth={1.5} fillOpacity={1} fill="url(#neuGrad)"/>
              <Area type="monotone" dataKey="Negative" stroke="#B91C1C" strokeWidth={1.5} fillOpacity={1} fill="url(#negGrad)"/>
            </AreaChart>
          </ResponsiveContainer>
        </ChartCard>

        {/* Pie + bar */}
        <div className="panel p-4 flex flex-col gap-6">
          {/* Sentiment split */}
          <div>
            <h3 className="text-sm font-semibold text-ink mb-1">Sentiment split</h3>
            <p className="text-xs text-ink-3 mb-3">Ratio of audience emotional feedback</p>
            <div className="h-28">
              {accounts.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Tooltip {...tooltipStyle}/>
                    <Pie
                      data={finalPieData} cx="50%" cy="50%"
                      innerRadius={34} outerRadius={50}
                      paddingAngle={3} dataKey="value"
                    >
                      {finalPieData.map((e, i) => <Cell key={i} fill={e.color}/>)}
                    </Pie>
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-full flex items-center justify-center text-xs text-ink-3">
                  No data
                </div>
              )}
            </div>
            {accounts.length > 0 && (
              <div className="flex gap-3 text-xs mt-2">
                {[
                  { label: `Positive (${positiveCount})`, color: '#15803D' },
                  { label: `Neutral (${neutralCount})`,   color: '#D6D3CC' },
                  { label: `Negative (${negativeCount})`, color: '#B91C1C' },
                ].map(d => (
                  <div key={d.label} className="flex items-center gap-1 text-ink-3">
                    <span className="h-2 w-2 rounded-full flex-shrink-0" style={{ background: d.color }}/>
                    {d.label}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Platform bar */}
          <div className="border-t border-border pt-4">
            <h3 className="text-sm font-semibold text-ink mb-1">Comments by platform</h3>
            <div className="h-20">
              {platformBarData.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={platformBarData} layout="vertical" margin={{ top: 0, right: 8, left: -20, bottom: 0 }}>
                    <XAxis type="number" stroke="#8A857E" fontSize={10} tickLine={false} axisLine={false}/>
                    <YAxis dataKey="name" type="category" stroke="#8A857E" fontSize={10} tickLine={false} axisLine={false}/>
                    <Tooltip {...tooltipStyle}/>
                    <Bar dataKey="Comments" radius={[0, 3, 3, 0]}>
                      {platformBarData.map((e, i) => <Cell key={i} fill={e.color}/>)}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-full flex items-center justify-center text-xs text-ink-3">No data</div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* ── Critical comments ──────────────────────────────────────────── */}
      <div className="panel p-4">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="text-sm font-semibold text-ink">Critical pending comments</h3>
            <p className="text-xs text-ink-3 mt-0.5">High-priority or negative comments requiring replies</p>
          </div>
          <Button
            variant="ghost"
            size="sm"
            iconRight={<ArrowUpRight size={14} />}
            onClick={() => onNavigate('comments')}
          >
            Manage feed
          </Button>
        </div>

        {criticalComments.length > 0 ? (
          <div className="space-y-2">
            {criticalComments.map(comment => (
              <div
                key={comment.id}
                className="flex items-start justify-between gap-4 rounded-lg border border-border p-3 hover:bg-surface-2 transition-colors"
              >
                <div className="flex items-start gap-3 min-w-0">
                  <Avatar src={comment.authorAvatar} name={comment.author} size="sm" className="flex-shrink-0" />
                  <div className="min-w-0">
                    <div className="flex items-center gap-1.5 mb-0.5">
                      <span className="text-sm font-semibold text-ink">{comment.author}</span>
                      <span>{getPlatformIcon(comment.platform, 12)}</span>
                      <span className="text-xs text-ink-3 truncate max-w-[120px]">{comment.postTitle}</span>
                    </div>
                    <p className="text-sm text-ink-2 line-clamp-2">"{comment.text}"</p>
                  </div>
                </div>
                <div className="flex flex-col items-end gap-1.5 flex-shrink-0">
                  <Badge variant="danger" size="xs">Urgent</Badge>
                  <button
                    onClick={() => onNavigate('comments')}
                    className="text-xs text-accent hover:underline font-medium flex items-center gap-0.5"
                  >
                    Reply <CornerUpRight size={10} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <EmptyState
            title={accounts.length > 0 ? 'All clear!' : 'No channels connected'}
            body={accounts.length > 0
              ? 'No high-priority comments pending. Keep up the great work.'
              : 'Connect an account under Integrations to monitor comments.'}
            className="py-8"
          />
        )}
      </div>
    </div>
  );
};
