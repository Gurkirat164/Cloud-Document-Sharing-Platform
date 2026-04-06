import apiClient from './apiClient'

// Access control API calls: grant permission, revoke permission, list permissions, create share link, resolve share link.
export const accessApi = {
  grantPermission: (fileId, data) => apiClient.post(`/files/${fileId}/permissions`, data),
  revokePermission: (fileId, userId) => apiClient.delete(`/files/${fileId}/permissions/${userId}`),
  listPermissions: (fileId) => apiClient.get(`/files/${fileId}/permissions`),
  createShareLink: (data) => apiClient.post('/share-links', data),
  resolveShareLink: (token) => apiClient.get(`/share-links/${token}`),
}
