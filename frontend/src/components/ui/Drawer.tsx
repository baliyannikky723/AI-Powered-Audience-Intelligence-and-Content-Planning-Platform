import React, { useEffect, useRef } from 'react';
import { cn } from '../../lib/cn';
import { X } from 'lucide-react';

export interface DrawerProps {
  open:       boolean;
  onClose:    () => void;
  title?:     string;
  children:   React.ReactNode;
  footer?:    React.ReactNode;
  side?:      'left' | 'right';
  width?:     string;
  className?: string;
}

export const Drawer: React.FC<DrawerProps> = ({
  open,
  onClose,
  title,
  children,
  footer,
  side    = 'left',
  width   = '280px',
  className,
}) => {
  const closeRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) return;
    const prev = document.activeElement as HTMLElement;
    // Small delay to allow animation before focusing
    const t = setTimeout(() => closeRef.current?.focus(), 60);

    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKey);
    document.body.style.overflow = 'hidden';

    return () => {
      clearTimeout(t);
      document.removeEventListener('keydown', handleKey);
      document.body.style.overflow = '';
      prev?.focus();
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50" role="dialog" aria-modal="true" aria-label={title ?? 'Drawer'}>
      {/* Scrim */}
      <div
        className="absolute inset-0 bg-ink/30 animate-fade-in"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Drawer panel */}
      <div
        className={cn(
          'absolute top-0 bottom-0 flex flex-col bg-surface border-border shadow-xl',
          side === 'left'  ? 'left-0 border-r animate-slide-in-left' : 'right-0 border-l',
          className
        )}
        style={{ width }}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-border flex-shrink-0">
          {title ? (
            <h2 className="text-base font-semibold text-ink">{title}</h2>
          ) : (
            <span />
          )}
          <button
            ref={closeRef}
            onClick={onClose}
            aria-label="Close drawer"
            className="h-7 w-7 rounded-md flex items-center justify-center text-ink-3 hover:bg-surface-2 hover:text-ink transition-colors"
          >
            <X size={16} />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto px-4 py-4">{children}</div>

        {/* Footer */}
        {footer && (
          <div className="flex-shrink-0 px-4 py-3 border-t border-border bg-surface-2 flex gap-2 justify-end">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
};
