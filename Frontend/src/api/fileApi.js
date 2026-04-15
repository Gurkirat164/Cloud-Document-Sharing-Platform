import axiosInstance from './axiosInstance';
import axios from 'axios';

/**
 * Step 1 — Ask the backend for a presigned PUT URL.
 * Returns { presignedUrl, s3Key, expiresInSeconds }
 */
export const getPresignedUploadUrl = async (filename, contentType) => {
  const response = await axiosInstance.get('/api/files/presigned-url', {
    params: { filename, contentType },
  });
  return response.data.data;
};

/**
 * Step 2 — PUT the file directly to S3 using the presigned URL.
 * Uses plain axios (no auth header — S3 rejects extra headers on presigned PUTs).
 * @param {string} presignedUrl  URL returned by the backend
 * @param {File}   file          browser File object
 * @param {function} onProgress  callback(percent: number)
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
 */
export const saveFileMetadata = async ({ originalName, s3Key, mimeType, sizeBytes, checksum }) => {
  const response = await axiosInstance.post('/api/files', {
    originalName,
    s3Key,
    mimeType,
    sizeBytes,
    checksum,
  });
  return response.data.data;
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
 * Convenience helper: runs the full 3-step upload flow.
 * @param {File}     file        browser File object
 * @param {function} onProgress  callback(percent: number)
 * @returns {Promise<FileResponse>}
 */
export const uploadFile = async (file, onProgress) => {
  // 1. Get presigned URL
  const { presignedUrl, s3Key } = await getPresignedUploadUrl(file.name, file.type);

  // 2. Upload directly to S3
  await uploadFileToS3(presignedUrl, file, onProgress);

  // 3. Save metadata to MySQL via backend
  return saveFileMetadata({
    originalName: file.name,
    s3Key,
    mimeType: file.type,
    sizeBytes: file.size,
  });
};
