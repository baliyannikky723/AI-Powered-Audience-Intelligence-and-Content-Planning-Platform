import React from 'react';
import { cn } from '../../lib/cn';

// ─── Tabs ─────────────────────────────────────────────────────────────────────
export interface TabItem<T extends string = string> {
  id:       T;
  label:    string;
  icon?:    React.ReactNode;
  badge?:   string | number;
  disabled?: boolean;
}

export interface TabsProps<T extends string = string> {
  tabs:       TabItem<T>[];
  activeTab:  T;
  onChange:   (id: T) => void;
  variant?:   'underline' | 'pill';
  className?: string;
}

export function Tabs<T extends string = string>({
  tabs,
  activeTab,
  onChange,
  variant = 'underline',
  className,
}: TabsProps<T>) {
  if (variant === 'pill') {
    return (
      <div
        role="tablist"
        className={cn(
          'inline-flex items-center gap-1 p-1 rounded-lg bg-surface-2 border border-border',
          className
        )}
      >
        {tabs.map(tab => (
          <button
            key={tab.id}
            role="tab"
            aria-selected={activeTab === tab.id}
            disabled={tab.disabled}
            onClick={() => onChange(tab.id)}
            className={cn(
              'inline-flex items-center gap-1.5 h-7 px-3 text-sm font-medium rounded-md transition-colors',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-1',
              'disabled:opacity-40 disabled:cursor-not-allowed',
              activeTab === tab.id
                ? 'bg-surface text-ink shadow-sm border border-border'
                : 'text-ink-2 hover:text-ink'
            )}
          >
            {tab.icon}
            {tab.label}
            {tab.badge !== undefined && (
              <span className="ml-0.5 min-w-[18px] h-[18px] px-1 rounded-full bg-accent text-white text-[10px] font-semibold inline-flex items-center justify-center">
                {tab.badge}
              </span>
            )}
          </button>
        ))}
      </div>
    );
  }

  // Underline variant
  return (
    <div
      role="tablist"
      className={cn('flex items-center border-b border-border gap-0', className)}
    >
      {tabs.map(tab => (
        <button
          key={tab.id}
          role="tab"
          aria-selected={activeTab === tab.id}
          disabled={tab.disabled}
          onClick={() => onChange(tab.id)}
          className={cn(
            'inline-flex items-center gap-1.5 px-3 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-inset',
            'disabled:opacity-40 disabled:cursor-not-allowed',
            activeTab === tab.id
              ? 'border-accent text-accent'
              : 'border-transparent text-ink-2 hover:text-ink hover:border-border-strong'
          )}
        >
          {tab.icon}
          {tab.label}
          {tab.badge !== undefined && (
            <span className={cn(
              'min-w-[18px] h-[18px] px-1 rounded-full text-[10px] font-semibold inline-flex items-center justify-center',
              activeTab === tab.id ? 'bg-accent-soft text-accent-ink' : 'bg-surface-2 text-ink-3'
            )}>
              {tab.badge}
            </span>
          )}
        </button>
      ))}
    </div>
  );
}

// ─── SegmentedControl ─────────────────────────────────────────────────────────
export interface SegmentedControlProps<T extends string = string> {
  options:   { value: T; label: string; icon?: React.ReactNode }[];
  value:     T;
  onChange:  (v: T) => void;
  className?: string;
}

export function SegmentedControl<T extends string = string>({
  options,
  value,
  onChange,
  className,
}: SegmentedControlProps<T>) {
  return (
    <div
      role="group"
      className={cn(
        'inline-flex items-center p-0.5 rounded-md bg-surface-2 border border-border gap-0.5',
        className
      )}
    >
      {options.map(opt => (
        <button
          key={opt.value}
          role="radio"
          aria-checked={value === opt.value}
          onClick={() => onChange(opt.value)}
          className={cn(
            'inline-flex items-center gap-1.5 h-7 px-2.5 text-xs font-medium rounded transition-colors',
            'focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-accent',
            value === opt.value
              ? 'bg-surface text-ink shadow-sm border border-border'
              : 'text-ink-3 hover:text-ink'
          )}
        >
          {opt.icon}
          {opt.label}
        </button>
      ))}
    </div>
  );
}
