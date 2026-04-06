import apiClient from './apiClient'

// Activity log API calls: fetch paged activity logs with optional filters.
export const activityApi = {
  getLogs: (params) => apiClient.get('/activity-logs', { params }),
}
