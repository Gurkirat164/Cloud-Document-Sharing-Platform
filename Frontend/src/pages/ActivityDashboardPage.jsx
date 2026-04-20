import { useState, useEffect, useCallback, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getActivityLogs, exportActivityCsv } from '../api/activityApi';
import EventBadge from '../components/activity/EventBadge';
import './ActivityDashboard.css';

/** All known EventType values from the backend enum. */
const EVENT_TYPES = [
  'FILE_UPLOAD',
  'FILE_DOWNLOAD',
  'FILE_DELETE',
  'FILE_RENAME',
  'FILE_SHARE',
  'PERMISSION_GRANT',
  'PERMISSION_REVOKE',
  'USER_LOGIN',
  'USER_LOGOUT',
  'USER_REGISTER',
];

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

/** Formats an ISO timestamp string using the browser locale. */
const formatTimestamp = (isoString) => {
  if (!isoString) return '—';
  try {
    return new Intl.DateTimeFormat(undefined, {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    }).format(new Date(isoString));
  } catch {
    return isoString;
  }
};

/** Skeleton rows shown while data is loading. */
const SkeletonRows = ({ count = 8 }) =>
  Array.from({ length: count }).map((_, i) => (
    <tr key={i} className="skeleton-row">
      {Array.from({ length: 6 }).map((__, j) => (
        <td key={j}>
          <div className="skeleton-cell" style={{ width: `${60 + Math.random() * 30}%` }} />
        </td>
      ))}
    </tr>
  ));

// ─────────────────────────────────────────────────────────────────────────────

const ActivityDashboardPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const debounceRef = useRef(null);

  // ── Filter state (mirrored in URL search params) ───────────────────
  const [fileSearch, setFileSearch]   = useState(searchParams.get('fileUuid') ?? '');
  const [eventType,  setEventType]    = useState(searchParams.get('eventType') ?? '');
  const [startDate,  setStartDate]    = useState(searchParams.get('startDate') ?? '');
  const [endDate,    setEndDate]      = useState(searchParams.get('endDate') ?? '');
  const [page,       setPage]         = useState(Number(searchParams.get('page') ?? 0));
  const [pageSize,   setPageSize]     = useState(Number(searchParams.get('size') ?? 20));

  // Debounced fileUuid used as the actual query param (500 ms)
  const [debouncedFileSearch, setDebouncedFileSearch] = useState(fileSearch);

  /** Sync URL search params whenever any filter changes. */
  const syncParams = useCallback((overrides = {}) => {
    const current = {
      ...(debouncedFileSearch && { fileUuid: debouncedFileSearch }),
      ...(eventType           && { eventType }),
      ...(startDate           && { startDate }),
      ...(endDate             && { endDate }),
      page: String(page),
      size: String(pageSize),
      ...overrides,
    };
    setSearchParams(current, { replace: true });
  }, [debouncedFileSearch, eventType, startDate, endDate, page, pageSize, setSearchParams]);

  // Debounce the file search input
  useEffect(() => {
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedFileSearch(fileSearch);
      setPage(0); // reset to first page on new search
    }, 500);
    return () => clearTimeout(debounceRef.current);
  }, [fileSearch]);

  // Sync URL whenever filter state settles
  useEffect(() => {
    syncParams();
  }, [syncParams]);

  // ── Query ──────────────────────────────────────────────────────────
  const queryParams = {
    ...(debouncedFileSearch && { fileUuid: debouncedFileSearch }),
    ...(eventType           && { eventType }),
    ...(startDate           && { startDate }),
    ...(endDate             && { endDate }),
    page,
    size: pageSize,
  };

  const {
    data: logs = [],
    isLoading,
    isError,
    error,
    isFetching,
  } = useQuery({
    queryKey: ['activityLogs', queryParams],
    queryFn: () => getActivityLogs(queryParams),
    staleTime: 30_000,
  });

  // ── Export ─────────────────────────────────────────────────────────
  const [isExporting, setIsExporting] = useState(false);

  const handleExportCsv = async () => {
    setIsExporting(true);
    let objectUrl = null;
    try {
      const blob = await exportActivityCsv(queryParams);
      objectUrl = URL.createObjectURL(blob);

      const anchor = document.createElement('a');
      anchor.href = objectUrl;
      anchor.download = `activity-log-${new Date().toISOString().slice(0, 10)}.csv`;
      document.body.appendChild(anchor);
      anchor.click();
      document.body.removeChild(anchor);
    } catch (err) {
      console.error('CSV export failed:', err);
    } finally {
      // Always revoke the object URL to prevent memory leaks
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
      setIsExporting(false);
    }
  };

  // ── Helpers ────────────────────────────────────────────────────────
  const activeFilterCount = [
    debouncedFileSearch, eventType, startDate, endDate,
  ].filter(Boolean).length;

  const handleClearFilters = () => {
    setFileSearch('');
    setEventType('');
    setStartDate('');
    setEndDate('');
    setPage(0);
  };

  const handlePageSizeChange = (e) => {
    setPageSize(Number(e.target.value));
    setPage(0);
  };

  // Determine pagination state
  // /api/activity/me returns a plain List, not a Page, so we simulate
  // client-side pagination when the backend doesn't inject paging metadata.
  const totalItems  = logs.length;   // full list length (server may return all)
  const totalPages  = Math.max(1, Math.ceil(totalItems / pageSize));
  const pagedLogs   = logs.slice(page * pageSize, page * pageSize + pageSize);
  const startIndex  = totalItems === 0 ? 0 : page * pageSize + 1;
  const endIndex    = Math.min(page * pageSize + pageSize, totalItems);

  // 403 Forbidden handling
  const is403 = isError && error?.response?.status === 403;

  // ── Render ─────────────────────────────────────────────────────────
  return (
    <div className="activity-page">

      {/* ── Header ── */}
      <header className="activity-header">
        <h1 className="activity-title">
          Activity <span>Log</span>
        </h1>
        <button
          className="btn-export"
          onClick={handleExportCsv}
          disabled={isExporting || isLoading}
          aria-label="Export activity log as CSV"
        >
          {isExporting ? (
            <>⏳ Exporting…</>
          ) : (
            <>↓ Export CSV</>
          )}
        </button>
      </header>

      {/* ── Error banner ── */}
      {isError && (
        <div className="activity-error-banner" role="alert">
          <span>⚠</span>
          <span>
            {is403
              ? 'Access denied — you can only view activity logs for your own files.'
              : (error?.response?.data?.message ?? 'Failed to load activity logs. Please try again.')}
          </span>
        </div>
      )}

      {/* ── Filter bar ── */}
      <div className="activity-filters" role="search" aria-label="Activity filters">
        <span className="filter-label">Filters</span>
        {activeFilterCount > 0 && (
          <span className="filter-count-pill" aria-label={`${activeFilterCount} active filters`}>
            {activeFilterCount}
          </span>
        )}

        {/* Event type dropdown */}
        <select
          id="filter-event-type"
          className="filter-select"
          value={eventType}
          onChange={(e) => { setEventType(e.target.value); setPage(0); }}
          aria-label="Filter by event type"
        >
          <option value="">All Events</option>
          {EVENT_TYPES.map((et) => (
            <option key={et} value={et}>{et.replace(/_/g, ' ')}</option>
          ))}
        </select>

        {/* File UUID / name search */}
        <input
          id="filter-file-search"
          type="search"
          className="filter-input"
          placeholder="Search by file UUID…"
          value={fileSearch}
          onChange={(e) => setFileSearch(e.target.value)}
          aria-label="Search by file UUID"
        />

        <div className="filter-divider" aria-hidden="true" />

        {/* Date range */}
        <input
          id="filter-start-date"
          type="date"
          className="filter-date"
          value={startDate}
          onChange={(e) => { setStartDate(e.target.value); setPage(0); }}
          aria-label="Start date"
        />
        <span className="filter-label" aria-hidden="true">→</span>
        <input
          id="filter-end-date"
          type="date"
          className="filter-date"
          value={endDate}
          onChange={(e) => { setEndDate(e.target.value); setPage(0); }}
          aria-label="End date"
        />

        {activeFilterCount > 0 && (
          <button
            className="btn-clear-filters"
            onClick={handleClearFilters}
            aria-label="Clear all filters"
          >
            ✕ Clear
          </button>
        )}
      </div>

      {/* ── Summary bar ── */}
      <div className="activity-summary">
        <span className="summary-count">
          {isLoading ? 'Loading…' : (
            <>
              Showing <strong>{startIndex}–{endIndex}</strong> of{' '}
              <strong>{totalItems}</strong> events
              {isFetching && !isLoading && ' · Refreshing…'}
            </>
          )}
        </span>

        <div className="rows-per-page">
          <label htmlFor="rows-per-page-select">Rows per page:</label>
          <select
            id="rows-per-page-select"
            className="rows-select"
            value={pageSize}
            onChange={handlePageSizeChange}
          >
            {PAGE_SIZE_OPTIONS.map((n) => (
              <option key={n} value={n}>{n}</option>
            ))}
          </select>
        </div>
      </div>

      {/* ── Main content ── */}
      {!isError && (
        <>
          {/* Empty state */}
          {!isLoading && pagedLogs.length === 0 ? (
            <div className="activity-state-container" role="status">
              <div className="state-icon">🔍</div>
              <p className="state-title">No activity found</p>
              <p className="state-subtitle">
                {activeFilterCount > 0
                  ? 'Try adjusting or clearing your filters to see more results.'
                  : 'No activity has been recorded yet. Actions you take on files will appear here.'}
              </p>
              {activeFilterCount > 0 && (
                <button className="btn-export" onClick={handleClearFilters}>
                  Clear filters
                </button>
              )}
            </div>
          ) : (
            /* Table */
            <div className="activity-table-wrapper">
              <table className="activity-table" aria-label="Activity log entries">
                <thead>
                  <tr>
                    <th scope="col">Timestamp</th>
                    <th scope="col">Event</th>
                    <th scope="col">File</th>
                    <th scope="col">Actor</th>
                    <th scope="col">IP Address</th>
                    <th scope="col">Metadata</th>
                  </tr>
                </thead>
                <tbody>
                  {isLoading ? (
                    <SkeletonRows count={pageSize > 10 ? 10 : pageSize} />
                  ) : (
                    pagedLogs.map((log) => (
                      <tr key={log.id}>
                        <td>
                          <span title={log.timestamp}>
                            {formatTimestamp(log.timestamp)}
                          </span>
                        </td>
                        <td>
                          <EventBadge type={log.eventType} />
                        </td>
                        <td>
                          {log.fileName ? (
                            <div>
                              <div>{log.fileName}</div>
                              {log.fileUuid && (
                                <div className="cell-muted" title={log.fileUuid}>
                                  {log.fileUuid.substring(0, 8)}…
                                </div>
                              )}
                            </div>
                          ) : (
                            <span className="cell-muted">—</span>
                          )}
                        </td>
                        <td>
                          <span title={log.userEmail}>{log.userEmail ?? 'anonymous'}</span>
                        </td>
                        <td>
                          <span className="cell-ip">{log.ipAddress ?? '—'}</span>
                        </td>
                        <td>
                          {log.metadata ? (
                            <span
                              className="cell-metadata"
                              title={log.metadata}
                            >
                              {log.metadata}
                            </span>
                          ) : (
                            <span className="cell-muted">—</span>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}

          {/* ── Pagination ── */}
          {!isLoading && totalItems > 0 && (
            <nav className="activity-pagination" aria-label="Activity log pagination">
              <span className="pagination-info">
                Page <strong>{page + 1}</strong> of <strong>{totalPages}</strong>
              </span>

              <div className="pagination-controls">
                <button
                  id="btn-first-page"
                  className="btn-page"
                  onClick={() => setPage(0)}
                  disabled={page === 0}
                  aria-label="First page"
                >
                  «
                </button>
                <button
                  id="btn-prev-page"
                  className="btn-page"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  aria-label="Previous page"
                >
                  ‹ Previous
                </button>

                <span className="page-number-badge">{page + 1}</span>

                <button
                  id="btn-next-page"
                  className="btn-page"
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  aria-label="Next page"
                >
                  Next ›
                </button>
                <button
                  id="btn-last-page"
                  className="btn-page"
                  onClick={() => setPage(totalPages - 1)}
                  disabled={page >= totalPages - 1}
                  aria-label="Last page"
                >
                  »
                </button>
              </div>
            </nav>
          )}
        </>
      )}
    </div>
  );
};

export default ActivityDashboardPage;
