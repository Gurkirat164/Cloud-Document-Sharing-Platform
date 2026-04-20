import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getVersionHistory, restoreVersion, deleteVersion } from '../api/fileApi';

/* ─── Helpers ──────────────────────────────────────────────────────────────── */

const formatBytes = (bytes) => {
  if (!bytes && bytes !== 0) return '—';
  if (bytes === 0) return '0 B';
  if (bytes < 1_024) return `${bytes} B`;
  if (bytes < 1_048_576) return `${(bytes / 1_024).toFixed(1)} KB`;
  if (bytes < 1_073_741_824) return `${(bytes / 1_048_576).toFixed(1)} MB`;
  return `${(bytes / 1_073_741_824).toFixed(1)} GB`;
};

const formatDate = (isoString) => {
  if (!isoString) return '—';
  return new Date(isoString).toLocaleString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
};

/* ─── Inline styles (injected once) ────────────────────────────────────────── */

const injectVersionStyles = () => {
  if (document.getElementById('cv-version-styles')) return;
  const style = document.createElement('style');
  style.id = 'cv-version-styles';
  style.textContent = `
    @keyframes cv-vfadeIn {
      from { opacity: 0; transform: scale(0.97); }
      to   { opacity: 1; transform: scale(1); }
    }
    @keyframes cv-vspin {
      to { transform: rotate(360deg); }
    }

    .cv-version-overlay {
      position: fixed; inset: 0; z-index: 1000;
      background: rgba(0, 0, 0, 0.70);
      backdrop-filter: blur(6px);
      display: flex; align-items: center; justify-content: center;
      padding: 20px;
      animation: cv-vfadeIn 0.2s ease;
    }

    .cv-version-panel {
      background: linear-gradient(160deg, #131026 0%, #1a1535 100%);
      border: 1px solid rgba(255,255,255,0.10);
      border-radius: 20px;
      width: 100%;
      max-width: 820px;
      max-height: 85vh;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      box-shadow: 0 32px 64px rgba(0,0,0,0.6);
    }

    .cv-version-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 20px 24px;
      border-bottom: 1px solid rgba(255,255,255,0.07);
      flex-shrink: 0;
    }

    .cv-version-body {
      overflow-y: auto;
      flex: 1;
      padding: 20px 24px;
    }

    .cv-version-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 13px;
      font-family: 'Inter', system-ui, sans-serif;
    }

    .cv-version-table th {
      text-align: left;
      padding: 8px 10px;
      color: #64748b;
      font-size: 11px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      border-bottom: 1px solid rgba(255,255,255,0.07);
      white-space: nowrap;
    }

    .cv-version-table td {
      padding: 12px 10px;
      color: #cbd5e1;
      border-bottom: 1px solid rgba(255,255,255,0.04);
      vertical-align: middle;
    }

    .cv-version-row-current td {
      background: rgba(34,197,94,0.05);
    }

    .cv-version-row-current:hover td {
      background: rgba(34,197,94,0.08);
    }

    .cv-version-table tr:not(.cv-version-row-current):hover td {
      background: rgba(255,255,255,0.025);
    }

    .cv-version-badge-current {
      display: inline-flex; align-items: center; gap: 4px;
      background: rgba(34,197,94,0.15); color: #4ade80;
      font-size: 11px; font-weight: 700;
      padding: 2px 8px; border-radius: 20px;
    }

    .cv-version-badge-restored {
      display: inline-flex; align-items: center;
      background: rgba(245,158,11,0.15); color: #fbbf24;
      font-size: 10px; font-weight: 600;
      padding: 2px 7px; border-radius: 20px;
      margin-left: 6px;
    }

    .cv-version-btn {
      background: transparent;
      border: 1px solid rgba(99,102,241,0.35);
      color: #818cf8;
      padding: 4px 11px;
      border-radius: 7px;
      cursor: pointer;
      font-size: 12px;
      font-family: 'Inter', sans-serif;
      font-weight: 500;
      transition: all 0.15s;
      white-space: nowrap;
    }
    .cv-version-btn:hover { background: rgba(99,102,241,0.15); border-color: rgba(99,102,241,0.6); }
    .cv-version-btn:disabled { opacity: 0.4; cursor: not-allowed; }

    .cv-version-btn-delete {
      background: transparent;
      border: 1px solid rgba(239,68,68,0.25);
      color: #f87171;
      padding: 4px 11px;
      border-radius: 7px;
      cursor: pointer;
      font-size: 12px;
      font-family: 'Inter', sans-serif;
      font-weight: 500;
      transition: all 0.15s;
      white-space: nowrap;
      margin-left: 6px;
    }
    .cv-version-btn-delete:hover { background: rgba(239,68,68,0.15); border-color: rgba(239,68,68,0.5); }
    .cv-version-btn-delete:disabled { opacity: 0.4; cursor: not-allowed; }

    .cv-version-close-btn {
      background: rgba(255,255,255,0.06);
      border: 1px solid rgba(255,255,255,0.1);
      color: #94a3b8;
      width: 32px; height: 32px;
      border-radius: 8px;
      cursor: pointer;
      font-size: 16px;
      display: flex; align-items: center; justify-content: center;
      transition: all 0.15s;
      flex-shrink: 0;
    }
    .cv-version-close-btn:hover { background: rgba(255,255,255,0.12); color: #e2e8f0; }

    .cv-version-feedback {
      padding: 10px 14px;
      border-radius: 10px;
      font-size: 12.5px;
      font-family: 'Inter', sans-serif;
      margin-bottom: 14px;
      animation: cv-vfadeIn 0.2s ease;
      display: flex; align-items: center; gap: 8px;
    }
    .cv-version-feedback.success {
      background: rgba(74,222,128,0.10);
      border: 1px solid rgba(74,222,128,0.25);
      color: #86efac;
    }
    .cv-version-feedback.error {
      background: rgba(239,68,68,0.10);
      border: 1px solid rgba(239,68,68,0.25);
      color: #fca5a5;
    }

    .cv-version-spinner {
      width: 14px; height: 14px;
      border: 2px solid rgba(255,255,255,0.15);
      border-top-color: #818cf8;
      border-radius: 50%;
      animation: cv-vspin 0.7s linear infinite;
      display: inline-block;
    }

    .cv-version-empty {
      text-align: center; padding: 48px 24px;
      color: #475569;
    }

    .cv-version-loading {
      display: flex; align-items: center; justify-content: center;
      gap: 10px;
      padding: 48px 24px;
      color: #64748b;
      font-size: 13px;
      font-family: 'Inter', sans-serif;
    }

    .cv-version-skeleton {
      background: linear-gradient(90deg,
        rgba(255,255,255,0.04) 25%,
        rgba(255,255,255,0.08) 50%,
        rgba(255,255,255,0.04) 75%);
      background-size: 400px 100%;
      animation: cv-shimmer 1.4s infinite;
      border-radius: 6px;
    }
    @keyframes cv-shimmer {
      0%   { background-position: -400px 0; }
      100% { background-position:  400px 0; }
    }
  `;
  document.head.appendChild(style);
};

// Inject on module load
injectVersionStyles();

/* ─── FileVersionHistory ───────────────────────────────────────────────────── */

/**
 * Modal that shows the full version history of a file and allows
 * restoring or permanently deleting non-current versions.
 *
 * Props:
 *   fileUuid   {string}    public UUID of the file
 *   fileName   {string}    display name shown in the modal title
 *   onClose    {function}  called when the user closes the modal
 *   onRestored {function}  called after a successful restore (parent can refresh)
 */
const FileVersionHistory = ({ fileUuid, fileName, onClose, onRestored }) => {
  const queryClient = useQueryClient();
  const [feedback, setFeedback] = useState(null); // { type: 'success'|'error', message }
  const [busyVersionId, setBusyVersionId] = useState(null);

  // ── Fetch version history ───────────────────────────────────────────────
  const {
    data: versions,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['versions', fileUuid],
    queryFn:  () => getVersionHistory(fileUuid),
    staleTime: 10_000,
  });

  // ── Restore mutation ────────────────────────────────────────────────────
  const restoreMutation = useMutation({
    mutationFn: ({ vNum }) => restoreVersion(fileUuid, vNum),
    onSuccess: (data, { vNum }) => {
      setFeedback({ type: 'success', message: `File restored to version ${vNum} successfully.` });
      refetch();
      queryClient.invalidateQueries({ queryKey: ['files'] });
      queryClient.invalidateQueries({ queryKey: ['fileStats'] });
      if (onRestored) onRestored(data);
    },
    onError: (err) => {
      setFeedback({
        type: 'error',
        message: err.response?.data?.message || err.message || 'Restore failed. Please try again.',
      });
    },
    onSettled: () => setBusyVersionId(null),
  });

  // ── Delete mutation ─────────────────────────────────────────────────────
  const deleteMutation = useMutation({
    mutationFn: ({ vNum }) => deleteVersion(fileUuid, vNum),
    onSuccess: (_, { vNum }) => {
      setFeedback({ type: 'success', message: `Version ${vNum} permanently deleted.` });
      refetch();
    },
    onError: (err) => {
      setFeedback({
        type: 'error',
        message: err.response?.data?.message || err.message || 'Delete failed. Please try again.',
      });
    },
    onSettled: () => setBusyVersionId(null),
  });

  // ── Handlers ────────────────────────────────────────────────────────────
  const handleRestore = (vNum) => {
    const confirmed = window.confirm(
      `Restore file to version ${vNum}? This will create a new version with the old content.`
    );
    if (!confirmed) return;
    setFeedback(null);
    setBusyVersionId(vNum);
    restoreMutation.mutate({ vNum });
  };

  const handleDelete = (vNum) => {
    const confirmed = window.confirm(
      `Permanently delete version ${vNum}? This cannot be undone.`
    );
    if (!confirmed) return;
    setFeedback(null);
    setBusyVersionId(vNum);
    deleteMutation.mutate({ vNum });
  };

  // Close on overlay click (not panel click)
  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) onClose();
  };

  // ── Render ──────────────────────────────────────────────────────────────
  return (
    <div
      className="cv-version-overlay"
      id="version-history-modal"
      role="dialog"
      aria-modal="true"
      aria-label={`Version history — ${fileName}`}
      onClick={handleOverlayClick}
    >
      <div className="cv-version-panel">

        {/* Header */}
        <div className="cv-version-header">
          <div>
            <h2 style={{
              margin: 0, fontSize: '16px', fontWeight: 700,
              color: '#e2e8f0', fontFamily: "'Inter', sans-serif",
            }}>
              Version History
            </h2>
            <p style={{
              margin: '3px 0 0', fontSize: '12px',
              color: '#475569', fontFamily: "'Inter', sans-serif",
              overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
              maxWidth: '500px',
            }}>
              {fileName}
            </p>
          </div>
          <button
            id="close-version-modal-btn"
            className="cv-version-close-btn"
            onClick={onClose}
            aria-label="Close version history"
          >
            ✕
          </button>
        </div>

        {/* Body */}
        <div className="cv-version-body">

          {/* Feedback banner */}
          {feedback && (
            <div className={`cv-version-feedback ${feedback.type}`}>
              <span>{feedback.type === 'success' ? '✅' : '⚠️'}</span>
              <span style={{ flex: 1 }}>{feedback.message}</span>
              <button
                onClick={() => setFeedback(null)}
                style={{ background: 'none', border: 'none', color: 'inherit', cursor: 'pointer', opacity: 0.7, fontSize: '14px', padding: 0 }}
              >✕</button>
            </div>
          )}

          {/* Loading */}
          {isLoading && (
            <div className="cv-version-loading">
              <span className="cv-version-spinner" />
              <span>Loading version history…</span>
            </div>
          )}

          {/* Error */}
          {isError && !isLoading && (
            <div className="cv-version-feedback error" style={{ marginBottom: 0 }}>
              <span>⚠️</span>
              <span>{error?.response?.data?.message || error?.message || 'Failed to load version history'}</span>
              <button
                onClick={refetch}
                style={{
                  marginLeft: 'auto', background: 'none', border: '1px solid rgba(239,68,68,0.4)',
                  color: '#f87171', cursor: 'pointer', borderRadius: '6px',
                  padding: '2px 8px', fontSize: '11px', fontFamily: 'Inter,sans-serif',
                }}
              >
                Retry
              </button>
            </div>
          )}

          {/* Empty */}
          {!isLoading && !isError && versions?.length === 0 && (
            <div className="cv-version-empty">
              <div style={{ fontSize: '36px', marginBottom: '12px' }}>📋</div>
              <p style={{ margin: 0, fontFamily: "'Inter', sans-serif", fontSize: '14px' }}>
                No version history available.
              </p>
              <p style={{ margin: '6px 0 0', fontSize: '12px', color: '#334155', fontFamily: "'Inter', sans-serif" }}>
                Versions are recorded when bucket versioning is enabled on S3.
              </p>
            </div>
          )}

          {/* Version table */}
          {!isLoading && !isError && versions?.length > 0 && (
            <table className="cv-version-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Name</th>
                  <th>Size</th>
                  <th>Uploaded by</th>
                  <th>Date</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {versions.map((v) => {
                  const isBusy = busyVersionId === v.versionNumber;

                  return (
                    <tr
                      key={v.id}
                      className={v.isCurrentVersion ? 'cv-version-row-current' : ''}
                    >
                      {/* Version number */}
                      <td>
                        <span style={{ fontWeight: 700, color: '#818cf8' }}>
                          v{v.versionNumber}
                        </span>
                      </td>

                      {/* Filename */}
                      <td style={{ maxWidth: '180px' }}>
                        <span style={{
                          overflow: 'hidden', textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap', display: 'block',
                        }}>
                          {v.originalName}
                        </span>
                        {v.restoredFromVersion != null && (
                          <span className="cv-version-badge-restored">
                            ↩ from v{v.restoredFromVersion}
                          </span>
                        )}
                      </td>

                      {/* Size */}
                      <td style={{ whiteSpace: 'nowrap', color: '#64748b' }}>
                        {v.sizeFormatted || formatBytes(v.sizeBytes)}
                      </td>

                      {/* Uploaded by */}
                      <td style={{ maxWidth: '140px' }}>
                        <span style={{
                          overflow: 'hidden', textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap', display: 'block', color: '#94a3b8',
                        }}>
                          {v.uploadedByName || v.uploadedByEmail || '—'}
                        </span>
                      </td>

                      {/* Date */}
                      <td style={{ whiteSpace: 'nowrap', color: '#64748b', fontSize: '12px' }}>
                        {formatDate(v.createdAt)}
                      </td>

                      {/* Status */}
                      <td>
                        {v.isCurrentVersion ? (
                          <span className="cv-version-badge-current">● Active</span>
                        ) : (
                          <span style={{ color: '#334155', fontSize: '11px' }}>—</span>
                        )}
                      </td>

                      {/* Actions */}
                      <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                        {v.isCurrentVersion ? (
                          <span style={{ color: '#334155', fontSize: '12px' }}>Active</span>
                        ) : (
                          <>
                            <button
                              id={`restore-v${v.versionNumber}-btn`}
                              className="cv-version-btn"
                              onClick={() => handleRestore(v.versionNumber)}
                              disabled={isBusy || restoreMutation.isPending || deleteMutation.isPending}
                            >
                              {isBusy && restoreMutation.isPending
                                ? <span className="cv-version-spinner" />
                                : '↩ Restore'}
                            </button>
                            <button
                              id={`delete-v${v.versionNumber}-btn`}
                              className="cv-version-btn-delete"
                              onClick={() => handleDelete(v.versionNumber)}
                              disabled={isBusy || restoreMutation.isPending || deleteMutation.isPending}
                            >
                              {isBusy && deleteMutation.isPending
                                ? <span className="cv-version-spinner" />
                                : '🗑 Delete'}
                            </button>
                          </>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
};

export default FileVersionHistory;
