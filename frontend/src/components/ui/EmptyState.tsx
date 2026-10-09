import React from 'react';
import { cn } from '../../lib/cn';

// ─── EmptyState ───────────────────────────────────────────────────────────────
export interface EmptyStateProps {
  icon?:     React.ReactNode;
  title:     string;
  body?:     string;
  action?:   React.ReactNode;
  className?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  body,
  action,
  className,
}) => (
  <div
    className={cn(
      'flex flex-col items-center justify-center text-center py-16 px-6',
      className
    )}
    role="status"
  >
    {icon && (
      <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-surface-2 border border-border text-ink-3">
        {icon}
      </div>
    )}
    <h3 className="text-sm font-semibold text-ink mb-1">{title}</h3>
    {body && <p className="text-sm text-ink-3 max-w-xs leading-relaxed">{body}</p>}
    {action && <div className="mt-4">{action}</div>}
  </div>
);

// ─── ErrorState ───────────────────────────────────────────────────────────────
export interface ErrorStateProps {
  title?:    string;
  body?:     string;
  onRetry?:  () => void;
  className?: string;
}

const ErrorIcon = () => (
  <svg className="h-6 w-6 text-danger" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="1.5"/>
    <path d="M12 8v4M12 16h.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
  </svg>
);

export const ErrorState: React.FC<ErrorStateProps> = ({
  title   = 'Something went wrong',
  body    = "We couldn't load this data. Please try again.",
  onRetry,
  className,
}) => (
  <div
    className={cn(
      'flex flex-col items-center justify-center text-center py-16 px-6',
      className
    )}
    role="alert"
  >
    <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-danger-soft border border-danger-border">
      <ErrorIcon />
    </div>
    <h3 className="text-sm font-semibold text-ink mb-1">{title}</h3>
    <p className="text-sm text-ink-3 max-w-xs leading-relaxed">{body}</p>
    {onRetry && (
      <button
        onClick={onRetry}
        className="mt-4 h-8 px-3.5 text-sm font-medium rounded-md border bg-surface text-ink border-border hover:bg-surface-2 transition-colors"
      >
        Try again
      </button>
    )}
  </div>
);
