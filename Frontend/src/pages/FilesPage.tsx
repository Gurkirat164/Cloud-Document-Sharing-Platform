import React, { useState } from 'react';
import {
  FolderOpen,
  Grid3x3,
  List,
  Download,
  Share2,
  Trash2,
  Search,
  Filter,
} from 'lucide-react';
import { Button, Card, Modal } from '../components/ui';
import { formatFileSize, formatDate, getFileIcon } from '../utils/helpers';
import type { File } from '../types';

// Mock data
const mockFiles: File[] = [
  {
    id: '1',
    name: 'Project Proposal.pdf',
    size: 2458624,
    type: 'application/pdf',
    uploadedBy: 'current-user',
    uploadedAt: '2026-02-16T10:30:00Z',
    isPublic: false,
    permissions: [],
    version: 1,
    s3Key: 'files/project-proposal.pdf',
  },
  {
    id: '2',
    name: 'Budget Report.xlsx',
    size: 1245698,
    type: 'application/vnd.ms-excel',
    uploadedBy: 'current-user',
    uploadedAt: '2026-02-15T14:20:00Z',
    isPublic: true,
    permissions: [],
    version: 2,
    s3Key: 'files/budget-report.xlsx',
  },
  {
    id: '3',
    name: 'Team Photo.jpg',
    size: 3567890,
    type: 'image/jpeg',
    uploadedBy: 'current-user',
    uploadedAt: '2026-02-14T09:15:00Z',
    isPublic: false,
    permissions: [],
    version: 1,
    s3Key: 'files/team-photo.jpg',
  },
  {
    id: '4',
    name: 'Meeting Notes.docx',
    size: 456789,
    type: 'application/vnd.ms-word',
    uploadedBy: 'current-user',
    uploadedAt: '2026-02-13T16:45:00Z',
    isPublic: false,
    permissions: [],
    version: 1,
    s3Key: 'files/meeting-notes.docx',
  },
  {
    id: '5',
    name: 'Presentation.pptx',
    size: 5678901,
    type: 'application/vnd.ms-powerpoint',
    uploadedBy: 'current-user',
    uploadedAt: '2026-02-12T11:00:00Z',
    isPublic: true,
    permissions: [],
    version: 3,
    s3Key: 'files/presentation.pptx',
  },
];

export const FilesPage: React.FC = () => {
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isShareModalOpen, setIsShareModalOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const filteredFiles = mockFiles.filter((file) =>
    file.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleShare = (file: File) => {
    setSelectedFile(file);
    setIsShareModalOpen(true);
  };

  const handleDelete = (file: File) => {
    // TODO: Implement delete functionality
    console.log('Delete file:', file.id);
  };

  const handleDownload = (file: File) => {
    // TODO: Implement download functionality
    console.log('Download file:', file.id);
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-text-primary mb-2">My Files</h1>
          <p className="text-text-secondary">
            Manage and organize your documents
          </p>
        </div>
        <Button variant="primary" icon={<FolderOpen className="w-5 h-5" />}>
          New Folder
        </Button>
      </div>

      {/* Toolbar */}
      <Card>
        <div className="flex items-center justify-between gap-4 flex-wrap">
          {/* Search */}
          <div className="flex-1 min-w-75 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-text-muted" />
            <input
              type="text"
              placeholder="Search files..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-secondary border border-border rounded-lg text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
            />
          </div>

          {/* Filter & View Controls */}
          <div className="flex items-center gap-2">
            <Button variant="secondary" size="sm" icon={<Filter className="w-4 h-4" />}>
              Filter
            </Button>
            <div className="flex border border-border rounded-lg overflow-hidden">
              <button
                onClick={() => setViewMode('grid')}
                className={`p-2 transition-colors ${
                  viewMode === 'grid'
                    ? 'bg-accent text-primary'
                    : 'bg-secondary text-text-muted hover:text-text-primary'
                }`}
              >
                <Grid3x3 className="w-4 h-4" />
              </button>
              <button
                onClick={() => setViewMode('list')}
                className={`p-2 transition-colors ${
                  viewMode === 'list'
                    ? 'bg-accent text-primary'
                    : 'bg-secondary text-text-muted hover:text-text-primary'
                }`}
              >
                <List className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </Card>

      {/* Files Display */}
      {viewMode === 'grid' ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {filteredFiles.map((file) => (
            <Card key={file.id} hoverable className="group">
              <div className="flex flex-col h-full">
                {/* File Icon/Preview */}
                <div className="w-full h-32 bg-secondary rounded-lg flex items-center justify-center text-5xl mb-4">
                  {getFileIcon(file.type)}
                </div>

                {/* File Info */}
                <div className="flex-1">
                  <h3 className="text-text-primary font-medium mb-1 truncate">
                    {file.name}
                  </h3>
                  <p className="text-text-muted text-sm">
                    {formatFileSize(file.size)}
                  </p>
                  <p className="text-text-muted text-xs mt-1">
                    {formatDate(file.uploadedAt)}
                  </p>
                </div>

                {/* Actions */}
                <div className="flex items-center gap-2 mt-4 pt-4 border-t border-border opacity-0 group-hover:opacity-100 transition-opacity">
                  <button
                    onClick={() => handleDownload(file)}
                    className="flex-1 p-2 text-text-muted hover:text-accent hover:bg-secondary rounded-lg transition-all"
                    title="Download"
                  >
                    <Download className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => handleShare(file)}
                    className="flex-1 p-2 text-text-muted hover:text-accent hover:bg-secondary rounded-lg transition-all"
                    title="Share"
                  >
                    <Share2 className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => handleDelete(file)}
                    className="flex-1 p-2 text-text-muted hover:text-error hover:bg-secondary rounded-lg transition-all"
                    title="Delete"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      ) : (
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="border-b border-border">
                <tr>
                  <th className="text-left py-3 px-4 text-text-secondary font-medium">
                    Name
                  </th>
                  <th className="text-left py-3 px-4 text-text-secondary font-medium">
                    Size
                  </th>
                  <th className="text-left py-3 px-4 text-text-secondary font-medium">
                    Modified
                  </th>
                  <th className="text-left py-3 px-4 text-text-secondary font-medium">
                    Status
                  </th>
                  <th className="text-right py-3 px-4 text-text-secondary font-medium">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody>
                {filteredFiles.map((file) => (
                  <tr
                    key={file.id}
                    className="border-b border-border hover:bg-secondary transition-colors"
                  >
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-3">
                        <span className="text-2xl">{getFileIcon(file.type)}</span>
                        <span className="text-text-primary font-medium">
                          {file.name}
                        </span>
                      </div>
                    </td>
                    <td className="py-3 px-4 text-text-secondary">
                      {formatFileSize(file.size)}
                    </td>
                    <td className="py-3 px-4 text-text-secondary">
                      {formatDate(file.uploadedAt)}
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`px-2 py-1 rounded text-xs font-medium ${
                          file.isPublic
                            ? 'bg-success/10 text-success'
                            : 'bg-warning/10 text-warning'
                        }`}
                      >
                        {file.isPublic ? 'Public' : 'Private'}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => handleDownload(file)}
                          className="p-2 text-text-muted hover:text-accent hover:bg-border rounded-lg transition-all"
                        >
                          <Download className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleShare(file)}
                          className="p-2 text-text-muted hover:text-accent hover:bg-border rounded-lg transition-all"
                        >
                          <Share2 className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDelete(file)}
                          className="p-2 text-text-muted hover:text-error hover:bg-border rounded-lg transition-all"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* Share Modal */}
      <Modal
        isOpen={isShareModalOpen}
        onClose={() => setIsShareModalOpen(false)}
        title="Share File"
      >
        <div className="space-y-4">
          <p className="text-text-secondary">
            Share <span className="font-medium text-text-primary">{selectedFile?.name}</span> with others
          </p>
          {/* TODO: Add share form */}
          <Button variant="primary" className="w-full">
            Share
          </Button>
        </div>
      </Modal>
    </div>
  );
};
