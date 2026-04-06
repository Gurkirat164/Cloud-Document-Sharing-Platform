import apiClient from './apiClient'

// File API calls: list files, get presigned upload URL, register metadata, download URL, rename, delete.
export const fileApi = {
  list: (params) => apiClient.get('/files', { params }),
  getUploadUrl: (data) => apiClient.post('/files/upload-url', data),
  registerMetadata: (data) => apiClient.post('/files', data),
  getDownloadUrl: (fileId) => apiClient.get(`/files/${fileId}/download-url`),
  rename: (fileId, name) => apiClient.patch(`/files/${fileId}`, { name }),
  remove: (fileId) => apiClient.delete(`/files/${fileId}`),
}
