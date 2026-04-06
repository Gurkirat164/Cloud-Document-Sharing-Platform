import { useQuery } from '@tanstack/react-query'
import { fileApi } from '../api/fileApi'

// Hook for fetching the authenticated user's file list with optional filter params.
export function useFiles(params) {
  return useQuery({
    queryKey: ['files', params],
    queryFn: () => fileApi.list(params).then((r) => r.data.data),
  })
}
