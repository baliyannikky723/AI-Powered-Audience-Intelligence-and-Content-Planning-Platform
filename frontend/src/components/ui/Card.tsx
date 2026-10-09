import React from 'react';
import { cn } from '../../lib/cn';

// ─── Card ─────────────────────────────────────────────────────────────────────
export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  padding?: 'sm' | 'md' | 'lg' | 'none';
}

export const Card: React.FC<CardProps> = ({ padding = 'md', className, children, ...props }) => {
  const paddings = { none: '', sm: 'p-3', md: 'p-4', lg: 'p-5' };
  return (
    <div className={cn('panel', paddings[padding], className)} {...props}>
      {children}
    </div>
  );
};

// ─── CardHeader ───────────────────────────────────────────────────────────────
export interface CardHeaderProps extends React.HTMLAttributes<HTMLDivElement> {
  title:    string;
  subtitle?: string;
  action?:  React.ReactNode;
}

export const CardHeader: React.FC<CardHeaderProps> = ({
  title,
  subtitle,
  action,
  className,
  ...props
}) => {
  return (
    <div className={cn('flex items-start justify-between gap-3 mb-4', className)} {...props}>
      <div>
        <h3 className="text-sm font-semibold text-ink leading-snug">{title}</h3>
        {subtitle && <p className="text-xs text-ink-3 mt-0.5">{subtitle}</p>}
      </div>
      {action && <div className="flex-shrink-0">{action}</div>}
    </div>
  );
};

// ─── CardBody ─────────────────────────────────────────────────────────────────
export const CardBody: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className,
  children,
  ...props
}) => (
  <div className={cn('', className)} {...props}>
    {children}
  </div>
);

// ─── CardFooter ───────────────────────────────────────────────────────────────
export const CardFooter: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className,
  children,
  ...props
}) => (
  <div
    className={cn('mt-4 pt-4 border-t border-border flex items-center justify-between gap-3', className)}
    {...props}
  >
    {children}
  </div>
);

// ─── StatCard ─────────────────────────────────────────────────────────────────
export interface StatCardProps {
  label:      string;
  value:      string | number;
  change?:    number;       // positive = up, negative = down, undefined = no badge
  caption?:   string;
  icon?:      React.ReactNode;
  iconColor?: string;       // Tailwind bg class e.g. "bg-accent-soft"
  className?: string;
}

export const StatCard: React.FC<StatCardProps> = ({
  label,
  value,
  change,
  caption,
  icon,
  iconColor = 'bg-accent-soft',
  className,
}) => {
  const isPositive = change !== undefined && change >= 0;
  const isNegative = change !== undefined && change < 0;

  return (
    <div className={cn('panel p-4', className)}>
      {/* Row: label + icon */}
      <div className="flex items-center justify-between mb-3">
        <span className="text-xs font-medium text-ink-3">{label}</span>
        {icon && (
          <div className={cn('h-7 w-7 rounded-md flex items-center justify-center text-accent', iconColor)}>
            {icon}
          </div>
        )}
      </div>

      {/* Value + change */}
      <div className="flex items-baseline gap-2 flex-wrap">
        <span className="text-[26px] font-semibold text-ink leading-none tracking-tight">
          {value}
        </span>
        {change !== undefined && (
          <span
            className={cn(
              'inline-flex items-center gap-0.5 text-xs font-semibold px-1.5 py-0.5 rounded',
              isPositive ? 'bg-success-soft text-success' : '',
              isNegative ? 'bg-danger-soft  text-danger'  : '',
            )}
          >
            {isPositive ? '↑' : '↓'} {Math.abs(change).toFixed(1)}%
          </span>
        )}
      </div>

      {/* Caption */}
      {caption && <p className="mt-2 text-xs text-ink-3">{caption}</p>}
    </div>
  );
};
