import axios from 'axios';
import { getAccessToken, getRefreshToken, saveTokens, clearTokens } from '../utils/tokenUtils';

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json'
  },
  timeout: 10000
});

axiosInstance.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

axiosInstance.interceptors.response.use((response) => {
  return response;
}, async (error) => {
  const originalRequest = error.config;
  // Some auth failures are returned as 403 when JWT is invalid/expired.
  if ((error.response?.status === 401 || error.response?.status === 403) && !originalRequest._retry) {
    originalRequest._retry = true;
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
      clearTokens();
      window.location.href = '/login';
      return Promise.reject(error);
    }
    
    try {
      const response = await axios.post(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/auth/refresh`, { refreshToken });
      const authData = response.data?.data;
      const newAccessToken = authData?.accessToken;
      const newRefreshToken = authData?.refreshToken || refreshToken;

      if (!newAccessToken) {
        throw new Error('Refresh response missing accessToken');
      }

      saveTokens(newAccessToken, newRefreshToken);
      
      originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
      return axiosInstance(originalRequest);
    } catch (refreshError) {
      if (import.meta.env.DEV) console.error("Refresh token failed", refreshError);
      clearTokens();
      window.location.href = '/login';
      return Promise.reject(refreshError);
    }
  }
  return Promise.reject(error);
});

export default axiosInstance;
