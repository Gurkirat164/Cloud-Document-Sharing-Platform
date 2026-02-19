import React from 'react';
import {
  Upload,
  Download,
  Folder,
  Activity,
  TrendingUp,
  Users,
  FileText,
  Clock,
} from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../components/ui';
import { formatFileSize, formatDate } from '../utils/helpers';

// Mock data
const stats = [
  {
    label: 'Total Files',
    value: '124',
    icon: <FileText className="w-6 h-6" />,
    trend: '+12%',
    color: 'text-primary',
  },
  {
    label: 'Storage Used',
    value: '2.4 GB',
    icon: <Folder className="w-6 h-6" />,
    trend: '+8%',
    color: 'text-secondary',
  },
  {
    label: 'Shared Files',
    value: '38',
    icon: <Users className="w-6 h-6" />,
    trend: '+5',
    color: 'text-warning-light',
  },
  {
    label: 'Downloads',
    value: '856',
    icon: <Download className="w-6 h-6" />,
    trend: '+23%',
    color: 'text-info-light',
  },
];

const recentFiles = [
  {
    id: '1',
    name: 'Project Proposal.pdf',
    size: 2458624,
    uploadedAt: '2026-02-16T10:30:00Z',
    type: 'application/pdf',
  },
  {
    id: '2',
    name: 'Budget Report.xlsx',
    size: 1245698,
    uploadedAt: '2026-02-15T14:20:00Z',
    type: 'application/vnd.ms-excel',
  },
  {
    id: '3',
    name: 'Team Photo.jpg',
    size: 3567890,
    uploadedAt: '2026-02-14T09:15:00Z',
    type: 'image/jpeg',
  },
  {
    id: '4',
    name: 'Meeting Notes.docx',
    size: 456789,
    uploadedAt: '2026-02-13T16:45:00Z',
    type: 'application/vnd.ms-word',
  },
];

const recentActivity = [
  {
    id: '1',
    action: 'uploaded',
    fileName: 'Project Proposal.pdf',
    user: 'You',
    time: '2026-02-16T10:30:00Z',
  },
  {
    id: '2',
    action: 'shared',
    fileName: 'Budget Report.xlsx',
    user: 'John Doe',
    time: '2026-02-15T14:20:00Z',
  },
  {
    id: '3',
    action: 'downloaded',
    fileName: 'Team Photo.jpg',
    user: 'Jane Smith',
    time: '2026-02-14T09:15:00Z',
  },
  {
    id: '4',
    action: 'deleted',
    fileName: 'Old Document.txt',
    user: 'You',
    time: '2026-02-13T16:45:00Z',
  },
];

export const DashboardPage: React.FC = () => {
  return (
    <div className="space-y-8">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-4xl font-bold text-text-primary mb-2 tracking-tight">Dashboard</h1>
          <p className="text-text-secondary text-lg">
            Overview of your document management activity
          </p>
        </div>
        <button className="px-6 py-3 bg-primary text-white rounded-xl font-semibold shadow-lg shadow-primary/30 hover:shadow-xl hover:shadow-primary/40 hover:bg-primary-hover transition-all duration-200">
          Quick Upload
        </button>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {stats.map((stat) => (
          <Card key={stat.label} hoverable className="group">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-text-muted text-sm font-semibold mb-2 uppercase tracking-wider">{stat.label}</p>
                <h3 className="text-4xl font-bold text-text-primary mb-3">
                  {stat.value}
                </h3>
                <div className="flex items-center gap-1.5 text-sm text-success-light font-medium">
                  <TrendingUp className="w-4 h-4" />
                  <span>{stat.trend}</span>
                </div>
              </div>
              <div className={`p-4 bg-gradient-to-br from-primary/20 to-secondary/20 rounded-xl ${stat.color} group-hover:scale-110 group-hover:shadow-lg transition-all duration-300`}>
                {stat.icon}
              </div>
            </div>
          </Card>
        ))}
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Recent Files */}
        <Card className="overflow-hidden">
          <CardHeader>
            <CardTitle className="text-2xl">Recent Files</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {recentFiles.map((file) => (
                <div
                  key={file.id}
                  className="flex items-center gap-4 p-4 rounded-xl hover:bg-surface-elevated transition-all duration-200 cursor-pointer group"
                >
                  <div className="w-12 h-12 bg-gradient-to-br from-primary/20 to-secondary/20 rounded-xl flex items-center justify-center text-2xl group-hover:scale-110 transition-transform duration-200">
                    📄
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-text-primary font-semibold truncate group-hover:text-primary transition-colors">
                      {file.name}
                    </p>
                    <p className="text-text-muted text-sm">
                      {formatFileSize(file.size)} •{' '}
                      {formatDate(file.uploadedAt)}
                    </p>
                  </div>
                  <button className="text-text-muted hover:text-primary transition-colors p-2 hover:bg-primary/10 rounded-lg">
                    <Download className="w-5 h-5" />
                  </button>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Recent Activity */}
        <Card>
          <CardHeader>
            <CardTitle>Recent Activity</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {recentActivity.map((activity) => (
                <div key={activity.id} className="flex items-start gap-3">
                  <div
                    className={`p-2 rounded-lg ${
                      activity.action === 'uploaded'
                        ? 'bg-success/10 text-success'
                        : activity.action === 'shared'
                        ? 'bg-accent/10 text-accent'
                        : activity.action === 'downloaded'
                        ? 'bg-warning/10 text-warning'
                        : 'bg-error/10 text-error'
                    }`}
                  >
                    {activity.action === 'uploaded' && (
                      <Upload className="w-4 h-4" />
                    )}
                    {activity.action === 'shared' && (
                      <Users className="w-4 h-4" />
                    )}
                    {activity.action === 'downloaded' && (
                      <Download className="w-4 h-4" />
                    )}
                    {activity.action === 'deleted' && (
                      <Activity className="w-4 h-4" />
                    )}
                  </div>
                  <div className="flex-1">
                    <p className="text-text-primary text-sm">
                      <span className="font-medium">{activity.user}</span>{' '}
                      {activity.action}{' '}
                      <span className="font-medium">{activity.fileName}</span>
                    </p>
                    <p className="text-text-muted text-xs flex items-center gap-1 mt-1">
                      <Clock className="w-3 h-3" />
                      {formatDate(activity.time)}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle>Quick Actions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <button className="flex items-center gap-3 p-4 bg-secondary rounded-lg hover:bg-border transition-colors group">
              <div className="p-3 bg-accent rounded-lg group-hover:scale-110 transition-transform">
                <Upload className="w-5 h-5 text-primary" />
              </div>
              <div className="text-left">
                <p className="text-text-primary font-medium">Upload Files</p>
                <p className="text-text-muted text-sm">
                  Add new documents
                </p>
              </div>
            </button>

            <button className="flex items-center gap-3 p-4 bg-secondary rounded-lg hover:bg-border transition-colors group">
              <div className="p-3 bg-highlight rounded-lg group-hover:scale-110 transition-transform">
                <Folder className="w-5 h-5 text-primary" />
              </div>
              <div className="text-left">
                <p className="text-text-primary font-medium">Create Folder</p>
                <p className="text-text-muted text-sm">
                  Organize your files
                </p>
              </div>
            </button>

            <button className="flex items-center gap-3 p-4 bg-secondary rounded-lg hover:bg-border transition-colors group">
              <div className="p-3 bg-warning rounded-lg group-hover:scale-110 transition-transform">
                <Users className="w-5 h-5 text-primary" />
              </div>
              <div className="text-left">
                <p className="text-text-primary font-medium">Share Files</p>
                <p className="text-text-muted text-sm">
                  Collaborate with team
                </p>
              </div>
            </button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
