import React, { forwardRef } from 'react';
import { cn } from '../../utils/helpers';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  icon?: React.ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, helperText, icon, className, type = 'text', ...props }, ref) => {
    return (
      <div className="w-full">
        {label && (
          <label className="block text-sm font-semibold text-text-primary mb-2">
            {label}
          </label>
        )}
        <div className="relative">
          {icon && (
            <div className="absolute left-4 top-1/2 -translate-y-1/2 text-text-muted">
              {icon}
            </div>
          )}
          <input
            ref={ref}
            type={type}
            className={cn(
              'w-full px-4 py-3 bg-surface text-text-primary rounded-lg border-2 border-border',
              'placeholder:text-text-muted',
              'focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary',
              'transition-all duration-200',
              'disabled:opacity-50 disabled:cursor-not-allowed',
              error && 'border-error focus:ring-error/50 focus:border-error',
              icon ? 'pl-11' : '',
              className
            )}
            {...props}
          />
        </div>
        {error && (
          <p className="mt-2 text-sm font-medium text-error-light flex items-center gap-1">
            <svg
              className="w-4 h-4"
              fill="currentColor"
              viewBox="0 0 20 20"
            >
              <path
                fillRule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                clipRule="evenodd"
              />
            </svg>
            {error}
          </p>
        )}
        {helperText && !error && (
          <p className="mt-1.5 text-sm text-text-muted">{helperText}</p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
