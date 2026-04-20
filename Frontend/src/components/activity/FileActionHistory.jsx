import { useQuery } from '@tanstack/react-query';
import { getFileActivity } from '../../api/activityApi';
import EventBadge from './EventBadge';
import './FileActionHistory.css';

/** Event types shown in the condensed file-history panel. */
const VISIBLE_EVENTS = new Set(['FILE_DOWNLOAD', 'FILE_UPLOAD', 'FILE_DELETE', 'PERMISSION_GRANT', 'PERMISSION_REVOKE']);

/** Formats an ISO timestamp compactly (date + time, no seconds). */
const formatCompact = (isoString) => {
  if (!isoString) return '—';
  try {
    return new Intl.DateTimeFormat(undefined, {
      month: 'short',
      day:   '2-digit',
      hour:  '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(new Date(isoString));
  } catch {
    return isoString;
  }
};

/**
 * Condensed activity-history widget for use inside a file details sidebar or modal.
 *
 * Fetches the full activity trail for the given file and renders a compact
 * list, filtered to download and view events only. Designed to be embedded —
 * no page-level chrome included.
 *
 * @param {{ fileUuid: string, maxRows?: number }} props
 */
const FileActionHistory = ({ fileUuid, maxRows = 15 }) => {
  const {
    data: allLogs = [],
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ['fileActivity', fileUuid],
    queryFn: () => getFileActivity(fileUuid),
    enabled: !!fileUuid,
    staleTime: 60_000,
    retry: (failCount, err) => {
      // Don't retry 403 — the user simply doesn't own this file.
      if (err?.response?.status === 403) return false;
      return failCount < 2;
    },
  });

  // Filter to only download/view events and slice to maxRows.
  const filteredLogs = allLogs
    .filter((log) => VISIBLE_EVENTS.has(log.eventType))
    .slice(0, maxRows);

  const is403 = isError && error?.response?.status === 403;

  // ── States ─────────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="fah-container">
        <h4 className="fah-title">Recent Activity</h4>
        <ul className="fah-list" aria-busy="true">
          {Array.from({ length: 4 }).map((_, i) => (
            <li key={i} className="fah-skeleton-row">
              <div className="fah-skeleton-badge" />
              <div className="fah-skeleton-text" />
            </li>
          ))}
        </ul>
      </div>
    );
  }

  if (is403) {
    return (
      <div className="fah-container">
        <h4 className="fah-title">Recent Activity</h4>
        <p className="fah-empty">Access restricted — owner only.</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="fah-container">
        <h4 className="fah-title">Recent Activity</h4>
        <p className="fah-error">Failed to load activity history.</p>
      </div>
    );
  }

  if (filteredLogs.length === 0) {
    return (
      <div className="fah-container">
        <h4 className="fah-title">Recent Activity</h4>
        <p className="fah-empty">No download or view events yet.</p>
      </div>
    );
  }

  // ── Main render ────────────────────────────────────────────────────
  return (
    <div className="fah-container">
      <h4 className="fah-title">
        Recent Activity
        <span className="fah-count">{filteredLogs.length}</span>
      </h4>

      <ul className="fah-list" aria-label="File action history">
        {filteredLogs.map((log) => (
          <li key={log.id} className="fah-row">
            <EventBadge type={log.eventType} />

            <div className="fah-row-body">
              <span className="fah-actor" title={log.userEmail}>
                {log.userEmail ?? 'anonymous'}
              </span>
              {log.ipAddress && (
                <span className="fah-ip">{log.ipAddress}</span>
              )}
            </div>

            <time
              className="fah-time"
              dateTime={log.timestamp}
              title={log.timestamp}
            >
              {formatCompact(log.timestamp)}
            </time>
          </li>
        ))}
      </ul>

      {allLogs.filter((l) => VISIBLE_EVENTS.has(l.eventType)).length > maxRows && (
        <p className="fah-overflow-note">
          Showing first {maxRows} events.
        </p>
      )}
    </div>
  );
};

export default FileActionHistory;
