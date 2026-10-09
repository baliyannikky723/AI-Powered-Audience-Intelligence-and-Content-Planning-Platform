import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldAlert, ArrowLeft, KeyRound } from 'lucide-react';
import { Button } from '../../components/ui/Button';
import { useAuth } from '../../context/AuthContext';

export const ForbiddenPage: React.FC = () => {
  const { user, switchRole } = useAuth();

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 p-6">
      <div className="max-w-md w-full text-center space-y-6 bg-white p-8 rounded-2xl border border-slate-200 shadow-sm">
        <div className="w-16 h-16 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center mx-auto shadow-inner">
          <ShieldAlert className="w-8 h-8" />
        </div>

        <div className="space-y-2">
          <span className="text-xs font-bold uppercase tracking-widest text-rose-600 bg-rose-50 px-2.5 py-1 rounded-full">
            403 Forbidden
          </span>
          <h1 className="text-2xl font-extrabold text-slate-900">Access Restricted</h1>
          <p className="text-sm text-slate-500 leading-relaxed">
            You don't have administrative privileges to view this section. Your current role is{' '}
            <span className="font-semibold text-slate-800 bg-slate-100 px-2 py-0.5 rounded">
              {user?.role || 'GUEST'}
            </span>.
          </p>
        </div>

        {/* Demo switcher for instant testing */}
        <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 text-left space-y-2">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-700">
            <KeyRound className="w-3.5 h-3.5 text-indigo-600" />
            <span>Developer / Demo Quick-Switch</span>
          </div>
          <p className="text-[11px] text-slate-500">
            Switch your mock session role to test administrator-only screens:
          </p>
          <Button
            variant="secondary"
            size="sm"
            onClick={() => switchRole('ADMIN')}
            className="w-full text-xs font-semibold text-indigo-600 border-indigo-200 bg-white hover:bg-indigo-50"
          >
            Elevate to ADMIN Role
          </Button>
        </div>

        <div className="flex items-center justify-center gap-3 pt-2">
          <Link to="/app/overview">
            <Button variant="primary" size="md">
              <ArrowLeft className="w-4 h-4 mr-2" />
              Return to Overview
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
};
