import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useDashboardOverview } from '../../hooks/useApi';
import {
  ResponsiveContainer, AreaChart, Area,
  XAxis, YAxis, Tooltip, PieChart, Pie, Cell, BarChart, Bar
} from 'recharts';
import { getPlatformIcon } from '../../lib/platform';
import { formatCompact } from '../../lib/format';
import { StatCard } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Avatar } from '../../components/ui/Avatar';
import { ChartCard } from '../../components/ui/ChartCard';
import {
  Users, MessageSquare, Smile, AlertCircle,
  ArrowUpRight, BrainCircuit, CornerUpRight, RefreshCw,
} from 'lucide-react';

export const OverviewPage: React.FC = () => {
  const navigate = useNavigate();
  const { data, isLoading, error, refetch, isFetching } = useDashboardOverview();

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="h-28 rounded-xl bg-white border border-slate-200 animate-pulse p-4" />
          ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 h-72 rounded-xl bg-white border border-slate-200 animate-pulse" />
          <div className="h-72 rounded-xl bg-white border border-slate-200 animate-pulse" />
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="p-8 text-center bg-white rounded-xl border border-slate-200">
        <AlertCircle className="w-8 h-8 text-danger mx-auto mb-2" />
        <h3 className="font-bold text-ink">Failed to load overview data</h3>
        <p className="text-xs text-ink-3 mt-1 mb-4">Please check your connection or retry.</p>
        <Button variant="secondary" size="sm" onClick={() => refetch()}>
          Retry
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Top Banner with Quick Query shortcut */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 p-5 rounded-2xl bg-gradient-to-r from-indigo-600 via-indigo-700 to-purple-700 text-white shadow-sm">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold uppercase tracking-wider bg-white/20 px-2 py-0.5 rounded-full">
              Live Overview
            </span>
            <span className="text-xs text-indigo-100 flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              Synced across 3 platforms
            </span>
          </div>
          <h2 className="text-xl font-extrabold tracking-tight text-white">
            Audience Engagement is up 12.4% this week
          </h2>
          <p className="text-xs text-indigo-100/90 max-w-xl">
            High positive sentiment detected around your React 19 architecture posts. 4 unresolved high-priority feedback items require attention.
          </p>
        </div>

        <div className="flex items-center gap-2 flex-shrink-0">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => refetch()}
            disabled={isFetching}
            className="bg-white/10 hover:bg-white/20 text-white border-white/20 text-xs"
          >
            <RefreshCw className={`w-3.5 h-3.5 mr-1.5 ${isFetching ? 'animate-spin' : ''}`} />
            Refresh
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={() => navigate('/app/chat')}
            className="bg-white text-indigo-700 hover:bg-slate-100 font-bold shadow text-xs border-0"
          >
            <BrainCircuit className="w-3.5 h-3.5 mr-1.5 text-indigo-600" />
            Ask PulseGPT
          </Button>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          label="Total Audience"
          value={formatCompact(data.totalAudience)}
          change={data.audienceGrowth}
          caption="Across 3 connected accounts"
          icon={<Users className="w-4 h-4" />}
        />
        <StatCard
          label="Monitored Comments"
          value={formatCompact(data.totalComments)}
          change={data.commentsGrowth}
          caption="Last 30 days processed"
          icon={<MessageSquare className="w-4 h-4" />}
        />
        <StatCard
          label="Positive Sentiment"
          value={`${data.sentimentRatio}%`}
          change={data.sentimentGrowth}
          caption="Net sentiment score: +78"
          icon={<Smile className="w-4 h-4" />}
        />
        <div onClick={() => navigate('/app/comments')} className="cursor-pointer">
          <StatCard
            label="Actionable Issues"
            value={data.highPriorityIssues}
            caption="Unresolved high priority items"
            icon={<AlertCircle className="w-4 h-4 text-warning" />}
          />
        </div>
      </div>

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Engagement Trend */}
        <div className="lg:col-span-2">
          <ChartCard
            title="Weekly Audience Reach & Comments"
            subtitle="Combined engagement across YouTube, Instagram & Facebook"
          >
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={data.engagementData}>
                  <defs>
                    <linearGradient id="viewsGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#6366F1" stopOpacity={0.25} />
                      <stop offset="95%" stopColor="#6366F1" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <XAxis dataKey="day" stroke="#94A3B8" fontSize={11} tickLine={false} />
                  <YAxis stroke="#94A3B8" fontSize={11} tickLine={false} axisLine={false} tickFormatter={v => formatCompact(v)} />
                  <Tooltip contentStyle={{ background: '#0F172A', color: '#fff', borderRadius: '8px', fontSize: '12px' }} />
                  <Area type="monotone" dataKey="views" stroke="#6366F1" strokeWidth={2.5} fillOpacity={1} fill="url(#viewsGrad)" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </ChartCard>
        </div>

        {/* Sentiment Distribution */}
        <div>
          <ChartCard title="Sentiment Breakdown" subtitle="Algorithmic classification of recent feedback">
            <div className="h-52 flex items-center justify-center">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={data.sentimentBreakdown} cx="50%" cy="50%" innerRadius={55} outerRadius={78} paddingAngle={4} dataKey="value">
                    {data.sentimentBreakdown.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{ background: '#0F172A', color: '#fff', borderRadius: '8px', fontSize: '12px' }} />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="flex justify-center gap-4 text-xs font-medium text-slate-600 border-t border-slate-100 pt-3">
              {data.sentimentBreakdown.map(item => (
                <div key={item.name} className="flex items-center gap-1.5">
                  <span className="w-2.5 h-2.5 rounded-full" style={{ background: item.color }} />
                  <span>{item.name} ({item.value})</span>
                </div>
              ))}
            </div>
          </ChartCard>
        </div>
      </div>

      {/* Platform Breakdown & Recent Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Platform Share */}
        <div>
          <ChartCard title="Platform Ingestion" subtitle="Comments processed per platform">
            <div className="h-60">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data.platformBreakdown}>
                  <XAxis dataKey="name" stroke="#94A3B8" fontSize={11} tickLine={false} />
                  <YAxis stroke="#94A3B8" fontSize={11} tickLine={false} axisLine={false} />
                  <Tooltip contentStyle={{ background: '#0F172A', color: '#fff', borderRadius: '8px', fontSize: '12px' }} />
                  <Bar dataKey="count" radius={[4, 4, 0, 0]}>
                    {data.platformBreakdown.map((entry, index) => (
                      <Cell key={`bar-${index}`} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </ChartCard>
        </div>

        {/* Recent High Priority Comments */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200 p-5 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div>
              <h3 className="text-sm font-bold text-slate-900">Recent Audience Comments</h3>
              <p className="text-xs text-slate-500">Live stream of incoming community feedback</p>
            </div>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => navigate('/app/comments')}
              className="text-xs font-semibold text-indigo-600"
            >
              View All <ArrowUpRight className="w-3.5 h-3.5 ml-1" />
            </Button>
          </div>

          <div className="divide-y divide-slate-100">
            {data.recentComments.map(comment => (
              <div key={comment.id} className="py-3 flex items-start gap-3 hover:bg-slate-50/60 p-2 rounded-lg transition-colors">
                <Avatar name={comment.author} size="sm" />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2 mb-1">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-slate-900">{comment.author}</span>
                      <span className="flex items-center gap-1 text-[11px] text-slate-500">
                        {getPlatformIcon(comment.platform, 12)}
                        <span className="capitalize">{comment.platform}</span>
                      </span>
                    </div>
                    <Badge variant={comment.sentiment === 'positive' ? 'success' : comment.sentiment === 'negative' ? 'danger' : 'default'} size="xs">
                      {comment.sentiment}
                    </Badge>
                  </div>
                  <p className="text-xs text-slate-700 line-clamp-1">{comment.text}</p>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => navigate('/app/comments')}
                  className="text-xs text-indigo-600 flex-shrink-0"
                >
                  <CornerUpRight className="w-3.5 h-3.5 mr-1" />
                  Reply
                </Button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
