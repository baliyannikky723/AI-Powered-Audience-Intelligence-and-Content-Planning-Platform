import React, { useEffect, useRef } from 'react';
import { cn } from '../../lib/cn';
import { X } from 'lucide-react';

export interface ModalProps {
  open:        boolean;
  onClose:     () => void;
  title?:      string;
  description?: string;
  children:    React.ReactNode;
  footer?:     React.ReactNode;
  size?:       'sm' | 'md' | 'lg' | 'xl';
  className?:  string;
}

const sizeMap: Record<string, string> = {
  sm: 'max-w-sm',
  md: 'max-w-md',
  lg: 'max-w-lg',
  xl: 'max-w-2xl',
};

export const Modal: React.FC<ModalProps> = ({
  open,
  onClose,
  title,
  description,
  children,
  footer,
  size = 'md',
  className,
}) => {
  const overlayRef = useRef<HTMLDivElement>(null);
  const firstFocusRef = useRef<HTMLButtonElement>(null);

  // Trap focus + ESC
  useEffect(() => {
    if (!open) return;
    const prev = document.activeElement as HTMLElement;
    firstFocusRef.current?.focus();

    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKey);
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', handleKey);
      document.body.style.overflow = '';
      prev?.focus();
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      ref={overlayRef}
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby={title ? 'modal-title' : undefined}
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-ink/40 animate-fade-in"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Panel */}
      <div
        className={cn(
          'relative w-full rounded-xl bg-surface shadow-xl border border-border animate-scale-in overflow-hidden',
          sizeMap[size],
          className
        )}
      >
        {/* Header */}
        {title && (
          <div className="flex items-start justify-between gap-3 px-5 py-4 border-b border-border">
            <div>
              <h2 id="modal-title" className="text-base font-semibold text-ink">{title}</h2>
              {description && (
                <p className="mt-0.5 text-sm text-ink-3">{description}</p>
              )}
            </div>
            <button
              ref={firstFocusRef}
              onClick={onClose}
              aria-label="Close modal"
              className="flex-shrink-0 h-7 w-7 rounded-md flex items-center justify-center text-ink-3 hover:bg-surface-2 hover:text-ink transition-colors"
            >
              <X size={16} />
            </button>
          </div>
        )}
        {!title && (
          <button
            ref={firstFocusRef}
            onClick={onClose}
            aria-label="Close modal"
            className="absolute top-3 right-3 h-7 w-7 rounded-md flex items-center justify-center text-ink-3 hover:bg-surface-2 hover:text-ink transition-colors z-10"
          >
            <X size={16} />
          </button>
        )}

        {/* Body */}
        <div className="px-5 py-5">{children}</div>

        {/* Footer */}
        {footer && (
          <div className="px-5 py-4 border-t border-border bg-surface-2 flex justify-end gap-2">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
};

// ─── ConfirmDialog ─────────────────────────────────────────────────────────────
export interface ConfirmDialogProps {
  open:         boolean;
  onClose:      () => void;
  onConfirm:    () => void;
  title:        string;
  body?:        string;
  confirmLabel?: string;
  cancelLabel?:  string;
  variant?:      'danger' | 'primary';
  loading?:      boolean;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  open,
  onClose,
  onConfirm,
  title,
  body,
  confirmLabel = 'Confirm',
  cancelLabel  = 'Cancel',
  variant      = 'danger',
  loading      = false,
}) => (
  <Modal open={open} onClose={onClose} title={title} size="sm">
    {body && <p className="text-sm text-ink-3 leading-relaxed">{body}</p>}
    <div className="mt-5 flex justify-end gap-2">
      <button
        onClick={onClose}
        disabled={loading}
        className="h-8 px-3.5 text-sm font-medium rounded-md border bg-surface text-ink border-border hover:bg-surface-2 transition-colors disabled:opacity-50"
      >
        {cancelLabel}
      </button>
      <button
        onClick={onConfirm}
        disabled={loading}
        className={cn(
          'h-8 px-3.5 text-sm font-medium rounded-md border transition-colors disabled:opacity-50',
          variant === 'danger'
            ? 'bg-danger text-white border-danger hover:bg-[#991B1B]'
            : 'bg-accent text-white border-accent hover:bg-accent-hover'
        )}
      >
        {loading ? 'Processing…' : confirmLabel}
      </button>
    </div>
  </Modal>
);
