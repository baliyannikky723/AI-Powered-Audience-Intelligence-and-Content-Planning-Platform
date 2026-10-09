import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { cn } from '../lib/cn';
import { useAuth } from '../context/AuthContext';
import {
  LayoutDashboard,
  MessageSquare,
  BrainCircuit,
  Calendar,
  Share2,
  ChevronLeft,
  ChevronRight,
  Activity,
  Users,
  Sparkles,
  Settings,
  ShieldCheck,
  Flame,
  Brain,
  Lightbulb,
  CheckCircle2,
  Lock,
  LogOut,
  UserCheck,
  FlaskConical,
} from 'lucide-react';
import { Avatar } from './ui/Avatar';

interface NavItem {
  path: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  badge?: string;
  adminOnly?: boolean;
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

const NAV_GROUPS: NavGroup[] = [
  {
    label: 'Core Workflow',
    items: [
      { path: '/app/overview', label: 'Overview', icon: LayoutDashboard },
      { path: '/app/comments', label: 'Comments', icon: MessageSquare, badge: 'Live' },
      { path: '/app/chat', label: 'Ask PulseGPT', icon: BrainCircuit },
      { path: '/app/planner', label: 'Planner Kanban', icon: Sparkles },
      { path: '/app/production', label: 'Production Copilot', icon: Lightbulb, badge: 'Phase 3K' },
      { path: '/app/calendar', label: 'Calendar', icon: Calendar },
    ],
  },
  {
    label: 'Audience Intelligence',
    items: [
      { path: '/app/audience', label: 'Audience Cohorts', icon: Users },
      { path: '/app/trends', label: 'Trends Radar', icon: Flame, badge: 'Hot' },
      { path: '/app/memory', label: 'Audience Memory', icon: Brain },
      { path: '/app/recommendations', label: 'AI Post Ideas', icon: Sparkles },
      { path: '/app/evaluation', label: 'LLM Guardrails', icon: CheckCircle2 },
      { path: '/app/research', label: 'Research Lab', icon: FlaskConical, badge: 'Phase 3M' },
    ],

  },
  {
    label: 'Management',
    items: [
      { path: '/app/integrations', label: 'Integrations', icon: Share2 },
      { path: '/app/settings', label: 'Settings', icon: Settings },
      { path: '/admin', label: 'Administration', icon: ShieldCheck, adminOnly: true },
    ],
  },
];

interface SidebarProps {
  collapsed?: boolean;
  onToggleCollapse?: () => void;
  className?: string;
  onItemClick?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  collapsed = false,
  onToggleCollapse,
  className,
  onItemClick,
}) => {
  const { user, logout, switchRole } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <aside
      className={cn(
        'relative flex flex-col bg-white border-r border-slate-200 transition-all duration-300 ease-in-out select-none h-screen sticky top-0 z-30',
        collapsed ? 'w-16' : 'w-60',
        className
      )}
    >
      {/* Brand Header */}
      <div className="flex items-center justify-between h-14 px-4 border-b border-slate-100 flex-shrink-0">
        <NavLink to="/app/overview" className="flex items-center gap-2.5 overflow-hidden">
          <div className="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center text-white flex-shrink-0 shadow-sm">
            <Activity className="w-4 h-4" />
          </div>
          {!collapsed && (
            <div className="flex flex-col">
              <span className="font-bold text-sm tracking-tight text-slate-900 leading-none">
                Pulse<span className="text-indigo-600">GPT</span>
              </span>
              <span className="text-[10px] text-slate-400 font-medium tracking-wide uppercase mt-0.5">
                v2.0 Platform
              </span>
            </div>
          )}
        </NavLink>

        {onToggleCollapse && (
          <button
            onClick={onToggleCollapse}
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            className={cn(
              'p-1.5 rounded-md text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors',
              collapsed && 'hidden'
            )}
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
        )}
      </div>

      {/* Navigation Groups */}
      <div className="flex-1 overflow-y-auto py-3 px-2 space-y-4 scrollbar-thin">
        {NAV_GROUPS.map(group => (
          <div key={group.label} className="space-y-1">
            {!collapsed && (
              <p className="px-2.5 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                {group.label}
              </p>
            )}
            {group.items.map(item => {
              const Icon = item.icon;
              const isLocked = item.adminOnly && user?.role !== 'ADMIN';

              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  onClick={onItemClick}
                  title={collapsed ? item.label : undefined}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-2.5 px-2.5 py-2 rounded-lg text-xs font-medium transition-colors group relative',
                      isActive
                        ? 'bg-indigo-50 text-indigo-700 font-semibold'
                        : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900',
                      collapsed && 'justify-center px-0'
                    )
                  }
                >
                  <Icon className={cn('w-4 h-4 flex-shrink-0', isLocked && 'text-slate-400')} />
                  {!collapsed && (
                    <div className="flex-1 flex items-center justify-between min-w-0">
                      <span className="truncate">{item.label}</span>
                      {item.badge && (
                        <span className="px-1.5 py-0.5 text-[9px] font-bold rounded-full bg-indigo-100 text-indigo-700">
                          {item.badge}
                        </span>
                      )}
                      {isLocked && (
                        <span className="text-[10px] text-amber-600 font-bold flex items-center gap-1">
                          <Lock className="w-3 h-3" /> Admin
                        </span>
                      )}
                    </div>
                  )}
                </NavLink>
              );
            })}
          </div>
        ))}
      </div>

      {/* User Profile & Role Switch Widget Footer */}
      <div className="p-2 border-t border-slate-100 flex-shrink-0 bg-slate-50/50">
        {!collapsed ? (
          <div className="space-y-2">
            <div className="flex items-center justify-between p-2 rounded-lg bg-white border border-slate-200">
              <div className="flex items-center gap-2 min-w-0">
                <Avatar name={user?.name || 'Creator'} size="sm" />
                <div className="min-w-0">
                  <p className="text-xs font-bold text-slate-900 truncate">
                    {user?.name || 'Creator'}
                  </p>
                  <p className="text-[10px] text-slate-500 truncate">
                    Role: <strong className="text-indigo-600">{user?.role || 'USER'}</strong>
                  </p>
                </div>
              </div>

              <button
                onClick={handleLogout}
                title="Log Out"
                className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-md transition-colors"
              >
                <LogOut className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* Quick Demo Role Switcher button */}
            <button
              onClick={() => switchRole(user?.role === 'ADMIN' ? 'USER' : 'ADMIN')}
              className="w-full py-1 text-[10px] font-semibold text-slate-500 hover:text-indigo-600 hover:bg-indigo-50/50 rounded flex items-center justify-center gap-1 transition-colors"
            >
              <UserCheck className="w-3 h-3" />
              Switch to {user?.role === 'ADMIN' ? 'USER' : 'ADMIN'} Role
            </button>
          </div>
        ) : (
          <div className="flex flex-col items-center gap-2 py-1">
            <button onClick={handleLogout} title="Log Out" className="p-2 text-slate-400 hover:text-rose-600">
              <LogOut className="w-4 h-4" />
            </button>
            {onToggleCollapse && (
              <button
                onClick={onToggleCollapse}
                className="p-1 text-slate-400 hover:text-slate-600 hover:bg-slate-200 rounded"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            )}
          </div>
        )}
      </div>
    </aside>
  );
};
