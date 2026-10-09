import React, { useState } from 'react';
import { useTrends } from '../../hooks/useApi';
import { Button } from '../../components/ui/Button';
import { getPlatformIcon } from '../../lib/platform';
import {
  Flame, Zap, ArrowUpRight,
  Sparkles,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const CATEGORIES = [
  { id: 'all', label: 'All Signals' },
  { id: 'ai', label: 'AI & LLMs' },
  { id: 'tech', label: 'Frontend / Tech' },
  { id: 'design', label: 'UI/UX Design' },
  { id: 'business', label: 'Creator Business' },
];

export const TrendsPage: React.FC = () => {
  const navigate = useNavigate();
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const { data: trends, isLoading } = useTrends(selectedCategory);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Flame className="w-5 h-5 text-amber-500" />
            Trends & Viral Signals Radar
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Breakout discussions and high-velocity topics detected across tech social communities.
          </p>
        </div>

        <Button
          variant="primary"
          size="sm"
          onClick={() => navigate('/app/planner')}
          className="text-xs"
        >
          <Sparkles className="w-3.5 h-3.5 mr-1.5" />
          Plan Content from Trends
        </Button>
      </div>

      {/* Category Pills */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1">
        {CATEGORIES.map(cat => (
          <button
            key={cat.id}
            onClick={() => setSelectedCategory(cat.id)}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
              selectedCategory === cat.id
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
            }`}
          >
            {cat.label}
          </button>
        ))}
      </div>

      {/* Trends List */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="h-44 bg-white rounded-xl border border-slate-200 animate-pulse" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {trends?.map(trend => (
            <div
              key={trend.id}
              className="bg-white rounded-xl border border-slate-200 p-5 space-y-3 hover:border-indigo-300 hover:shadow-sm transition-all flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between gap-2 mb-2">
                  <div className="flex items-center gap-2">
                    <span className="p-1.5 rounded-md bg-slate-100 text-slate-700">
                      {getPlatformIcon(trend.platform, 14)}
                    </span>
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                      {trend.category}
                    </span>
                  </div>
                  <div className="flex items-center gap-1.5 text-xs font-bold text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full">
                    <Zap className="w-3.5 h-3.5 fill-amber-500 text-amber-500" />
                    <span>Velocity {trend.velocity}/100</span>
                  </div>
                </div>

                <h3 className="text-base font-bold text-slate-900 leading-snug">
                  {trend.topic}
                </h3>

                <div className="p-3 rounded-lg bg-slate-50 border border-slate-100 mt-2.5">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-600 block mb-0.5">
                    Recommended Content Angle
                  </span>
                  <p className="text-xs text-slate-700">{trend.recommendedAngle}</p>
                </div>
              </div>

              <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-xs">
                <span className="text-slate-400">Estimated volume: ~{trend.volume} mentions/day</span>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => navigate('/app/planner')}
                  className="text-xs text-indigo-600 font-semibold p-0 hover:bg-transparent"
                >
                  Create Card <ArrowUpRight className="w-3.5 h-3.5 ml-1" />
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
