// User types
export interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  createdAt: string;
}

export const UserRole = {
  ADMIN: 'ADMIN',
  USER: 'USER',
} as const;

export type UserRole = typeof UserRole[keyof typeof UserRole];

// File types
export interface File {
  id: string;
  name: string;
  size: number;
  type: string;
  uploadedBy: string;
  uploadedAt: string;
  isPublic: boolean;
  permissions: FilePermission[];
  version: number;
  s3Key: string;
}

export interface FilePermission {
  userId: string;
  userName: string;
  userEmail: string;
  level: PermissionLevel;
  grantedAt: string;
  grantedBy: string;
}

export const PermissionLevel = {
  VIEW: 'VIEW',
  DOWNLOAD: 'DOWNLOAD',
  EDIT: 'EDIT',
} as const;

export type PermissionLevel = typeof PermissionLevel[keyof typeof PermissionLevel];

// Activity Log types
export interface ActivityLog {
  id: string;
  action: ActivityAction;
  fileName: string;
  fileId: string;
  performedBy: string;
  performedAt: string;
  details?: string;
}

export const ActivityAction = {
  UPLOAD: 'UPLOAD',
  DOWNLOAD: 'DOWNLOAD',
  DELETE: 'DELETE',
  SHARE: 'SHARE',
  PERMISSION_CHANGE: 'PERMISSION_CHANGE',
  VIEW: 'VIEW',
} as const;

export type ActivityAction = typeof ActivityAction[keyof typeof ActivityAction];

// Auth types
export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterData {
  email: string;
  name: string;
  password: string;
  confirmPassword: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

// API Response types
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
}

// Upload types
export interface UploadProgress {
  fileId: string;
  fileName: string;
  progress: number;
  status: 'uploading' | 'completed' | 'error';
}
