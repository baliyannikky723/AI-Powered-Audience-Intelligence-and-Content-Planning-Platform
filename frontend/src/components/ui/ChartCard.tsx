import React from 'react';
import { cn } from '../../lib/cn';
import { CardHeader } from './Card';
import { Skeleton } from './Skeleton';
import { EmptyState } from './EmptyState';

// ─── ChartCard ────────────────────────────────────────────────────────────────
export interface ChartCardProps {
  title:     string;
  subtitle?: string;
  action?:   React.ReactNode;
  height?:   number;
  loading?:  boolean;
  empty?:    boolean;
  emptyText?: string;
  children:  React.ReactNode;
  className?: string;
}

export const ChartCard: React.FC<ChartCardProps> = ({
  title,
  subtitle,
  action,
  height  = 240,
  loading = false,
  empty   = false,
  emptyText = 'No data available. Connect a platform to start tracking.',
  children,
  className,
}) => {
  return (
    <div className={cn('panel p-4', className)}>
      <CardHeader title={title} subtitle={subtitle} action={action} />

      <div style={{ height }} className="w-full">
        {loading ? (
          <div className="h-full flex flex-col justify-end gap-2 pb-2">
            {/* Fake bar chart skeleton */}
            <div className="flex items-end gap-2 h-full px-4">
              {[40, 70, 55, 85, 65, 90].map((h, i) => (
                <Skeleton key={i} className="flex-1" style={{ height: `${h}%` }} rounded="sm" />
              ))}
            </div>
          </div>
        ) : empty ? (
          <EmptyState
            title="No data yet"
            body={emptyText}
            className="h-full py-8"
          />
        ) : (
          children
        )}
      </div>
    </div>
  );
};
