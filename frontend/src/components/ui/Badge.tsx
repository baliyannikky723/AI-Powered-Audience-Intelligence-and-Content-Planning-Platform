import React from 'react';
import { cn } from '../../lib/cn';

// ─── Types ────────────────────────────────────────────────────────────────────
export type BadgeVariant =
  | 'default'
  | 'accent'
  | 'success'
  | 'warning'
  | 'danger'
  | 'info'
  | 'outline';

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
  size?:    'xs' | 'sm';
  dot?:     boolean;
}

const variantClasses: Record<BadgeVariant, string> = {
  default: 'bg-surface-2 text-ink-2 border border-border',
  accent:  'bg-accent-soft text-accent-ink border border-accent/20',
  success: 'bg-success-soft text-success border border-success-border',
  warning: 'bg-warning-soft text-warning border border-warning-border',
  danger:  'bg-danger-soft  text-danger  border border-danger-border',
  info:    'bg-info-soft    text-info     border border-info-border',
  outline: 'bg-transparent text-ink-2 border border-border',
};

// ─── Badge ────────────────────────────────────────────────────────────────────
export const Badge: React.FC<BadgeProps> = ({
  variant  = 'default',
  size     = 'sm',
  dot      = false,
  className,
  children,
  ...props
}) => {
  const sizeClass = size === 'xs'
    ? 'px-1.5 py-0 text-[10px] font-semibold leading-5'
    : 'px-2 py-0.5 text-xs font-semibold leading-none';

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full whitespace-nowrap',
        sizeClass,
        variantClasses[variant],
        className
      )}
      {...props}
    >
      {dot && (
        <span
          className={cn(
            'h-1.5 w-1.5 rounded-full flex-shrink-0',
            variant === 'success' ? 'bg-success' :
            variant === 'danger'  ? 'bg-danger'  :
            variant === 'warning' ? 'bg-warning' :
            variant === 'info'    ? 'bg-info'    :
            variant === 'accent'  ? 'bg-accent'  :
            'bg-ink-3'
          )}
        />
      )}
      {children}
    </span>
  );
};

// ─── SentimentBadge ───────────────────────────────────────────────────────────
type SentimentType = 'positive' | 'neutral' | 'negative';

export const SentimentBadge: React.FC<{ sentiment: SentimentType; className?: string }> = ({
  sentiment,
  className,
}) => {
  const config: Record<SentimentType, { label: string; variant: BadgeVariant }> = {
    positive: { label: 'Positive', variant: 'success' },
    neutral:  { label: 'Neutral',  variant: 'default' },
    negative: { label: 'Negative', variant: 'danger'  },
  };
  const { label, variant } = config[sentiment];
  return <Badge variant={variant} dot className={className}>{label}</Badge>;
};

// ─── PriorityBadge ────────────────────────────────────────────────────────────
type PriorityType = 'high' | 'medium' | 'low';

export const PriorityBadge: React.FC<{ priority: PriorityType; className?: string }> = ({
  priority,
  className,
}) => {
  const config: Record<PriorityType, { label: string; variant: BadgeVariant }> = {
    high:   { label: 'High priority', variant: 'danger'  },
    medium: { label: 'Medium',        variant: 'warning' },
    low:    { label: 'Low',           variant: 'default' },
  };
  const { label, variant } = config[priority];
  return <Badge variant={variant} size="xs" className={cn('uppercase tracking-wide', className)}>{label}</Badge>;
};

// ─── StatusBadge ─────────────────────────────────────────────────────────────
type StatusType = 'connected' | 'disconnected' | 'error' | 'syncing';

export const StatusBadge: React.FC<{ status: StatusType; className?: string }> = ({
  status,
  className,
}) => {
  const config: Record<StatusType, { label: string; variant: BadgeVariant }> = {
    connected:    { label: 'Connected',    variant: 'success' },
    disconnected: { label: 'Disconnected', variant: 'default' },
    error:        { label: 'Error',        variant: 'danger'  },
    syncing:      { label: 'Syncing',      variant: 'info'    },
  };
  const { label, variant } = config[status] ?? config.disconnected;
  return <Badge variant={variant} dot className={className}>{label}</Badge>;
};
