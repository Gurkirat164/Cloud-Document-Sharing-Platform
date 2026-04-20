import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { resolvePublicShareLink } from '../api/accessApi';

const PublicSharePage = () => {
  const { token } = useParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState(() => (token ? 'loading' : 'error'));
  const [message, setMessage] = useState(() => (token ? 'Preparing your download...' : 'Missing share token.'));
  const [password, setPassword] = useState('');

  useEffect(() => {
    if (!token) {
      return;
    }

    let cancelled = false;

    const openSharedFile = async () => {
      try {
        setStatus('loading');
        setMessage('Resolving shared file...');
        const downloadUrl = await resolvePublicShareLink(token, password || undefined);
        if (cancelled) return;

        setStatus('redirecting');
        setMessage('Opening download...');
        window.location.replace(downloadUrl);
      } catch (error) {
        if (cancelled) return;

        const errorMessage = error.response?.data?.message || error.message || 'Unable to open this share link.';
        setStatus('error');
        setMessage(errorMessage);
      }
    };

    openSharedFile();

    return () => {
      cancelled = true;
    };
  }, [password, token]);

  return (
    <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', background: 'linear-gradient(135deg, #0a0818 0%, #1a1235 40%, #0f1a2e 100%)', color: '#e2e8f0', fontFamily: 'Inter, sans-serif', padding: '24px' }}>
      <div style={{ width: '100%', maxWidth: '520px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: '20px', padding: '28px', boxShadow: '0 30px 80px rgba(0,0,0,0.35)' }}>
        <div style={{ fontSize: '42px', marginBottom: '14px' }}>☁️</div>
        <h1 style={{ margin: '0 0 10px', fontSize: '26px', letterSpacing: '-0.03em' }}>Shared file access</h1>
        <p style={{ margin: '0 0 20px', color: '#94a3b8', lineHeight: 1.6 }}>{message}</p>

        {status === 'error' && (
          <div style={{ display: 'grid', gap: '12px' }}>
            {message.toLowerCase().includes('password') && (
              <label style={{ display: 'grid', gap: '8px', fontSize: '13px', color: '#cbd5e1' }}>
                Password
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter share password"
                  style={{ width: '100%', padding: '12px 14px', borderRadius: '10px', border: '1px solid rgba(255,255,255,0.12)', background: 'rgba(15,23,42,0.8)', color: '#e2e8f0' }}
                />
              </label>
            )}

            <button
              type="button"
              onClick={() => navigate('/dashboard')}
              style={{ width: '100%', padding: '12px 16px', borderRadius: '10px', border: '1px solid rgba(99,102,241,0.35)', background: 'rgba(99,102,241,0.14)', color: '#c7d2fe', fontWeight: 700, cursor: 'pointer' }}
            >
              Go to dashboard
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default PublicSharePage;