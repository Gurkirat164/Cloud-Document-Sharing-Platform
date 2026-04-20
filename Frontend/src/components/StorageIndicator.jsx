import { useMemo } from 'react';

/* ─── Helpers ──────────────────────────────────────────────────────────────── */

/**
 * Converts a raw byte count to a human-readable string with 1 decimal place.
 * @param {number} bytes
 * @returns {string}  e.g. "2.4 MB", "1.1 GB"
 */
const formatBytes = (bytes) => {
  if (!bytes && bytes !== 0) return '—';
  if (bytes === 0) return '0 B';
  if (bytes < 1_024) return `${bytes} B`;
  if (bytes < 1_048_576) return `${(bytes / 1_024).toFixed(1)} KB`;
  if (bytes < 1_073_741_824) return `${(bytes / 1_048_576).toFixed(1)} MB`;
  return `${(bytes / 1_073_741_824).toFixed(1)} GB`;
};

/* ─── StorageIndicator ─────────────────────────────────────────────────────── */

/**
 * A storage usage progress bar with colour-coded warnings.
 *
 * Props:
 *   storageUsed   {number}  bytes already consumed
 *   storageQuota  {number}  total allowed bytes
 *   showDetails   {boolean} whether to render the text line (default: true)
 */
const StorageIndicator = ({ storageUsed = 0, storageQuota = 1, showDetails = true }) => {
  // ── Derived values ────────────────────────────────────────────────────────
  const { percentage, barColor, usedFormatted, quotaFormatted } = useMemo(() => {
    const raw = storageQuota > 0 ? (storageUsed / storageQuota) * 100 : 0;
    const pct = Math.min(100, Math.round(raw * 10) / 10);

    let color;
    if (pct >= 90) {
      color = '#ef4444'; // red
    } else if (pct >= 70) {
      color = '#f59e0b'; // amber
    } else {
      color = '#22c55e'; // green
    }

    return {
      percentage:     pct,
      barColor:       color,
      usedFormatted:  formatBytes(storageUsed),
      quotaFormatted: formatBytes(storageQuota),
    };
  }, [storageUsed, storageQuota]);

  // ── Styles ────────────────────────────────────────────────────────────────
  const containerStyle = {
    width: '100%',
    fontFamily: "'Inter', system-ui, sans-serif",
  };

  const trackStyle = {
    width: '100%',
    height: '8px',
    background: 'rgba(255,255,255,0.08)',
    borderRadius: '100px',
    overflow: 'hidden',
  };

  const fillStyle = {
    height: '100%',
    width: `${percentage}%`,
    background: barColor,
    borderRadius: '100px',
    transition: 'width 0.5s cubic-bezier(0.4, 0, 0.2, 1), background 0.3s ease',
  };

  const detailsStyle = {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: '6px',
    fontSize: '12px',
    color: '#94a3b8',
    lineHeight: 1.4,
  };

  const warningStyle = {
    marginTop: '6px',
    fontSize: '12px',
    fontWeight: 600,
    color: '#f87171',
    display: 'flex',
    alignItems: 'center',
    gap: '4px',
  };

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div style={containerStyle} role="progressbar" aria-valuenow={percentage} aria-valuemin={0} aria-valuemax={100}>
      {/* Progress bar track */}
      <div style={trackStyle}>
        <div style={fillStyle} />
      </div>

      {/* Detail text line */}
      {showDetails && (
        <div style={detailsStyle}>
          <span>
            <span style={{ color: '#e2e8f0', fontWeight: 600 }}>{usedFormatted}</span>
            {' of '}
            <span>{quotaFormatted}</span>
            {' used'}
          </span>
          <span style={{ color: barColor, fontWeight: 600 }}>
            {percentage.toFixed(1)}%
          </span>
        </div>
      )}

      {/* Warning banners */}
      {percentage >= 100 && (
        <div style={warningStyle}>
          ❌ Storage full — delete files to upload more
        </div>
      )}
      {percentage >= 90 && percentage < 100 && (
        <div style={warningStyle}>
          ⚠️ Storage almost full
        </div>
      )}
    </div>
  );
};

export default StorageIndicator;
