import React from 'react';
import { cn } from '../../lib/cn';

// ─── ProgressBar ─────────────────────────────────────────────────────────────
export interface ProgressBarProps {
  value:      number;     // 0–100
  max?:       number;
  label?:     string;
  showValue?: boolean;
  color?:     'accent' | 'success' | 'warning' | 'danger';
  size?:      'xs' | 'sm' | 'md';
  className?: string;
}

const colorMap: Record<string, string> = {
  accent:  'bg-accent',
  success: 'bg-success',
  warning: 'bg-warning',
  danger:  'bg-danger',
};

const heightMap: Record<string, string> = {
  xs: 'h-1',
  sm: 'h-1.5',
  md: 'h-2',
};

export const ProgressBar: React.FC<ProgressBarProps> = ({
  value,
  max        = 100,
  label,
  showValue  = false,
  color      = 'accent',
  size       = 'sm',
  className,
}) => {
  const pct = Math.min(100, Math.max(0, (value / max) * 100));
  return (
    <div className={cn('w-full', className)}>
      {(label || showValue) && (
        <div className="mb-1 flex items-center justify-between text-xs text-ink-3">
          {label && <span>{label}</span>}
          {showValue && <span className="font-medium text-ink">{Math.round(pct)}%</span>}
        </div>
      )}
      <div className={cn('w-full rounded-full bg-border', heightMap[size])}>
        <div
          className={cn('h-full rounded-full transition-all duration-500', colorMap[color])}
          style={{ width: `${pct}%` }}
          role="progressbar"
          aria-valuenow={value}
          aria-valuemin={0}
          aria-valuemax={max}
          aria-label={label}
        />
      </div>
    </div>
  );
};
