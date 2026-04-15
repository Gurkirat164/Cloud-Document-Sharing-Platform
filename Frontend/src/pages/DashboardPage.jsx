import { useState, useEffect, useCallback, useRef } from 'react';
import { uploadFile, listFiles, deleteFile } from '../api/fileApi';
import useAuth from '../hooks/useAuth';

// ─── Constants ───────────────────────────────────────────────────────────────

const MAX_FILE_SIZE = 500 * 1024 * 1024; // 500 MB

const ALLOWED_TYPES = {
  'image/jpeg': true,
  'image/png': true,
  'image/gif': true,
  'image/webp': true,
  'image/svg+xml': true,
  'application/pdf': true,
  'text/plain': true,
  'text/csv': true,
  'application/json': true,
  'application/zip': true,
  'application/x-zip-compressed': true,
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': true,
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': true,
  'application/vnd.openxmlformats-officedocument.presentationml.presentation': true,
  'application/msword': true,
  'application/vnd.ms-excel': true,
  'video/mp4': true,
  'video/webm': true,
  'audio/mpeg': true,
  'audio/wav': true,
};

// ─── Helpers ─────────────────────────────────────────────────────────────────

const formatBytes = (bytes) => {
  if (!bytes && bytes !== 0) return '—';
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
};

const getFileIconMeta = (mimeType) => {
  if (!mimeType) return { icon: '📄', color: '#64748b' };
  if (mimeType.startsWith('image/')) return { icon: '🖼️', color: '#8b5cf6' };
  if (mimeType.startsWith('video/')) return { icon: '🎬', color: '#ef4444' };
  if (mimeType.startsWith('audio/')) return { icon: '🎵', color: '#f59e0b' };
  if (mimeType === 'application/pdf') return { icon: '📕', color: '#f87171' };
  if (mimeType.includes('spreadsheet') || mimeType.includes('excel')) return { icon: '📊', color: '#4ade80' };
  if (mimeType.includes('word') || mimeType.includes('document')) return { icon: '📝', color: '#60a5fa' };
  if (mimeType.includes('presentation')) return { icon: '📊', color: '#fb923c' };
  if (mimeType.includes('zip') || mimeType.includes('archive')) return { icon: '🗜️', color: '#a78bfa' };
  if (mimeType === 'text/plain') return { icon: '📃', color: '#94a3b8' };
  if (mimeType === 'application/json') return { icon: '{ }', color: '#34d399' };
  return { icon: '📄', color: '#64748b' };
};

const timeAgo = (isoString) => {
  if (!isoString) return '';
  const diff = Date.now() - new Date(isoString).getTime();
  const secs = Math.floor(diff / 1000);
  if (secs < 60) return 'just now';
  const mins = Math.floor(secs / 60);
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  const days = Math.floor(hrs / 24);
  if (days < 30) return `${days}d ago`;
  return new Date(isoString).toLocaleDateString();
};

const validateFile = (file) => {
  if (!ALLOWED_TYPES[file.type]) {
    return `File type "${file.type || 'unknown'}" is not supported`;
  }
  if (file.size > MAX_FILE_SIZE) {
    return `File exceeds 500 MB limit (${formatBytes(file.size)})`;
  }
  if (file.size === 0) {
    return 'File is empty';
  }
  return null;
};

// ─── Inline CSS Animations (injected once) ───────────────────────────────────

const injectStyles = () => {
  if (document.getElementById('cv-dash-styles')) return;
  const style = document.createElement('style');
  style.id = 'cv-dash-styles';
  style.textContent = `
    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap');

    * { box-sizing: border-box; }

    @keyframes cv-shimmer {
      0%   { background-position: -400px 0; }
      100% { background-position: 400px 0; }
    }
    @keyframes cv-spin {
      to { transform: rotate(360deg); }
    }
    @keyframes cv-fadeIn {
      from { opacity: 0; transform: translateY(8px); }
      to   { opacity: 1; transform: translateY(0); }
    }
    @keyframes cv-pulse-ring {
      0%   { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(99,102,241,0.5); }
      70%  { transform: scale(1);    box-shadow: 0 0 0 14px rgba(99,102,241,0);  }
      100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(99,102,241,0);    }
    }
    @keyframes cv-progress-stripe {
      from { background-position: 0 0; }
      to   { background-position: 40px 0; }
    }

    .cv-file-card {
      background: rgba(255,255,255,0.035);
      border: 1px solid rgba(255,255,255,0.07);
      border-radius: 16px;
      padding: 16px 18px;
      display: flex;
      align-items: center;
      gap: 14px;
      transition: background 0.2s, border-color 0.2s, transform 0.15s;
      animation: cv-fadeIn 0.3s ease both;
      cursor: default;
    }
    .cv-file-card:hover {
      background: rgba(255,255,255,0.065);
      border-color: rgba(99,102,241,0.35);
      transform: translateY(-1px);
    }

    .cv-delete-btn {
      background: transparent;
      border: 1px solid rgba(239,68,68,0.25);
      color: #f87171;
      padding: 5px 13px;
      border-radius: 8px;
      cursor: pointer;
      font-size: 12px;
      font-family: 'Inter', sans-serif;
      transition: all 0.2s;
      flex-shrink: 0;
      white-space: nowrap;
    }
    .cv-delete-btn:hover { background: rgba(239,68,68,0.15); border-color: rgba(239,68,68,0.5); }
    .cv-delete-btn:disabled { opacity: 0.4; cursor: not-allowed; }

    .cv-skeleton {
      background: linear-gradient(90deg, rgba(255,255,255,0.04) 25%, rgba(255,255,255,0.09) 50%, rgba(255,255,255,0.04) 75%);
      background-size: 400px 100%;
      animation: cv-shimmer 1.4s infinite;
      border-radius: 8px;
    }

    .cv-upload-item {
      padding: 12px 16px;
      border-radius: 12px;
      border: 1px solid rgba(255,255,255,0.07);
      background: rgba(255,255,255,0.03);
      animation: cv-fadeIn 0.25s ease both;
    }
    .cv-upload-item.error {
      border-color: rgba(239,68,68,0.3);
      background: rgba(239,68,68,0.06);
    }
    .cv-upload-item.done {
      border-color: rgba(74,222,128,0.25);
      background: rgba(74,222,128,0.05);
    }

    .cv-progress-bar {
      height: 5px;
      border-radius: 5px;
      overflow: hidden;
      background: rgba(255,255,255,0.08);
      margin-top: 8px;
    }
    .cv-progress-fill {
      height: 100%;
      border-radius: 5px;
      background: linear-gradient(90deg, #6366f1, #8b5cf6, #6366f1);
      background-size: 200% auto;
      transition: width 0.25s ease;
      animation: cv-shimmer 1.5s linear infinite;
    }
    .cv-progress-fill.done {
      background: #4ade80;
      animation: none;
    }
    .cv-progress-fill.error {
      background: #f87171;
      animation: none;
    }

    .cv-badge {
      display: inline-flex;
      align-items: center;
      font-size: 11px;
      padding: 2px 8px;
      border-radius: 20px;
      font-weight: 600;
      letter-spacing: 0.02em;
    }

    .cv-drop-zone {
      border: 2px dashed rgba(255,255,255,0.14);
      border-radius: 20px;
      padding: 56px 24px;
      text-align: center;
      cursor: pointer;
      background: rgba(255,255,255,0.015);
      transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
      position: relative;
      overflow: hidden;
    }
    .cv-drop-zone:hover {
      border-color: rgba(99,102,241,0.5);
      background: rgba(99,102,241,0.04);
    }
    .cv-drop-zone.active {
      border-color: #818cf8;
      background: rgba(99,102,241,0.1);
      transform: scale(1.01);
    }
    .cv-drop-zone .cv-drop-icon {
      font-size: 48px;
      display: block;
      margin-bottom: 14px;
      transition: transform 0.3s;
    }
    .cv-drop-zone.active .cv-drop-icon { transform: scale(1.15); }

    .cv-validation-error {
      display: flex;
      align-items: flex-start;
      gap: 6px;
      padding: 10px 14px;
      background: rgba(239,68,68,0.08);
      border: 1px solid rgba(239,68,68,0.25);
      border-radius: 10px;
      font-size: 12.5px;
      color: #fca5a5;
      animation: cv-fadeIn 0.2s ease;
    }

    .cv-thumbnail {
      width: 40px;
      height: 40px;
      border-radius: 8px;
      object-fit: cover;
      border: 1px solid rgba(255,255,255,0.1);
      flex-shrink: 0;
    }

    .cv-icon-badge {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      flex-shrink: 0;
    }

    .cv-btn-retry {
      background: transparent;
      border: 1px solid rgba(99,102,241,0.4);
      color: #818cf8;
      padding: 3px 10px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 11px;
      font-family: 'Inter', sans-serif;
      transition: all 0.15s;
      margin-top: 6px;
    }
    .cv-btn-retry:hover { background: rgba(99,102,241,0.15); }
  `;
  document.head.appendChild(style);
};

// ─── UploadItem Component ─────────────────────────────────────────────────────

const UploadItem = ({ id, name, size, progress, status, error, onRetry, file }) => {
  const statusColor = status === 'error' ? '#f87171' : status === 'done' ? '#4ade80' : '#818cf8';
  const statusLabel = status === 'error' ? '✗ Failed' : status === 'done' ? '✓ Done' : `${progress}%`;

  return (
    <div className={`cv-upload-item ${status}`}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        {/* File icon */}
        <div style={{ fontSize: '22px', flexShrink: 0 }}>
          {getFileIconMeta(file?.type).icon}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '13px', color: '#e2e8f0', fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '65%' }}>
              {name}
            </span>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexShrink: 0 }}>
              {size && <span style={{ fontSize: '11px', color: '#475569' }}>{formatBytes(size)}</span>}
              <span style={{ fontSize: '12px', fontWeight: 600, color: statusColor }}>{statusLabel}</span>
            </div>
          </div>

          {/* Progress bar */}
          {(status === 'uploading' || status === 'done') && (
            <div className="cv-progress-bar">
              <div
                className={`cv-progress-fill ${status === 'done' ? 'done' : ''}`}
                style={{ width: `${progress}%` }}
              />
            </div>
          )}

          {/* Error message + retry */}
          {status === 'error' && (
            <div style={{ marginTop: '5px' }}>
              <span style={{ fontSize: '11px', color: '#f87171' }}>{error}</span>
              {onRetry && (
                <div>
                  <button className="cv-btn-retry" onClick={() => onRetry(id, file)}>
                    ↺ Retry
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// ─── FileCard Component ───────────────────────────────────────────────────────

const FileCard = ({ file, onDelete, thumbnails }) => {
  const [deleting, setDeleting] = useState(false);
  const { icon, color } = getFileIconMeta(file.mimeType);
  const thumbUrl = thumbnails[file.uuid];

  const handleDelete = async () => {
    if (!window.confirm(`Delete "${file.originalName}"?\n\nThis action cannot be undone.`)) return;
    setDeleting(true);
    try {
      await onDelete(file.uuid);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="cv-file-card">
      {/* Thumbnail or icon */}
      {thumbUrl ? (
        <img src={thumbUrl} alt={file.originalName} className="cv-thumbnail" />
      ) : (
        <div className="cv-icon-badge" style={{ background: `${color}18` }}>
          <span style={{ fontSize: '20px' }}>{icon}</span>
        </div>
      )}

      {/* File info */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <p style={{ margin: 0, fontSize: '14px', fontWeight: 600, color: '#e2e8f0', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {file.originalName}
        </p>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginTop: '4px', flexWrap: 'wrap' }}>
          <span style={{ fontSize: '12px', color: '#64748b' }}>{formatBytes(file.sizeBytes)}</span>
          <span style={{ color: '#334155', fontSize: '10px' }}>•</span>
          <span style={{ fontSize: '12px', color: '#64748b' }}>{timeAgo(file.uploadedAt)}</span>
          {file.mimeType && (
            <>
              <span style={{ color: '#334155', fontSize: '10px' }}>•</span>
              <span className="cv-badge" style={{ background: `${color}18`, color }}>
                {file.mimeType.split('/')[1]?.toUpperCase().slice(0, 8) || 'FILE'}
              </span>
            </>
          )}
        </div>
      </div>

      {/* Actions */}
      <div style={{ display: 'flex', gap: '8px', flexShrink: 0 }}>
        <button
          className="cv-delete-btn"
          onClick={handleDelete}
          disabled={deleting}
          aria-label={`Delete ${file.originalName}`}
        >
          {deleting ? (
            <span style={{ display: 'inline-block', width: '12px', height: '12px', border: '2px solid #f87171', borderTopColor: 'transparent', borderRadius: '50%', animation: 'cv-spin 0.7s linear infinite', verticalAlign: 'middle' }} />
          ) : '🗑 Delete'}
        </button>
      </div>
    </div>
  );
};

// ─── SkeletonCard ─────────────────────────────────────────────────────────────

const SkeletonCard = () => (
  <div style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)', borderRadius: '16px', padding: '16px 18px', display: 'flex', alignItems: 'center', gap: '14px' }}>
    <div className="cv-skeleton" style={{ width: 40, height: 40, borderRadius: 10, flexShrink: 0 }} />
    <div style={{ flex: 1 }}>
      <div className="cv-skeleton" style={{ height: 14, width: '55%', marginBottom: 8 }} />
      <div className="cv-skeleton" style={{ height: 11, width: '30%' }} />
    </div>
    <div className="cv-skeleton" style={{ width: 72, height: 28, borderRadius: 8 }} />
  </div>
);

// ─── ValidationBanner ─────────────────────────────────────────────────────────

const ValidationBanner = ({ errors, onDismiss }) => (
  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginBottom: '18px' }}>
    {errors.map((e, i) => (
      <div key={i} className="cv-validation-error">
        <span>⚠️</span>
        <span style={{ flex: 1 }}><strong>{e.name}</strong> — {e.reason}</span>
        <button onClick={() => onDismiss(i)} style={{ background: 'none', border: 'none', color: '#f87171', cursor: 'pointer', fontSize: '14px', padding: '0', lineHeight: 1 }}>✕</button>
      </div>
    ))}
  </div>
);

// ─── DashboardPage ────────────────────────────────────────────────────────────

const DashboardPage = () => {
  const { user, logout } = useAuth();

  // State
  const [files, setFiles] = useState([]);
  const [loadingFiles, setLoadingFiles] = useState(true);
  const [uploads, setUploads] = useState([]);
  const [validationErrors, setValidationErrors] = useState([]);
  const [dragging, setDragging] = useState(false);
  const [thumbnails, setThumbnails] = useState({});    // uuid → objectURL
  const [showUploads, setShowUploads] = useState(true);

  const fileInputRef = useRef(null);
  const dragCounter = useRef(0);  // avoids flicker from child drag events

  // Inject animations once
  useEffect(() => { injectStyles(); }, []);

  // Fetch files
  const fetchFiles = useCallback(async () => {
    setLoadingFiles(true);
    try {
      const paged = await listFiles();
      setFiles(paged?.content ?? []);
    } catch (err) {
      console.error('Failed to load files', err);
    } finally {
      setLoadingFiles(false);
    }
  }, []);

  useEffect(() => { fetchFiles(); }, [fetchFiles]);

  // Generate local image thumbnails for image files in the list
  useEffect(() => {
    // For locally uploaded file objects (before they're on cloud) we can't generate URLs
    // This is just for display purposes — currently no-op since we don't have download URLs
    // The hook is here to be wired up when presigned GET URLs are added
  }, [files]);

  // ── Core upload handler ──────────────────────────────────────────────────

  const processFiles = useCallback((fileList) => {
    const toUpload = [];
    const errors = [];

    Array.from(fileList).forEach((file) => {
      const err = validateFile(file);
      if (err) {
        errors.push({ name: file.name, reason: err });
      } else {
        toUpload.push(file);
      }
    });

    if (errors.length) {
      setValidationErrors(prev => [...prev, ...errors]);
    }

    toUpload.forEach((file) => {
      const id = crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).slice(2);

      setUploads(prev => [
        { id, name: file.name, size: file.size, progress: 0, status: 'uploading', file },
        ...prev,
      ]);

      uploadFile(file, (pct) => {
        setUploads(prev => prev.map(u => u.id === id ? { ...u, progress: pct } : u));
      })
        .then(() => {
          setUploads(prev => prev.map(u => u.id === id
            ? { ...u, progress: 100, status: 'done' }
            : u
          ));
          fetchFiles();
          setTimeout(() => {
            setUploads(prev => prev.filter(u => u.id !== id));
          }, 4000);
        })
        .catch((err) => {
          const msg = err.response?.data?.message || err.response?.data?.data?.sizeBytes || err.message || 'Upload failed';
          setUploads(prev => prev.map(u => u.id === id
            ? { ...u, status: 'error', error: msg }
            : u
          ));
        });
    });
  }, [fetchFiles]);

  const handleRetry = useCallback((id, file) => {
    // Remove the failed item and re-queue it
    setUploads(prev => prev.filter(u => u.id !== id));
    if (file) processFiles([file]);
  }, [processFiles]);

  // ── Drag events ──────────────────────────────────────────────────────────

  const handleDragEnter = (e) => {
    e.preventDefault();
    dragCounter.current++;
    setDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    dragCounter.current--;
    if (dragCounter.current === 0) setDragging(false);
  };

  const handleDragOver = (e) => { e.preventDefault(); };

  const handleDrop = (e) => {
    e.preventDefault();
    dragCounter.current = 0;
    setDragging(false);
    processFiles(e.dataTransfer.files);
  };

  // ── File list actions ────────────────────────────────────────────────────

  const handleDelete = async (uuid) => {
    await deleteFile(uuid);
    setFiles(prev => prev.filter(f => f.uuid !== uuid));
    // Also revoke thumbnail URL if any
    if (thumbnails[uuid]) {
      URL.revokeObjectURL(thumbnails[uuid]);
      setThumbnails(prev => { const n = { ...prev }; delete n[uuid]; return n; });
    }
  };

  const dismissError = (idx) => {
    setValidationErrors(prev => prev.filter((_, i) => i !== idx));
  };

  const dismissAllErrors = () => setValidationErrors([]);

  // ── Derived ──────────────────────────────────────────────────────────────

  const usedPct = user
    ? Math.min(100, Math.round(((user.storageUsed ?? 0) / (user.storageQuota ?? 1)) * 100))
    : 0;

  const activeUploads = uploads.filter(u => u.status === 'uploading').length;
  const hasUploads = uploads.length > 0;

  // ─────────────────────────────────────────────────────────────────────────

  return (
    <div style={{ minHeight: '100vh', background: 'linear-gradient(135deg,#0a0818 0%,#1a1235 40%,#0f1a2e 100%)', fontFamily: "'Inter',sans-serif", color: '#e2e8f0' }}>

      {/* ─── Navbar ─────────────────────────────────────────────────────── */}
      <header style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '0 40px', height: '60px',
        background: 'rgba(10,8,24,0.7)',
        borderBottom: '1px solid rgba(255,255,255,0.07)',
        backdropFilter: 'blur(20px)',
        position: 'sticky', top: 0, zIndex: 50,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{ width: 30, height: 30, borderRadius: 8, background: 'linear-gradient(135deg,#6366f1,#8b5cf6)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 15 }}>☁️</div>
          <span style={{ fontWeight: 800, fontSize: '17px', background: 'linear-gradient(90deg,#818cf8,#c084fc)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
            CloudVault
          </span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
          {activeUploads > 0 && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '7px', fontSize: '13px', color: '#818cf8' }}>
              <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#818cf8', display: 'inline-block', animation: 'cv-pulse-ring 1.5s ease-in-out infinite' }} />
              {activeUploads} uploading…
            </div>
          )}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div style={{ width: 30, height: 30, borderRadius: '50%', background: 'linear-gradient(135deg,#6366f1,#8b5cf6)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 700, color: '#fff' }}>
              {(user?.fullName || user?.email || '?')[0].toUpperCase()}
            </div>
            <div style={{ lineHeight: 1.2 }}>
              <div style={{ fontSize: '13px', fontWeight: 600, color: '#e2e8f0' }}>{user?.fullName || 'User'}</div>
              <div style={{ fontSize: '11px', color: '#475569' }}>{user?.email}</div>
            </div>
          </div>
          <button
            onClick={logout}
            style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.25)', color: '#f87171', padding: '6px 14px', borderRadius: '8px', cursor: 'pointer', fontSize: '12.5px', fontFamily: 'Inter,sans-serif', fontWeight: 600 }}
          >
            Sign out
          </button>
        </div>
      </header>

      {/* ─── Main ───────────────────────────────────────────────────────── */}
      <main style={{ maxWidth: '860px', margin: '0 auto', padding: '36px 20px 60px' }}>

        {/* Storage widget */}
        {user && (
          <div style={{ marginBottom: '28px', background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)', borderRadius: '16px', padding: '16px 20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontSize: '13px', color: '#94a3b8', fontWeight: 500 }}>Storage</span>
                <span className="cv-badge" style={{ background: usedPct > 85 ? 'rgba(239,68,68,0.15)' : 'rgba(99,102,241,0.15)', color: usedPct > 85 ? '#f87171' : '#818cf8' }}>
                  {usedPct}%
                </span>
              </div>
              <span style={{ fontSize: '13px', color: '#64748b' }}>
                <span style={{ color: '#e2e8f0', fontWeight: 600 }}>{formatBytes(user.storageUsed ?? 0)}</span>
                &nbsp;/&nbsp;{formatBytes(user.storageQuota)}
              </span>
            </div>
            <div style={{ height: '5px', background: 'rgba(255,255,255,0.07)', borderRadius: '5px', overflow: 'hidden' }}>
              <div style={{
                height: '100%',
                width: `${usedPct}%`,
                background: usedPct > 85
                  ? 'linear-gradient(90deg,#f59e0b,#ef4444)'
                  : 'linear-gradient(90deg,#6366f1,#8b5cf6)',
                borderRadius: '5px',
                transition: 'width 0.6s cubic-bezier(0.4,0,0.2,1)',
              }} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '7px' }}>
              <span style={{ fontSize: '11px', color: '#334155' }}>
                {files.length} file{files.length !== 1 ? 's' : ''}
              </span>
              <span style={{ fontSize: '11px', color: '#334155' }}>
                {formatBytes((user.storageQuota ?? 0) - (user.storageUsed ?? 0))} free
              </span>
            </div>
          </div>
        )}

        {/* Validation errors */}
        {validationErrors.length > 0 && (
          <div style={{ marginBottom: '18px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
              <span style={{ fontSize: '13px', color: '#f87171', fontWeight: 600 }}>
                {validationErrors.length} file{validationErrors.length > 1 ? 's' : ''} rejected
              </span>
              <button onClick={dismissAllErrors} style={{ background: 'none', border: 'none', color: '#475569', cursor: 'pointer', fontSize: '12px', fontFamily: 'Inter,sans-serif' }}>
                Clear all
              </button>
            </div>
            <ValidationBanner errors={validationErrors} onDismiss={dismissError} />
          </div>
        )}

        {/* Drop zone */}
        <div
          id="upload-drop-zone"
          className={`cv-drop-zone${dragging ? ' active' : ''}`}
          onDragEnter={handleDragEnter}
          onDragLeave={handleDragLeave}
          onDragOver={handleDragOver}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          style={{ marginBottom: '28px' }}
          role="button"
          tabIndex={0}
          aria-label="Upload files"
          onKeyDown={e => e.key === 'Enter' && fileInputRef.current?.click()}
        >
          <input
            ref={fileInputRef}
            id="file-input"
            type="file"
            multiple
            style={{ display: 'none' }}
            onChange={e => { processFiles(e.target.files); e.target.value = ''; }}
            accept={Object.keys(ALLOWED_TYPES).join(',')}
          />

          <span className="cv-drop-icon">{dragging ? '🎯' : '📤'}</span>

          <p style={{ fontSize: '17px', fontWeight: 700, color: dragging ? '#818cf8' : '#e2e8f0', margin: '0 0 6px', transition: 'color 0.2s' }}>
            {dragging ? 'Drop it!' : 'Drag & drop files here'}
          </p>
          <p style={{ fontSize: '13px', color: '#475569', margin: '0 0 16px' }}>
            or <span style={{ color: '#818cf8', fontWeight: 600 }}>click to browse</span>
          </p>

          {/* Allowed types hint */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', justifyContent: 'center' }}>
            {['PDF', 'Images', 'Word', 'Excel', 'ZIP', 'Video', 'Audio'].map(t => (
              <span key={t} className="cv-badge" style={{ background: 'rgba(255,255,255,0.05)', color: '#64748b' }}>
                {t}
              </span>
            ))}
          </div>
          <p style={{ fontSize: '11px', color: '#334155', marginTop: '10px', marginBottom: 0 }}>
            Max 500 MB per file
          </p>
        </div>

        {/* Upload queue */}
        {hasUploads && (
          <div style={{ marginBottom: '28px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontSize: '12px', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.06em' }}>Transfers</span>
                {activeUploads > 0 && (
                  <span className="cv-badge" style={{ background: 'rgba(99,102,241,0.2)', color: '#818cf8' }}>
                    {activeUploads} active
                  </span>
                )}
              </div>
              <button onClick={() => setShowUploads(v => !v)} style={{ background: 'none', border: 'none', color: '#475569', cursor: 'pointer', fontSize: '12px', fontFamily: 'Inter,sans-serif' }}>
                {showUploads ? '▲ Hide' : '▼ Show'}
              </button>
            </div>

            {showUploads && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '7px' }}>
                {uploads.map(u => (
                  <UploadItem key={u.id} {...u} onRetry={handleRetry} />
                ))}
              </div>
            )}
          </div>
        )}

        {/* File list */}
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
            <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 700, color: '#cbd5e1' }}>
              Your Files
              {!loadingFiles && (
                <span style={{ marginLeft: 8, fontSize: '13px', fontWeight: 500, color: '#475569' }}>
                  ({files.length})
                </span>
              )}
            </h2>
            <button
              onClick={fetchFiles}
              disabled={loadingFiles}
              style={{ background: 'none', border: 'none', color: '#6366f1', cursor: 'pointer', fontSize: '13px', fontFamily: 'Inter,sans-serif', opacity: loadingFiles ? 0.5 : 1 }}
            >
              ↻ Refresh
            </button>
          </div>

          {loadingFiles ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {[1, 2, 3].map(i => <SkeletonCard key={i} />)}
            </div>
          ) : files.length === 0 ? (
            <div style={{
              textAlign: 'center', padding: '60px 24px',
              background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.05)',
              borderRadius: '20px', animation: 'cv-fadeIn 0.4s ease',
            }}>
              <div style={{ fontSize: '48px', marginBottom: '14px' }}>📂</div>
              <p style={{ fontSize: '16px', fontWeight: 600, color: '#475569', margin: '0 0 6px' }}>
                No files yet
              </p>
              <p style={{ fontSize: '13px', color: '#334155', margin: 0 }}>
                Drag & drop files above or click to start uploading
              </p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '9px' }}>
              {files.map(file => (
                <FileCard
                  key={file.uuid}
                  file={file}
                  onDelete={handleDelete}
                  thumbnails={thumbnails}
                />
              ))}
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

export default DashboardPage;
