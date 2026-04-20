import axiosInstance from './axiosInstance';
import axios from 'axios';

/**
 * Step 1 — Ask the backend for a presigned PUT URL.
 *
 * Passes fileSize so the server can perform an early quota pre-check and
 * return 413 PAYLOAD TOO LARGE immediately if the file would exceed the quota,
 * before any bytes are sent to S3.
 *
 * @param {string} filename     original filename
 * @param {string} contentType  MIME type
 * @param {number} fileSize     file size in bytes
 * @returns {Promise<{presignedUrl: string, s3Key: string, expiresInSeconds: number}>}
 * @throws {Error} with message "Not enough storage space. Please delete some files or contact admin." on 413
 */
export const getPresignedUploadUrl = async (filename, contentType, fileSize) => {
  try {
    const response = await axiosInstance.get('/api/files/presigned-url', {
      params: { filename, contentType, fileSize },
    });
    return response.data.data;
  } catch (error) {
    if (error.response?.status === 413) {
      throw new Error(
        'Not enough storage space. Please delete some files or contact admin.'
      );
    }
    throw new Error(
      error.response?.data?.message || error.message || 'Failed to get upload URL'
    );
  }
};

/**
 * Step 2 — PUT the file directly to S3 using the presigned URL.
 * Uses plain axios (no auth header — S3 rejects extra headers on presigned PUTs).
 *
 * @param {string}   presignedUrl  URL returned by the backend
 * @param {File}     file          browser File object
 * @param {function} onProgress    callback(percent: number)
 */
export const uploadFileToS3 = async (presignedUrl, file, onProgress) => {
  await axios.put(presignedUrl, file, {
    headers: { 'Content-Type': file.type },
    onUploadProgress: (event) => {
      if (onProgress && event.total) {
        onProgress(Math.round((event.loaded * 100) / event.total));
      }
    },
  });
};

/**
 * Step 3 — Notify the backend that the upload succeeded and persist metadata.
 * Returns FileResponse { uuid, originalName, mimeType, sizeBytes, s3Key, ownerEmail, uploadedAt }
 *
 * @throws {Error} with message "Storage quota exceeded" on 413
 */
export const saveFileMetadata = async ({ originalName, s3Key, mimeType, sizeBytes, checksum }) => {
  try {
    const response = await axiosInstance.post('/api/files', {
      originalName,
      s3Key,
      mimeType,
      sizeBytes,
      checksum,
    });
    return response.data.data;
  } catch (error) {
    if (error.response?.status === 413) {
      throw new Error(
        'Storage quota exceeded. Please delete some files or contact your administrator.'
      );
    }
    throw new Error(
      error.response?.data?.message || error.message || 'Failed to save file metadata'
    );
  }
};

/**
 * Fetch the authenticated user's file list (paginated).
 * Returns PagedResponse<FileResponse>
 */
export const listFiles = async (page = 0, size = 20) => {
  const response = await axiosInstance.get('/api/files', {
    params: { page, size, sort: 'uploadedAt,desc' },
  });
  return response.data.data;
};

/**
 * Soft-delete a file by its UUID.
 */
export const deleteFile = async (uuid) => {
  const response = await axiosInstance.delete(`/api/files/${uuid}`);
  return response.data;
};

/**
 * Fetch storage usage stats for the authenticated user.
 * Returns { totalFiles, storageUsed, storageQuota, storageUsedPercent, storageUsedFormatted }
 */
export const getFileStats = async () => {
  const response = await axiosInstance.get('/api/files/stats');
  return response.data.data;
};

/**
 * Fetch the 5 most recently uploaded files.
 * Returns FileResponse[]
 */
export const getRecentFiles = async () => {
  const response = await axiosInstance.get('/api/files/recent');
  return response.data.data;
};

/**
 * Convenience helper: runs the full 3-step upload flow, passing fileSize to
 * the presigned URL request for the early quota pre-check.
 *
 * @param {File}     file        browser File object
 * @param {function} onProgress  callback(percent: number)
 * @returns {Promise<FileResponse>}
 */
export const uploadFile = async (file, onProgress) => {
  // 1. Get presigned URL — includes early quota check; throws specific 413 message
  const { presignedUrl, s3Key } = await getPresignedUploadUrl(file.name, file.type, file.size);

  // 2. Upload directly to S3
  await uploadFileToS3(presignedUrl, file, onProgress);

  // 3. Save metadata to MySQL via backend — includes definitive quota check
  return saveFileMetadata({
    originalName: file.name,
    s3Key,
    mimeType: file.type,
    sizeBytes: file.size,
  });
};

// ── Versioning API ──────────────────────────────────────────────────────────

/**
 * Fetches the full version history for a file, newest first.
 * Returns FileVersionResponse[]
 *
 * @param {string} fileUuid  public UUID of the file
 */
export const getVersionHistory = async (fileUuid) => {
  const response = await axiosInstance.get(`/api/files/${fileUuid}/versions`);
  return response.data.data;
};

/**
 * Restores a file to a specific version by creating a new S3 version
 * with the content of the specified historical version.
 * Returns FileVersionResponse of the newly created restore version.
 *
 * @param {string} fileUuid      public UUID of the file
 * @param {number} versionNumber 1-based version number to restore
 */
export const restoreVersion = async (fileUuid, versionNumber) => {
  const response = await axiosInstance.post(
    `/api/files/${fileUuid}/versions/restore`,
    { versionNumber }
  );
  return response.data.data;
};

/**
 * Permanently deletes a specific non-current version from S3 and the DB.
 * Returns void — caller should refetch the version list on success.
 *
 * @param {string} fileUuid      public UUID of the file
 * @param {number} versionNumber 1-based version number to permanently delete
 */
export const deleteVersion = async (fileUuid, versionNumber) => {
  await axiosInstance.delete(`/api/files/${fileUuid}/versions/${versionNumber}`);
};

/**
 * Returns the total number of recorded versions for a file.
 *
 * @param {string} fileUuid  public UUID of the file
 * @returns {Promise<number>} version count
 */
export const getVersionCount = async (fileUuid) => {
  const response = await axiosInstance.get(`/api/files/${fileUuid}/versions/count`);
  return response.data.data;
};
