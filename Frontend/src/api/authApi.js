import axiosInstance from './axiosInstance';

export const register = async (fullName, email, password) => {
  const response = await axiosInstance.post('/auth/register', { fullName, email, password });
  return response.data.data;
};

export const login = async (email, password) => {
  const response = await axiosInstance.post('/auth/login', { email, password });
  return response.data.data;
};

export const refresh = async (refreshToken) => {
  const response = await axiosInstance.post('/auth/refresh', { refreshToken });
  return response.data.data;
};

export const logout = async (refreshToken) => {
  const response = await axiosInstance.post('/auth/logout', { refreshToken });
  return response.data.data;
};
