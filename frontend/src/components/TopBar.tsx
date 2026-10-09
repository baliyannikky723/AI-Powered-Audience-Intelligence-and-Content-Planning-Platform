import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { cn } from '../lib/cn';
import { Bell, Search, ChevronDown, Activity, Menu, LogOut } from 'lucide-react';
import { Avatar } from './ui/Avatar';
import { useAuth } from '../context/AuthContext';
import { useIntegrations } from '../hooks/useApi';
import { platformDotClass, platformLabel } from '../lib/platform';

interface TopBarProps {
  onMobileMenuOpen: () => void;
}

const ROUTE_LABELS: Record<string, { title: string; category: string }> = {
  '/app/overview': { title: 'Overview Dashboard', category: 'Core Workflow' },
  '/app/comments': { title: 'Audience Comments', category: 'Core Workflow' },
  '/app/chat': { title: 'Ask PulseGPT (RAG)', category: 'Core Workflow' },
  '/app/planner': { title: 'Content Planner Kanban', category: 'Core Workflow' },
  '/app/calendar': { title: 'Editorial Publishing Calendar', category: 'Core Workflow' },
  '/app/audience': { title: 'Audience Intelligence & Cohorts', category: 'Audience Intelligence' },
  '/app/trends': { title: 'Trends & Viral Signals Radar', category: 'Audience Intelligence' },
  '/app/memory': { title: 'Audience Long-term Memory', category: 'Audience Intelligence' },
  '/app/recommendations': { title: 'AI Post Recommendations', category: 'Audience Intelligence' },
  '/app/evaluation': { title: 'LLM Guardrails & RAG Accuracy', category: 'Audience Intelligence' },
  '/app/integrations': { title: 'Social Integrations & OAuth', category: 'Management' },
  '/app/settings': { title: 'Workspace Settings & API Keys', category: 'Management' },
  '/admin': { title: 'System Administration', category: 'Admin Only' },
};

export const TopBar: React.FC<TopBarProps> = ({ onMobileMenuOpen }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { data: accounts } = useIntegrations();

  const [searchQuery, setSearchQuery] = useState('');
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [isNotifOpen, setIsNotifOpen] = useState(false);

  const currentRouteInfo = ROUTE_LABELS[location.pathname] || {
    title: 'PulseGPT Workspace',
    category: 'Application',
  };

  const connectedAccounts = (accounts || []).filter(a => a.status === 'connected');

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/app/comments?search=${encodeURIComponent(searchQuery)}`);
    }
  };

  return (
    <header className="h-14 bg-white border-b border-slate-200 px-4 flex items-center justify-between sticky top-0 z-20 shadow-xs">
      {/* Left: Mobile hamburger & breadcrumbs */}
      <div className="flex items-center gap-3">
        <button
          onClick={onMobileMenuOpen}
          aria-label="Open navigation menu"
          className="lg:hidden p-2 rounded-lg text-slate-600 hover:bg-slate-100 transition-colors"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div>
          <div className="flex items-center gap-1.5 text-[10px] uppercase font-bold text-slate-400 tracking-wider">
            <span>{currentRouteInfo.category}</span>
            <span>/</span>
          </div>
          <h1 className="text-sm font-bold text-slate-900 leading-none">
            {currentRouteInfo.title}
          </h1>
        </div>
      </div>

      {/* Center: Search */}
      <div className="hidden md:flex items-center flex-1 max-w-sm mx-6">
        <form onSubmit={handleSearch} className="relative w-full">
          <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search comments, topics or creator memory (Press Enter)..."
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            className="w-full pl-8 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white text-slate-900 transition-all placeholder:text-slate-400"
          />
        </form>
      </div>

      {/* Right: Platform indicators, Notifications, User profile */}
      <div className="flex items-center gap-3">
        {/* Connected Platforms Pills */}
        <div className="hidden sm:flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-50 border border-slate-200 text-xs">
          <Activity className="w-3 h-3 text-slate-400" />
          <div className="flex items-center gap-1.5">
            {connectedAccounts.length > 0 ? (
              connectedAccounts.map(acc => (
                <span
                  key={acc.id}
                  className="flex items-center gap-1"
                  title={`${platformLabel(acc.platform)}: ${acc.handle}`}
                >
                  <span className={cn('w-2 h-2 rounded-full', platformDotClass(acc.platform))} />
                  <span className="text-[11px] font-medium text-slate-600 capitalize">
                    {acc.platform}
                  </span>
                </span>
              ))
            ) : (
              <span className="text-[11px] text-slate-400">No platforms</span>
            )}
          </div>
        </div>

        {/* Notification Bell */}
        <div className="relative">
          <button
            onClick={() => setIsNotifOpen(!isNotifOpen)}
            className="p-2 rounded-lg text-slate-600 hover:bg-slate-100 relative transition-colors"
          >
            <Bell className="w-4 h-4" />
            <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-rose-500 ring-2 ring-white" />
          </button>

          {isNotifOpen && (
            <div className="absolute right-0 mt-2 w-72 bg-white rounded-xl border border-slate-200 shadow-lg p-3 space-y-2 z-50 text-xs">
              <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                <span className="font-bold text-slate-800">Notifications</span>
                <span className="text-[10px] text-indigo-600 font-semibold cursor-pointer">
                  Mark all read
                </span>
              </div>
              <div className="space-y-2">
                <div className="p-2 rounded bg-indigo-50/60 text-indigo-900 space-y-0.5">
                  <p className="font-semibold text-[11px]">Breakout Trend Signal</p>
                  <p className="text-[10px] text-indigo-700">
                    Ollama 3.2 discussions surged by +94% velocity.
                  </p>
                </div>
                <div className="p-2 rounded bg-slate-50 text-slate-700 space-y-0.5">
                  <p className="font-semibold text-[11px]">High Priority Feedback</p>
                  <p className="text-[10px] text-slate-500">
                    4 new questions flagged on React 19 video.
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* User Dropdown */}
        <div className="relative">
          <button
            onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
            className="flex items-center gap-2 p-1 pl-1.5 rounded-lg hover:bg-slate-100 transition-colors"
          >
            <Avatar name={user?.name || 'User'} size="sm" />
            <span className="hidden sm:block text-xs font-semibold text-slate-800">
              {user?.name?.split(' ')[0] || 'User'}
            </span>
            <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
          </button>

          {isUserMenuOpen && (
            <div className="absolute right-0 mt-2 w-52 bg-white rounded-xl border border-slate-200 shadow-lg py-1.5 z-50 text-xs">
              <div className="px-3 py-2 border-b border-slate-100">
                <p className="font-bold text-slate-900">{user?.name || 'User'}</p>
                <p className="text-[10px] text-slate-500 truncate">{user?.email}</p>
                <span className="inline-block mt-1 px-1.5 py-0.5 text-[9px] font-bold rounded bg-indigo-50 text-indigo-700">
                  {user?.role}
                </span>
              </div>

              <div className="py-1">
                <button
                  onClick={() => {
                    setIsUserMenuOpen(false);
                    navigate('/app/settings');
                  }}
                  className="w-full text-left px-3 py-1.5 text-slate-700 hover:bg-slate-50"
                >
                  Workspace Settings
                </button>
                {user?.role === 'ADMIN' && (
                  <button
                    onClick={() => {
                      setIsUserMenuOpen(false);
                      navigate('/admin');
                    }}
                    className="w-full text-left px-3 py-1.5 text-indigo-600 font-semibold hover:bg-indigo-50"
                  >
                    Admin Console
                  </button>
                )}
              </div>

              <div className="border-t border-slate-100 pt-1">
                <button
                  onClick={() => {
                    setIsUserMenuOpen(false);
                    logout();
                    navigate('/login');
                  }}
                  className="w-full text-left px-3 py-1.5 text-rose-600 hover:bg-rose-50 flex items-center gap-1.5"
                >
                  <LogOut className="w-3.5 h-3.5" />
                  Sign Out
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
