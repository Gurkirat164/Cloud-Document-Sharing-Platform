import './EventBadge.css';

/**
 * Maps an EventType enum value to a CSS badge modifier class and a
 * human-readable label.
 */
const EVENT_META = {
  FILE_UPLOAD:        { cls: 'upload',   label: 'Upload'       },
  FILE_DOWNLOAD:      { cls: 'download', label: 'Download'     },
  FILE_DELETE:        { cls: 'delete',   label: 'Delete'       },
  FILE_RENAME:        { cls: 'rename',   label: 'Rename'       },
  FILE_SHARE:         { cls: 'share',    label: 'Share'        },
  PERMISSION_GRANT:   { cls: 'share',    label: 'Grant'        },
  PERMISSION_REVOKE:  { cls: 'delete',   label: 'Revoke'       },
  USER_LOGIN:         { cls: 'access',   label: 'Login'        },
  USER_LOGOUT:        { cls: 'access',   label: 'Logout'       },
  USER_REGISTER:      { cls: 'access',   label: 'Register'     },
};

const FALLBACK = { cls: 'access', label: 'Event' };

/**
 * Renders a coloured pill badge for an activity event type.
 *
 * @param {{ type: string }} props - `type` is an EventType enum string
 */
const EventBadge = ({ type }) => {
  const { cls, label } = EVENT_META[type] ?? FALLBACK;
  return (
    <span className={`event-badge badge-${cls}`} aria-label={label}>
      {label}
    </span>
  );
};

export default EventBadge;
