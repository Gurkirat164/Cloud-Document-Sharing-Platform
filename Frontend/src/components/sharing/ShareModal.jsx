import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  getPermissions, 
  grantPermission, 
  revokePermission,
  getShareLinks, 
  createShareLink, 
  revokeShareLink 
} from '../../api/accessApi';
import AccessItem from './AccessItem';
import './ShareModal.css';

const ShareModal = ({ file, onClose }) => {
  const [granteeEmail, setGranteeEmail] = useState('');
  const [permission, setPermission] = useState('VIEW');
  const [localError, setLocalError] = useState('');
  const queryClient = useQueryClient();

  useEffect(() => {
    const handleEsc = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleEsc);
    return () => window.removeEventListener('keydown', handleEsc);
  }, [onClose]);

  const { data: permissions = [], isLoading: isLoadingPermissions } = useQuery({
    queryKey: ['permissions', file.uuid],
    queryFn: () => getPermissions(file.uuid),
  });

  const { data: shareLinks = [], isLoading: isLoadingLinks } = useQuery({
    queryKey: ['shareLinks', file.uuid],
    queryFn: () => getShareLinks(file.uuid).catch(() => []),
  });

  const grantMutation = useMutation({
    mutationFn: (data) => grantPermission(file.uuid, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['permissions', file.uuid] });
      setGranteeEmail('');
      setLocalError('');
    },
    onError: (error) => {
      setLocalError(error.response?.data?.message || 'Failed to grant permission');
    }
  });

  const revokeMutation = useMutation({
    mutationFn: (id) => revokePermission(file.uuid, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['permissions', file.uuid] });
    },
    onError: (error) => {
      setLocalError(error.response?.data?.message || 'Failed to revoke permission');
    }
  });

  const createLinkMutation = useMutation({
    mutationFn: (data) => createShareLink(file.uuid, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shareLinks', file.uuid] });
      setLocalError('');
    },
    onError: (error) => {
      setLocalError(error.response?.data?.message || 'Failed to create share link');
    }
  });

  const revokeLinkMutation = useMutation({
    mutationFn: (token) => revokeShareLink(file.uuid, token),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shareLinks', file.uuid] });
    },
    onError: (error) => {
      setLocalError(error.response?.data?.message || 'Failed to revoke share link');
    }
  });

  const handleGrant = (e) => {
    e.preventDefault();
    if (!granteeEmail) return;
    grantMutation.mutate({ granteeEmail, permission });
  };

  const handleCreateLink = () => {
    createLinkMutation.mutate({ permission: 'VIEW' });
  };

  const handleCopyLink = (token) => {
    const url = `${window.location.origin}/share/${token}`;
    navigator.clipboard.writeText(url).then(() => {
      alert('Link copied to clipboard!');
    });
  };

  return (
    <div className="share-modal-overlay" onClick={onClose}>
      <div className="share-modal-container" onClick={(e) => e.stopPropagation()}>
        <div className="share-modal-header">
          <h3>Share {file.originalName || 'File'}</h3>
          <button className="share-modal-close-btn" onClick={onClose}>&times;</button>
        </div>
        
        <div className="share-modal-body">
          {localError && <div className="error-message">{localError}</div>}
          
          <form className="share-form-group" onSubmit={handleGrant}>
            <input 
              type="email" 
              className="share-input-email" 
              placeholder="User email" 
              value={granteeEmail}
              onChange={(e) => setGranteeEmail(e.target.value)}
              required
            />
            <select 
              className="share-select-permission" 
              value={permission} 
              onChange={(e) => setPermission(e.target.value)}
            >
              <option value="VIEW">View</option>
              <option value="EDIT">Edit</option>
            </select>
            <button 
              type="submit" 
              className="share-btn-submit" 
              disabled={grantMutation.isPending}
            >
              {grantMutation.isPending ? '...' : 'Invite'}
            </button>
          </form>

          <div className="share-section-title">People with access</div>
          {isLoadingPermissions ? (
            <div className="access-list-empty">Loading permissions...</div>
          ) : (
            <div className="access-list">
              {permissions.length === 0 ? (
                <div className="access-list-empty">No one has access yet.</div>
              ) : (
                permissions.map(perm => (
                  <AccessItem 
                    key={perm.id} 
                    permission={perm} 
                    onRevoke={(id) => revokeMutation.mutate(id)} 
                  />
                ))
              )}
            </div>
          )}

          <div className="share-section-title">Public Links</div>
          {isLoadingLinks ? (
            <div className="access-list-empty">Loading links...</div>
          ) : (
            <div>
              {shareLinks.length === 0 ? (
                <button 
                  type="button"
                  className="share-btn-submit" 
                  onClick={handleCreateLink}
                  disabled={createLinkMutation.isPending}
                  style={{ width: '100%' }}
                >
                  Generate Public Link
                </button>
              ) : (
                <div className="access-list">
                  {shareLinks.map(link => (
                    <div key={link.token} className="access-item">
                      <div className="access-info">
                        <div className="access-name">Public Link</div>
                        <div className="access-meta">
                          <span className={`badge badge-${link.permission?.toLowerCase() || 'view'}`}>
                            {link.permission || 'VIEW'}
                          </span>
                        </div>
                      </div>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button 
                          type="button"
                          className="revoke-btn" 
                          style={{ borderColor: '#d1d5db', color: '#374151' }}
                          onClick={() => handleCopyLink(link.token)}
                        >
                          Copy
                        </button>
                        <button 
                          type="button"
                          className="revoke-btn" 
                          onClick={() => revokeLinkMutation.mutate(link.token)}
                        >
                          Revoke
                        </button>
                      </div>
                    </div>
                  ))}
                  <button 
                    type="button"
                    className="share-btn-submit" 
                    onClick={handleCreateLink}
                    disabled={createLinkMutation.isPending}
                    style={{ width: '100%', marginTop: '12px', background: '#f3f4f6', color: '#374151', border: '1px solid #d1d5db' }}
                  >
                    + Generate Another Link
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

export default ShareModal;
