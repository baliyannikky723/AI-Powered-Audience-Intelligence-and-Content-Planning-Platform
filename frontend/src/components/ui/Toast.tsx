import React, { createContext, useCallback, useContext, useState } from 'react';
import { cn } from '../../lib/cn';
import { X, CheckCircle, AlertCircle, AlertTriangle, Info } from 'lucide-react';

// ─── Types ────────────────────────────────────────────────────────────────────
export type ToastVariant = 'success' | 'danger' | 'warning' | 'info' | 'default';

export interface ToastData {
  id:       string;
  title:    string;
  body?:    string;
  variant?: ToastVariant;
  duration?: number; // ms, default 4000
}

// ─── Context ──────────────────────────────────────────────────────────────────
interface ToastContextValue {
  addToast: (toast: Omit<ToastData, 'id'>) => void;
}

const ToastContext = createContext<ToastContextValue>({ addToast: () => {} });

export function useToast() {
  return useContext(ToastContext);
}

// ─── Single Toast ─────────────────────────────────────────────────────────────
const ToastItem: React.FC<{ toast: ToastData; onRemove: (id: string) => void }> = ({
  toast,
  onRemove,
}) => {
  const icons: Record<ToastVariant, React.ReactNode> = {
    success: <CheckCircle   size={16} className="text-success flex-shrink-0" />,
    danger:  <AlertCircle   size={16} className="text-danger  flex-shrink-0" />,
    warning: <AlertTriangle size={16} className="text-warning flex-shrink-0" />,
    info:    <Info          size={16} className="text-info    flex-shrink-0" />,
    default: null,
  };

  const variant = toast.variant ?? 'default';

  React.useEffect(() => {
    const t = setTimeout(() => onRemove(toast.id), toast.duration ?? 4000);
    return () => clearTimeout(t);
  }, [toast, onRemove]);

  return (
    <div
      role="alert"
      aria-live="polite"
      className={cn(
        'flex items-start gap-3 w-80 rounded-lg border p-4 shadow-md bg-surface animate-slide-up',
        variant === 'success' ? 'border-success-border' :
        variant === 'danger'  ? 'border-danger-border'  :
        variant === 'warning' ? 'border-warning-border' :
        variant === 'info'    ? 'border-info-border'    :
        'border-border'
      )}
    >
      {icons[variant]}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-ink">{toast.title}</p>
        {toast.body && <p className="text-xs text-ink-3 mt-0.5">{toast.body}</p>}
      </div>
      <button
        onClick={() => onRemove(toast.id)}
        aria-label="Dismiss notification"
        className="flex-shrink-0 h-5 w-5 rounded flex items-center justify-center text-ink-3 hover:text-ink transition-colors"
      >
        <X size={14} />
      </button>
    </div>
  );
};

// ─── ToastProvider ────────────────────────────────────────────────────────────
export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<ToastData[]>([]);

  const addToast = useCallback((toast: Omit<ToastData, 'id'>) => {
    setToasts(prev => [...prev, { ...toast, id: `toast-${Date.now()}-${Math.random()}` }]);
  }, []);

  const removeToast = useCallback((id: string) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  return (
    <ToastContext.Provider value={{ addToast }}>
      {children}
      {/* Portal-style fixed container */}
      <div
        aria-label="Notifications"
        className="fixed bottom-5 right-5 z-50 flex flex-col gap-2 pointer-events-none"
      >
        {toasts.map(t => (
          <div key={t.id} className="pointer-events-auto">
            <ToastItem toast={t} onRemove={removeToast} />
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
};
