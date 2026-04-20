import api from './axiosInstance';

export const getPermissions = async (fileUuid) => {
  const response = await api.get(`/api/files/${fileUuid}/share`);
  return response.data?.data || [];
};

export const grantPermission = async (fileUuid, data) => {
  const response = await api.post(`/api/files/${fileUuid}/share`, data);
  return response.data?.data;
};

export const revokePermission = async (fileUuid, permissionId) => {
  await api.delete(`/api/files/${fileUuid}/share/${permissionId}`);
};

export const getSharedWithMe = async () => {
  const response = await api.get('/api/files/shared-with-me');
  return response.data?.data || [];
};

export const getShareLinks = async (fileUuid) => {
  // If the backend doesn't implement this yet, we just handle the error gracefully elsewhere
  const response = await api.get(`/api/files/${fileUuid}/share-links`);
  return response.data?.data || [];
};

export const createShareLink = async (fileUuid, data) => {
  const response = await api.post(`/api/files/${fileUuid}/share-links`, data);
  return response.data?.data;
};

export const revokeShareLink = async (fileUuid, token) => {
  await api.delete(`/api/files/${fileUuid}/access/token/${token}`);
};

export const resolvePublicShareLink = async (token, password) => {
  const response = await api.get(`/api/public/share-links/${token}`, {
    params: password ? { password } : undefined,
  });
  return response.data?.data;
};
