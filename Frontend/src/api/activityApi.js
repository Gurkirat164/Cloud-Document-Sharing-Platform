import axiosInstance from './axiosInstance';

/** All activity-log related API calls. */

/**
 * Fetch a paginated, filtered list of activity logs.
 *
 * @param {Object} params
 * @param {string}  [params.fileUuid]  - filter by specific file
 * @param {string}  [params.eventType] - filter by event type enum value
 * @param {string}  [params.startDate] - ISO date string (inclusive lower bound)
 * @param {string}  [params.endDate]   - ISO date string (inclusive upper bound)
 * @param {number}  [params.page=0]    - zero-based page index
 * @param {number}  [params.size=20]   - page size
 * @returns {Promise<{ content: Array, page, size, totalElements, totalPages, last }>}
 */
export const getActivityLogs = async (params = {}) => {
  const response = await axiosInstance.get('/api/activity/me', { params });
  // Backend wraps data in ApiResponse<List<ActivityLogResponse>>
  return response.data?.data ?? [];
};

/**
 * Fetch the full activity trail for a single file.
 * Only the file owner may call this — the backend returns 403 otherwise.
 *
 * @param {string} fileUuid - public UUID of the file
 * @returns {Promise<Array<ActivityLogResponse>>}
 */
export const getFileActivity = async (fileUuid) => {
  const response = await axiosInstance.get(`/api/files/${fileUuid}/activity`);
  return response.data?.data ?? [];
};

/**
 * Trigger a CSV export of activity logs and download it in the browser.
 * Returns the raw Blob so the caller can create an object URL.
 *
 * @param {Object} params - same filter params as getActivityLogs
 * @returns {Promise<Blob>}
 */
export const exportActivityCsv = async (params = {}) => {
  const response = await axiosInstance.get('/api/activity/export', {
    params,
    responseType: 'blob',
  });
  return response.data; // Blob
};
