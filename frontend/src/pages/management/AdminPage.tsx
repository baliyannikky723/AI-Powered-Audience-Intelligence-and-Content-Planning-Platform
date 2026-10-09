import React from 'react';
import { useAdminStats, useAdminUsers, useUpdateUserRoleMutation, useUpdateUserStatusMutation } from '../../hooks/useApi';
import { StatCard } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Avatar } from '../../components/ui/Avatar';
import { formatCompact } from '../../lib/format';
import {
  Shield, Users, Activity, Server, Zap,
} from 'lucide-react';
import { useToast } from '../../components/ui/Toast';

export const AdminPage: React.FC = () => {
  const { data: stats, isLoading: statsLoading } = useAdminStats();
  const { data: users } = useAdminUsers();
  const updateRole = useUpdateUserRoleMutation();
  const updateStatus = useUpdateUserStatusMutation();
  const { addToast } = useToast();

  const handleToggleRole = (userId: string, currentRole: 'USER' | 'ADMIN') => {
    const newRole = currentRole === 'ADMIN' ? 'USER' : 'ADMIN';
    updateRole.mutate(
      { userId, role: newRole },
      {
        onSuccess: () => {
          addToast({
            title: 'Role Updated',
            body: `User role changed to ${newRole}`,
            variant: 'success',
          });
        },
      }
    );
  };

  const handleToggleStatus = (userId: string, currentStatus: string) => {
    const newStatus: 'active' | 'suspended' = currentStatus === 'active' ? 'suspended' : 'active';
    updateStatus.mutate(
      { userId, status: newStatus },
      {
        onSuccess: () => {
          addToast({
            title: 'User Status Updated',
            body: `Account is now ${newStatus}`,
            variant: newStatus === 'active' ? 'success' : 'warning',
          });
        },
      }
    );
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs font-bold uppercase tracking-wider text-rose-600 bg-rose-50 px-2 py-0.5 rounded-full border border-rose-200">
              Admin Exclusive
            </span>
          </div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Shield className="w-5 h-5 text-indigo-600" />
            System Administration & User Management
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Global telemetry, token quotas, server load, and RBAC user permissions.
          </p>
        </div>
      </div>

      {/* Admin Stats */}
      {statsLoading || !stats ? (
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="h-24 bg-white rounded-xl border border-slate-200 animate-pulse" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard
            label="Total Registered Users"
            value={formatCompact(stats.totalUsers)}
            change={18.4}
            caption={`${stats.activeToday} active today`}
            icon={<Users className="w-4 h-4 text-accent" />}
          />
          <StatCard
            label="Total Token Consumption"
            value={formatCompact(stats.totalTokensUsed)}
            caption="GPT-4o & Embeddings"
            icon={<Zap className="w-4 h-4 text-warning" />}
          />
          <StatCard
            label="API Requests (24h)"
            value={formatCompact(stats.apiRequests24h)}
            caption={`${stats.avgResponseTimeMs}ms avg latency`}
            icon={<Activity className="w-4 h-4 text-success" />}
          />
          <StatCard
            label="Cluster Health"
            value={stats.serverHealth.toUpperCase()}
            caption="99.98% Uptime SLA"
            icon={<Server className="w-4 h-4 text-accent" />}
          />
        </div>
      )}

      {/* User Management Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h3 className="text-sm font-bold text-slate-900">User Accounts & Roles</h3>
            <p className="text-xs text-slate-500">Manage access levels and inspect tenant token usage</p>
          </div>
          <Badge variant="accent" size="sm">
            {users?.length || 0} Accounts
          </Badge>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold uppercase tracking-wider text-[10px]">
                <th className="p-3.5 pl-5">User</th>
                <th className="p-3.5">Role</th>
                <th className="p-3.5">Status</th>
                <th className="p-3.5">Tokens Used</th>
                <th className="p-3.5">Accounts</th>
                <th className="p-3.5 pr-5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {users?.map(user => (
                <tr key={user.id} className="hover:bg-slate-50/50 transition-colors">
                  <td className="p-3.5 pl-5">
                    <div className="flex items-center gap-3">
                      <Avatar name={user.name} size="sm" />
                      <div>
                        <div className="font-bold text-slate-900">{user.name}</div>
                        <div className="text-slate-500 text-[11px]">{user.email}</div>
                      </div>
                    </div>
                  </td>
                  <td className="p-3.5">
                    <Badge variant={user.role === 'ADMIN' ? 'accent' : 'default'} size="xs">
                      {user.role}
                    </Badge>
                  </td>
                  <td className="p-3.5">
                    <span
                      className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[11px] font-semibold ${
                        user.status === 'active'
                          ? 'bg-emerald-50 text-emerald-700'
                          : 'bg-rose-50 text-rose-700'
                      }`}
                    >
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${
                          user.status === 'active' ? 'bg-emerald-500' : 'bg-rose-500'
                        }`}
                      />
                      {user.status}
                    </span>
                  </td>
                  <td className="p-3.5 font-medium text-slate-700">
                    {formatCompact(user.tokenUsage)} tokens
                  </td>
                  <td className="p-3.5 text-slate-600 font-medium">
                    {user.accountsConnected} connected
                  </td>
                  <td className="p-3.5 pr-5 text-right space-x-2">
                    <Button
                      variant="secondary"
                      size="xs"
                      onClick={() => handleToggleRole(user.id, user.role)}
                      className="text-[11px]"
                    >
                      {user.role === 'ADMIN' ? 'Demote to USER' : 'Promote to ADMIN'}
                    </Button>
                    <Button
                      variant={user.status === 'active' ? 'danger' : 'secondary'}
                      size="xs"
                      onClick={() => handleToggleStatus(user.id, user.status)}
                      className="text-[11px]"
                    >
                      {user.status === 'active' ? 'Suspend' : 'Activate'}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
