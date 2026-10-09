import React from 'react';
import { cn } from '../../lib/cn';

// ─── Types ────────────────────────────────────────────────────────────────────
export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'link';
export type ButtonSize    = 'xs' | 'sm' | 'md' | 'lg';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?:    ButtonSize;
  loading?: boolean;
  icon?:    React.ReactNode;
  iconRight?: React.ReactNode;
}

// ─── Spinner ──────────────────────────────────────────────────────────────────
const Spinner = ({ className }: { className?: string }) => (
  <svg
    className={cn('animate-spin', className)}
    viewBox="0 0 24 24"
    fill="none"
    aria-hidden="true"
  >
    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" />
    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
  </svg>
);

// ─── Button ───────────────────────────────────────────────────────────────────
export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant  = 'primary',
      size     = 'md',
      loading  = false,
      disabled,
      className,
      children,
      icon,
      iconRight,
      ...props
    },
    ref
  ) => {
    const base =
      'inline-flex items-center justify-center gap-1.5 font-medium rounded-md border ' +
      'transition-colors duration-150 select-none ' +
      'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-1 ' +
      'disabled:opacity-50 disabled:cursor-not-allowed';

    const variants: Record<ButtonVariant, string> = {
      primary:   'bg-accent text-white border-accent hover:bg-accent-hover active:bg-accent-hover',
      secondary: 'bg-surface text-ink border-border hover:bg-surface-2 active:bg-surface-2',
      ghost:     'bg-transparent text-ink-2 border-transparent hover:bg-surface-2 hover:text-ink active:bg-surface-2',
      danger:    'bg-danger text-white border-danger hover:bg-[#991B1B] active:bg-[#991B1B]',
      link:      'bg-transparent text-accent border-transparent hover:underline underline-offset-2 p-0 h-auto',
    };

    const sizes: Record<ButtonSize, string> = {
      xs: 'h-6  px-2   text-xs',
      sm: 'h-7  px-2.5 text-xs',
      md: 'h-8  px-3.5 text-sm',
      lg: 'h-10 px-4   text-sm',
    };

    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        aria-busy={loading}
        className={cn(base, variants[variant], variant !== 'link' ? sizes[size] : '', className)}
        {...props}
      >
        {loading ? (
          <Spinner className={size === 'xs' || size === 'sm' ? 'h-3 w-3' : 'h-3.5 w-3.5'} />
        ) : (
          icon && <span className="flex-shrink-0">{icon}</span>
        )}
        {children}
        {!loading && iconRight && <span className="flex-shrink-0">{iconRight}</span>}
      </button>
    );
  }
);
Button.displayName = 'Button';

// ─── IconButton ───────────────────────────────────────────────────────────────
export interface IconButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'default' | 'ghost' | 'danger';
  size?:    'sm' | 'md' | 'lg';
  label:    string; // required for a11y
  loading?: boolean;
}

export const IconButton = React.forwardRef<HTMLButtonElement, IconButtonProps>(
  ({ variant = 'ghost', size = 'md', label, loading, disabled, className, children, ...props }, ref) => {
    const base =
      'inline-flex items-center justify-center rounded-md border transition-colors duration-150 flex-shrink-0 ' +
      'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-1 ' +
      'disabled:opacity-50 disabled:cursor-not-allowed';

    const variants: Record<string, string> = {
      default: 'bg-surface text-ink-2 border-border hover:bg-surface-2 hover:text-ink',
      ghost:   'bg-transparent text-ink-2 border-transparent hover:bg-surface-2 hover:text-ink',
      danger:  'bg-transparent text-ink-3 border-transparent hover:bg-danger-soft hover:text-danger',
    };

    const sizes: Record<string, string> = {
      sm: 'h-6 w-6',
      md: 'h-8 w-8',
      lg: 'h-9 w-9',
    };

    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        aria-label={label}
        title={label}
        className={cn(base, variants[variant], sizes[size], className)}
        {...props}
      >
        {loading ? <Spinner className="h-3.5 w-3.5" /> : children}
      </button>
    );
  }
);
IconButton.displayName = 'IconButton';
