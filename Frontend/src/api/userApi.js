import apiClient from './apiClient'

// User API calls: get own profile, update profile fields.
export const userApi = {
  getProfile: () => apiClient.get('/users/me'),
  updateProfile: (data) => apiClient.patch('/users/me', data),
}
