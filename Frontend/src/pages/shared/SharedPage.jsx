import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getDownloadUrl } from '../../api/fileApi';
import { getSharedWithMe } from '../../api/accessApi';

const SharedPage = () => {
  const { data: sharedItems = [], isLoading, error, refetch } = useQuery({
    queryKey: ['shared-with-me'],
    queryFn: getSharedWithMe,
  });

  const [localError, setLocalError] = useState('');

  const handleDownload = async (fileUuid) => {
    try {
      setLocalError('');
      const downloadUrl = await getDownloadUrl(fileUuid);
      window.open(downloadUrl, '_blank', 'noopener,noreferrer');
    } catch (err) {
      setLocalError(err.response?.data?.message || err.message || 'Download failed.');
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: 'linear-gradient(135deg,#0a0818 0%,#1a1235 40%,#0f1a2e 100%)', color: '#e2e8f0', fontFamily: 'Inter, sans-serif', padding: '32px 20px' }}>
      <div style={{ maxWidth: '980px', margin: '0 auto' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'end', gap: '16px', marginBottom: '22px', flexWrap: 'wrap' }}>
          <div>
            <p style={{ margin: 0, fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.08em', color: '#818cf8' }}>Access</p>
            <h1 style={{ margin: '6px 0 0', fontSize: '30px', letterSpacing: '-0.03em' }}>Shared with me</h1>
          </div>
          <button
            onClick={() => refetch()}
            style={{ background: 'rgba(99,102,241,0.1)', border: '1px solid rgba(99,102,241,0.25)', color: '#c7d2fe', padding: '10px 14px', borderRadius: '10px', cursor: 'pointer', fontSize: '13px', fontWeight: 600 }}
          >
            Refresh
          </button>
        </div>

        {(error || localError) && (
          <div style={{ marginBottom: '16px', padding: '14px 16px', borderRadius: '12px', background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.25)', color: '#fca5a5' }}>
            {localError || 'Unable to load shared files.'}
          </div>
        )}

        {isLoading ? (
          <div style={{ color: '#94a3b8' }}>Loading shared files...</div>
        ) : sharedItems.length === 0 ? (
          <div style={{ padding: '32px', borderRadius: '16px', border: '1px solid rgba(255,255,255,0.08)', background: 'rgba(255,255,255,0.03)', color: '#94a3b8' }}>
            No files have been shared with you yet.
          </div>
        ) : (
          <div style={{ display: 'grid', gap: '12px' }}>
            {sharedItems.map((item) => (
              <div key={item.id} style={{ display: 'flex', justifyContent: 'space-between', gap: '16px', alignItems: 'center', padding: '16px 18px', borderRadius: '16px', border: '1px solid rgba(255,255,255,0.08)', background: 'rgba(255,255,255,0.04)' }}>
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontSize: '15px', fontWeight: 700, color: '#e2e8f0', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{item.fileName}</div>
                  <div style={{ marginTop: '6px', display: 'flex', gap: '8px', flexWrap: 'wrap', fontSize: '12px', color: '#94a3b8' }}>
                    <span style={{ padding: '2px 8px', borderRadius: '999px', background: 'rgba(99,102,241,0.14)', color: '#c7d2fe', fontWeight: 600 }}>{item.permission}</span>
                    <span>Granted by {item.grantedByEmail || 'unknown'}</span>
                    {item.expiresAt && <span>Expires {new Date(item.expiresAt).toLocaleDateString()}</span>}
                  </div>
                </div>
                <div style={{ display: 'flex', gap: '8px', flexShrink: 0 }}>
                  <button
                    type="button"
                    onClick={() => handleDownload(item.fileUuid)}
                    style={{ background: 'transparent', border: '1px solid rgba(34,197,94,0.35)', color: '#4ade80', padding: '8px 12px', borderRadius: '8px', cursor: 'pointer', fontSize: '12px', fontWeight: 600 }}
                  >
                    Download
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default SharedPage;
