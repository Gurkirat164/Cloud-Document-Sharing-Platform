import React from 'react';
import { cn } from '../../utils/helpers';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
  icon?: React.ReactNode;
}

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  icon,
  className,
  disabled,
  ...props
}) => {
  const baseStyles =
    'inline-flex items-center justify-center gap-2 font-medium rounded-lg transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-accent focus:ring-offset-2 focus:ring-offset-background disabled:opacity-50 disabled:cursor-not-allowed';

  const variants = {
    primary:
      'bg-primary text-white hover:bg-primary-hover active:bg-primary-active shadow-lg shadow-primary/30 hover:shadow-xl hover:shadow-primary/40',
    secondary:
      'bg-surface text-text-primary border border-border-strong hover:bg-surface-elevated hover:border-primary/50',
    danger:
      'bg-error text-white hover:bg-red-600 active:bg-red-700 shadow-lg shadow-error/30 hover:shadow-xl hover:shadow-error/40',
    ghost:
      'bg-transparent text-text-secondary hover:bg-surface hover:text-text-primary',
  };

  const sizes = {
    sm: 'px-4 py-2 text-sm font-medium rounded-lg',
    md: 'px-6 py-2.5 text-base font-medium rounded-lg',
    lg: 'px-8 py-3 text-lg font-semibold rounded-xl',
  };

  return (
    <button
      className={cn(
        baseStyles,
        variants[variant],
        sizes[size],
        isLoading && 'cursor-wait',
        className
      )}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? (
        <svg
          className="animate-spin h-5 w-5"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
      ) : (
        icon
      )}
      {children}
    </button>
  );
};
