import React from 'react';

const AccessItem = ({ permission, onRevoke }) => {
  const { id, granteeName, granteeEmail, permission: permType, expiresAt } = permission;

  return (
    <div className="access-item">
      <div className="access-info">
        <div className="access-name">
          {granteeName} <span className="access-email">({granteeEmail})</span>
        </div>
        <div className="access-meta">
          <span className={`badge badge-${(permType || 'VIEW').toLowerCase()}`}>
            {permType || 'VIEW'}
          </span>
          {expiresAt && (
            <span className="access-expiry">
              Expires: {new Date(expiresAt).toLocaleDateString()}
            </span>
          )}
        </div>
      </div>
      <button 
        className="revoke-btn"
        onClick={() => onRevoke(id)}
      >
        Revoke
      </button>
    </div>
  );
};

export default AccessItem;
