import axiosInstance from './axiosInstance';

export const getMyProfile = async () => {
  const response = await axiosInstance.get('/api/users/me');
  return response.data;
};

export const updateMyProfile = async (fullName) => {
  const response = await axiosInstance.put('/api/users/me', { fullName });
  return response.data;
};
