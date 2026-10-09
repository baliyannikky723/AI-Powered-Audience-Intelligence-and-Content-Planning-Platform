import React, { useState } from 'react';
import { useAudienceOverview } from '../../hooks/useApi';
import { StatCard } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { formatCompact } from '../../lib/format';
import {
  Users, Target, TrendingUp, Sparkles, AlertCircle,
  PieChart as PieIcon,
} from 'lucide-react';
import {
  ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip,
} from 'recharts';

export const AudiencePage: React.FC = () => {
  const { data, isLoading, error } = useAudienceOverview();
  const [selectedSegmentId, setSelectedSegmentId] = useState<string>('seg-1');

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="h-24 bg-white rounded-xl border border-slate-200 animate-pulse" />
          ))}
        </div>
        <div className="h-96 bg-white rounded-xl border border-slate-200 animate-pulse" />
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="p-8 text-center bg-white rounded-xl border border-slate-200">
        <AlertCircle className="w-8 h-8 text-danger mx-auto mb-2" />
        <p className="text-sm font-bold text-ink">Failed to load audience intelligence</p>
      </div>
    );
  }

  const activeSegment = data.segments.find(s => s.id === selectedSegmentId) || data.segments[0];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Target className="w-5 h-5 text-indigo-600" />
            Audience Intelligence & Cohort Analysis
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            AI-clustered audience personas, recurring pain points, and sentiment across all connected channels.
          </p>
        </div>
      </div>

      {/* KPI Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          label="Total Community Size"
          value={formatCompact(data.totalAudience)}
          change={14.2}
          caption="Cross-platform reach"
          icon={<Users className="w-4 h-4 text-accent" />}
        />
        <StatCard
          label="Active Monthly Engagers"
          value={formatCompact(data.activeFollowers)}
          change={8.6}
          caption="28.8% Engagement ratio"
          icon={<TrendingUp className="w-4 h-4 text-success" />}
        />
        <StatCard
          label="Avg. Engagement Rate"
          value={`${data.avgEngagementRate}%`}
          change={1.2}
          caption="Industry benchmark: 3.2%"
          icon={<Sparkles className="w-4 h-4 text-warning" />}
        />
        <StatCard
          label="Net Sentiment Index"
          value={`+${data.netSentimentScore}`}
          caption="Positive overall health"
          icon={<PieIcon className="w-4 h-4 text-accent" />}
        />
      </div>

      {/* Main Segment Explorer */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Segment Selector List */}
        <div className="space-y-3">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Discovered Audience Segments ({data.segments.length})
          </h3>
          <div className="space-y-2.5">
            {data.segments.map(seg => {
              const isSelected = seg.id === selectedSegmentId;
              return (
                <div
                  key={seg.id}
                  onClick={() => setSelectedSegmentId(seg.id)}
                  className={`p-4 rounded-xl border transition-all cursor-pointer ${
                    isSelected
                      ? 'bg-indigo-50/50 border-indigo-400 shadow-sm ring-1 ring-indigo-400'
                      : 'bg-white border-slate-200 hover:border-slate-300 hover:bg-slate-50/50'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-xs font-bold text-slate-900">{seg.name}</span>
                    <Badge variant={isSelected ? 'accent' : 'default'} size="xs">
                      {seg.percentage}% of audience
                    </Badge>
                  </div>
                  <div className="flex items-center gap-4 text-xs text-slate-500">
                    <span>Eng. Rate: <strong className="text-slate-700">{seg.engagementRate}%</strong></span>
                    <span>Growth: <strong className="text-emerald-600">+{seg.growth}%</strong></span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Selected Segment Deep Dive Card */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200 p-6 space-y-6">
          <div className="flex items-start justify-between border-b border-slate-100 pb-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <span className="text-xs font-bold uppercase tracking-wider text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded">
                  Cohort Details
                </span>
                <span className="text-xs text-slate-400">• Growth: +{activeSegment.growth}%</span>
              </div>
              <h3 className="text-lg font-bold text-slate-900">{activeSegment.name}</h3>
            </div>
            <div className="text-right">
              <span className="text-xs text-slate-400">Sentiment Score</span>
              <div className="text-xl font-extrabold text-emerald-600">{activeSegment.sentimentScore} / 100</div>
            </div>
          </div>

          {/* Top Interests and Pain Points */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-100 space-y-2">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-indigo-600" />
                Top Content Interests
              </h4>
              <div className="flex flex-wrap gap-1.5">
                {activeSegment.topInterests.map(tag => (
                  <span key={tag} className="text-xs px-2.5 py-1 rounded-md bg-white border border-slate-200 text-slate-800 font-medium">
                    {tag}
                  </span>
                ))}
              </div>
            </div>

            <div className="p-4 rounded-xl bg-rose-50/50 border border-rose-100 space-y-2">
              <h4 className="text-xs font-bold uppercase tracking-wider text-rose-800 flex items-center gap-1.5">
                <AlertCircle className="w-3.5 h-3.5 text-rose-600" />
                Key Recurring Pain Points
              </h4>
              <ul className="space-y-1 text-xs text-rose-900">
                {activeSegment.keyPainPoints.map(pain => (
                  <li key={pain} className="flex items-start gap-1.5">
                    <span className="text-rose-500 font-bold">•</span>
                    <span>{pain}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>

          {/* Historical Sentiment Trend Line */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-3">
              Cohort Sentiment Trend (Last 6 Weeks)
            </h4>
            <div className="h-44">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={data.sentimentTrends}>
                  <XAxis dataKey="date" stroke="#94A3B8" fontSize={11} tickLine={false} />
                  <YAxis stroke="#94A3B8" fontSize={11} tickLine={false} axisLine={false} domain={[0, 100]} />
                  <Tooltip contentStyle={{ background: '#0F172A', color: '#fff', borderRadius: '8px', fontSize: '12px' }} />
                  <Line type="monotone" dataKey="positive" stroke="#10B981" strokeWidth={2.5} name="Positive %" dot={{ r: 3 }} />
                  <Line type="monotone" dataKey="neutral" stroke="#94A3B8" strokeWidth={2} name="Neutral %" dot={{ r: 3 }} />
                  <Line type="monotone" dataKey="negative" stroke="#EF4444" strokeWidth={2} name="Negative %" dot={{ r: 3 }} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
