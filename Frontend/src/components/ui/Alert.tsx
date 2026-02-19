import React from 'react';
import { CheckCircle, XCircle, AlertCircle, Info } from 'lucide-react';
import { cn } from '../../utils/helpers';

interface AlertProps {
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  onClose?: () => void;
  className?: string;
}

export const Alert: React.FC<AlertProps> = ({
  type,
  message,
  onClose,
  className,
}) => {
  const icons = {
    success: <CheckCircle className="w-5 h-5" />,
    error: <XCircle className="w-5 h-5" />,
    warning: <AlertCircle className="w-5 h-5" />,
    info: <Info className="w-5 h-5" />,
  };

  const styles = {
    success: 'bg-success/10 border-success/50 text-success-light shadow-lg shadow-success/20',
    error: 'bg-error/10 border-error/50 text-error-light shadow-lg shadow-error/20',
    warning: 'bg-warning/10 border-warning/50 text-warning-light shadow-lg shadow-warning/20',
    info: 'bg-info/10 border-info/50 text-info-light shadow-lg shadow-info/20',
  };

  return (
    <div
      className={cn(
        'flex items-center gap-4 p-4 rounded-xl border-2',
        styles[type],
        className
      )}
    >
      {icons[type]}
      <span className="flex-1 text-sm font-medium">{message}</span>
      {onClose && (
        <button
          onClick={onClose}
          className="hover:opacity-70 transition-opacity"
        >
          <XCircle className="w-4 h-4" />
        </button>
      )}
    </div>
  );
};
