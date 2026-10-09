import React from 'react';
import { cn } from '../../lib/cn';

// ─── Skeleton base ────────────────────────────────────────────────────────────
export interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {
  height?: string | number;
  width?:  string | number;
  rounded?: 'sm' | 'md' | 'lg' | 'full';
}

export const Skeleton: React.FC<SkeletonProps> = ({
  height,
  width,
  rounded = 'md',
  className,
  style,
  ...props
}) => {
  const roundedMap = { sm: 'rounded', md: 'rounded-md', lg: 'rounded-lg', full: 'rounded-full' };
  return (
    <div
      role="status"
      aria-label="Loading"
      className={cn(
        'bg-border animate-pulse',
        roundedMap[rounded],
        className
      )}
      style={{
        height: typeof height === 'number' ? `${height}px` : height,
        width:  typeof width  === 'number' ? `${width}px`  : width,
        ...style,
      }}
      {...props}
    />
  );
};

// ─── SkeletonText ─────────────────────────────────────────────────────────────
export const SkeletonText: React.FC<{ lines?: number; className?: string }> = ({
  lines = 3,
  className,
}) => (
  <div className={cn('space-y-2', className)} role="status" aria-label="Loading">
    {Array.from({ length: lines }).map((_, i) => (
      <Skeleton
        key={i}
        height={14}
        width={i === lines - 1 ? '60%' : '100%'}
        rounded="sm"
      />
    ))}
  </div>
);

// ─── SkeletonCard ─────────────────────────────────────────────────────────────
export const SkeletonCard: React.FC<{ className?: string }> = ({ className }) => (
  <div className={cn('panel p-4 space-y-3', className)} role="status" aria-label="Loading">
    <div className="flex items-center justify-between">
      <Skeleton height={12} width="40%" rounded="sm" />
      <Skeleton height={28} width={28} rounded="md" />
    </div>
    <Skeleton height={32} width="55%" rounded="md" />
    <Skeleton height={12} width="70%" rounded="sm" />
  </div>
);

// ─── SkeletonRow ──────────────────────────────────────────────────────────────
export const SkeletonRow: React.FC<{ cols?: number; className?: string }> = ({
  cols = 4,
  className,
}) => (
  <div
    className={cn('flex items-center gap-3 py-3 px-4', className)}
    role="status"
    aria-label="Loading"
  >
    <Skeleton height={32} width={32} rounded="full" />
    <div className="flex-1 space-y-1.5">
      <Skeleton height={12} width="50%" rounded="sm" />
      <Skeleton height={10} width="35%" rounded="sm" />
    </div>
    {Array.from({ length: cols - 1 }).map((_, i) => (
      <Skeleton key={i} height={12} width={60} rounded="sm" />
    ))}
  </div>
);
