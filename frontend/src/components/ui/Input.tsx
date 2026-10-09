import React from 'react';
import { cn } from '../../lib/cn';

// ─── Input ────────────────────────────────────────────────────────────────────
export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  leftElement?:  React.ReactNode;
  rightElement?: React.ReactNode;
  error?:        string;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ leftElement, rightElement, error, className, id, ...props }, ref) => {
    return (
      <div className="relative w-full">
        {leftElement && (
          <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-ink-3">
            {leftElement}
          </div>
        )}
        <input
          ref={ref}
          id={id}
          aria-invalid={!!error}
          className={cn(
            'form-input w-full py-2',
            leftElement  ? 'pl-9'  : 'pl-3',
            rightElement ? 'pr-9'  : 'pr-3',
            error ? 'border-danger focus:border-danger focus:ring-danger/10' : '',
            className
          )}
          {...props}
        />
        {rightElement && (
          <div className="absolute inset-y-0 right-0 flex items-center pr-3 text-ink-3">
            {rightElement}
          </div>
        )}
        {error && (
          <p className="mt-1 text-xs text-danger" role="alert">{error}</p>
        )}
      </div>
    );
  }
);
Input.displayName = 'Input';

// ─── Textarea ─────────────────────────────────────────────────────────────────
export interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  error?: string;
}

export const Textarea = React.forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ error, className, ...props }, ref) => {
    return (
      <div className="w-full">
        <textarea
          ref={ref}
          aria-invalid={!!error}
          className={cn(
            'form-input w-full px-3 py-2 resize-none',
            error ? 'border-danger focus:border-danger' : '',
            className
          )}
          {...props}
        />
        {error && (
          <p className="mt-1 text-xs text-danger" role="alert">{error}</p>
        )}
      </div>
    );
  }
);
Textarea.displayName = 'Textarea';

// ─── Select ───────────────────────────────────────────────────────────────────
export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  error?: string;
}

export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ error, className, children, ...props }, ref) => {
    return (
      <div className="relative w-full">
        <select
          ref={ref}
          aria-invalid={!!error}
          className={cn(
            'form-input w-full py-2 pl-3 pr-8 appearance-none cursor-pointer',
            error ? 'border-danger' : '',
            className
          )}
          {...props}
        >
          {children}
        </select>
        {/* Chevron */}
        <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2.5 text-ink-3">
          <svg className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
            <path fillRule="evenodd" d="M5.22 8.22a.75.75 0 0 1 1.06 0L10 11.94l3.72-3.72a.75.75 0 1 1 1.06 1.06l-4.25 4.25a.75.75 0 0 1-1.06 0L5.22 9.28a.75.75 0 0 1 0-1.06Z" clipRule="evenodd"/>
          </svg>
        </div>
        {error && (
          <p className="mt-1 text-xs text-danger" role="alert">{error}</p>
        )}
      </div>
    );
  }
);
Select.displayName = 'Select';

// ─── FormField ────────────────────────────────────────────────────────────────
export interface FormFieldProps {
  label?:    string;
  htmlFor?:  string;
  hint?:     string;
  error?:    string;
  required?: boolean;
  children:  React.ReactNode;
  className?: string;
}

export const FormField: React.FC<FormFieldProps> = ({
  label,
  htmlFor,
  hint,
  error,
  required,
  children,
  className,
}) => {
  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      {label && (
        <label
          htmlFor={htmlFor}
          className="text-xs font-semibold text-ink-2 uppercase tracking-wide"
        >
          {label}
          {required && <span className="ml-0.5 text-danger">*</span>}
        </label>
      )}
      {children}
      {hint && !error && (
        <p className="text-xs text-ink-3">{hint}</p>
      )}
      {error && (
        <p className="text-xs text-danger" role="alert">{error}</p>
      )}
    </div>
  );
};
