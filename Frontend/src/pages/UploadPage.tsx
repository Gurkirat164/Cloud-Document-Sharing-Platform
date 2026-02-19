import React, { useState, useRef } from 'react';
import { Upload, X, CheckCircle, FileText } from 'lucide-react';
import { Button, Card, CardHeader, CardTitle, CardContent, Alert } from '../components/ui';
import { formatFileSize } from '../utils/helpers';

interface FileUpload {
  id: string;
  file: File;
  progress: number;
  status: 'pending' | 'uploading' | 'completed' | 'error';
  error?: string;
}

export const UploadPage: React.FC = () => {
  const [files, setFiles] = useState<FileUpload[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);

    const droppedFiles = Array.from(e.dataTransfer.files);
    addFiles(droppedFiles);
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const selectedFiles = Array.from(e.target.files);
      addFiles(selectedFiles);
    }
  };

  const addFiles = (newFiles: File[]) => {
    const fileUploads: FileUpload[] = newFiles.map((file) => ({
      id: Math.random().toString(36).substr(2, 9),
      file,
      progress: 0,
      status: 'pending',
    }));

    setFiles((prev) => [...prev, ...fileUploads]);
  };

  const removeFile = (id: string) => {
    setFiles((prev) => prev.filter((f) => f.id !== id));
  };

  const uploadFile = async (fileUpload: FileUpload) => {
    // TODO: Implement actual upload to backend
    setFiles((prev) =>
      prev.map((f) =>
        f.id === fileUpload.id ? { ...f, status: 'uploading' } : f
      )
    );

    // Simulate upload progress
    for (let progress = 0; progress <= 100; progress += 10) {
      await new Promise((resolve) => setTimeout(resolve, 200));
      setFiles((prev) =>
        prev.map((f) => (f.id === fileUpload.id ? { ...f, progress } : f))
      );
    }

    setFiles((prev) =>
      prev.map((f) =>
        f.id === fileUpload.id ? { ...f, status: 'completed', progress: 100 } : f
      )
    );
  };

  const handleUploadAll = () => {
    files
      .filter((f) => f.status === 'pending')
      .forEach((f) => uploadFile(f));
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Page Header */}
      <div>
        <h1 className="text-3xl font-bold text-text-primary mb-2">Upload Files</h1>
        <p className="text-text-secondary">
          Upload documents securely to your CloudVault
        </p>
      </div>

      {/* Upload Zone */}
      <Card>
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          className={`border-2 border-dashed rounded-lg p-12 text-center transition-all ${
            isDragging
              ? 'border-accent bg-accent/5'
              : 'border-border hover:border-accent hover:bg-accent/5'
          }`}
        >
          <div className="flex flex-col items-center gap-4">
            <div className="w-16 h-16 bg-accent/10 rounded-full flex items-center justify-center">
              <Upload className="w-8 h-8 text-accent" />
            </div>
            <div>
              <h3 className="text-xl font-semibold text-text-primary mb-2">
                Drop files here or click to browse
              </h3>
              <p className="text-text-muted mb-4">
                Supports: PDF, DOC, DOCX, XLS, XLSX, JPG, PNG, and more
              </p>
              <Button
                variant="primary"
                onClick={() => fileInputRef.current?.click()}
              >
                Select Files
              </Button>
              <input
                ref={fileInputRef}
                type="file"
                multiple
                onChange={handleFileSelect}
                className="hidden"
              />
            </div>
          </div>
        </div>
      </Card>

      {/* Upload Queue */}
      {files.length > 0 && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle>
                Upload Queue ({files.length} file{files.length !== 1 ? 's' : ''})
              </CardTitle>
              <div className="flex gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => setFiles([])}
                >
                  Clear All
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  onClick={handleUploadAll}
                  disabled={files.every((f) => f.status !== 'pending')}
                >
                  Upload All
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {files.map((fileUpload) => (
                <div
                  key={fileUpload.id}
                  className="flex items-center gap-4 p-4 bg-secondary rounded-lg"
                >
                  {/* File Icon */}
                  <div className="w-10 h-10 bg-card rounded-lg flex items-center justify-center">
                    <FileText className="w-5 h-5 text-accent" />
                  </div>

                  {/* File Info */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between mb-1">
                      <p className="text-text-primary font-medium truncate">
                        {fileUpload.file.name}
                      </p>
                      <span className="text-text-muted text-sm ml-2">
                        {formatFileSize(fileUpload.file.size)}
                      </span>
                    </div>

                    {/* Progress Bar */}
                    {(fileUpload.status === 'uploading' ||
                      fileUpload.status === 'completed') && (
                      <div className="w-full h-2 bg-background rounded-full overflow-hidden">
                        <div
                          className={`h-full rounded-full transition-all ${
                            fileUpload.status === 'completed'
                              ? 'bg-success'
                              : 'bg-accent'
                          }`}
                          style={{ width: `${fileUpload.progress}%` }}
                        />
                      </div>
                    )}

                    {/* Status */}
                    {fileUpload.status === 'error' && (
                      <p className="text-error text-sm mt-1">
                        {fileUpload.error || 'Upload failed'}
                      </p>
                    )}
                  </div>

                  {/* Status Icon */}
                  <div>
                    {fileUpload.status === 'completed' ? (
                      <CheckCircle className="w-5 h-5 text-success" />
                    ) : fileUpload.status === 'uploading' ? (
                      <div className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
                    ) : (
                      <button
                        onClick={() => removeFile(fileUpload.id)}
                        className="text-text-muted hover:text-error transition-colors"
                      >
                        <X className="w-5 h-5" />
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Upload Guidelines */}
      <Alert type="info" message="Maximum file size: 100MB per file. All uploads are encrypted." />
    </div>
  );
};
