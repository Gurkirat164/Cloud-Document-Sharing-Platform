import React from 'react';
import {
  Upload,
  Download,
  Share2,
  Trash2,
  Eye,
  Clock,
  Filter,
} from 'lucide-react';
import { Card, Button } from '../components/ui';
import { formatDate } from '../utils/helpers';
import type { ActivityLog, ActivityAction } from '../types';

// Mock data
const mockActivities: ActivityLog[] = [
  {
    id: '1',
    action: 'UPLOAD' as ActivityAction,
    fileName: 'Project Proposal.pdf',
    fileId: '1',
    performedBy: 'You',
    performedAt: '2026-02-16T10:30:00Z',
    details: 'Uploaded new file',
  },
  {
    id: '2',
    action: 'SHARE' as ActivityAction,
    fileName: 'Budget Report.xlsx',
    fileId: '2',
    performedBy: 'John Doe',
    performedAt: '2026-02-15T14:20:00Z',
    details: 'Shared with 3 users',
  },
  {
    id: '3',
    action: 'DOWNLOAD' as ActivityAction,
    fileName: 'Team Photo.jpg',
    fileId: '3',
    performedBy: 'Jane Smith',
    performedAt: '2026-02-14T09:15:00Z',
    details: 'Downloaded file',
  },
  {
    id: '4',
    action: 'PERMISSION_CHANGE' as ActivityAction,
    fileName: 'Meeting Notes.docx',
    fileId: '4',
    performedBy: 'You',
    performedAt: '2026-02-13T16:45:00Z',
    details: 'Changed permissions from VIEW to EDIT',
  },
  {
    id: '5',
    action: 'DELETE' as ActivityAction,
    fileName: 'Old Document.txt',
    fileId: '5',
    performedBy: 'You',
    performedAt: '2026-02-12T11:00:00Z',
    details: 'Permanently deleted',
  },
];

const getActivityIcon = (action: ActivityAction) => {
  switch (action) {
    case 'UPLOAD':
      return <Upload className="w-5 h-5" />;
    case 'DOWNLOAD':
      return <Download className="w-5 h-5" />;
    case 'SHARE':
      return <Share2 className="w-5 h-5" />;
    case 'DELETE':
      return <Trash2 className="w-5 h-5" />;
    case 'VIEW':
      return <Eye className="w-5 h-5" />;
    default:
      return <Clock className="w-5 h-5" />;
  }
};

const getActivityColor = (action: ActivityAction) => {
  switch (action) {
    case 'UPLOAD':
      return 'bg-success/10 text-success';
    case 'DOWNLOAD':
      return 'bg-accent/10 text-accent';
    case 'SHARE':
      return 'bg-highlight/10 text-highlight';
    case 'DELETE':
      return 'bg-error/10 text-error';
    case 'VIEW':
      return 'bg-info/10 text-text-muted';
    default:
      return 'bg-warning/10 text-warning';
  }
};

export const ActivityPage: React.FC = () => {
  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-text-primary mb-2">
            Activity Logs
          </h1>
          <p className="text-text-secondary">
            Track all actions performed on your files
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" icon={<Filter className="w-4 h-4" />}>
            Filter
          </Button>
          <Button variant="secondary" size="sm" icon={<Download className="w-4 h-4" />}>
            Export
          </Button>
        </div>
      </div>

      {/* Activity Timeline */}
      <Card>
        <div className="divide-y divide-border">
          {mockActivities.map((activity) => (
            <div key={activity.id} className="p-6 hover:bg-secondary transition-colors">
              <div className="flex items-start gap-4">
                {/* Icon */}
                <div
                  className={`p-3 rounded-lg ${getActivityColor(activity.action)}`}
                >
                  {getActivityIcon(activity.action)}
                </div>

                {/* Content */}
                <div className="flex-1">
                  <div className="flex items-start justify-between mb-1">
                    <div>
                      <h3 className="text-text-primary font-medium mb-1">
                        {activity.action.replace('_', ' ')}
                      </h3>
                      <p className="text-text-secondary text-sm">
                        <span className="font-medium">{activity.performedBy}</span>{' '}
                        performed action on{' '}
                        <span className="font-medium">{activity.fileName}</span>
                      </p>
                    </div>
                    <div className="flex items-center gap-2 text-text-muted text-sm">
                      <Clock className="w-4 h-4" />
                      {formatDate(activity.performedAt)}
                    </div>
                  </div>
                  {activity.details && (
                    <p className="text-text-muted text-sm mt-2">
                      {activity.details}
                    </p>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/* Load More */}
      <div className="text-center">
        <Button variant="secondary">Load More Activities</Button>
      </div>
    </div>
  );
};
