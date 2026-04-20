import { useQuery } from '@tanstack/react-query'
import { listFiles } from '../api/fileApi'

// Hook for fetching the authenticated user's file list with optional filter params.
export function useFiles(params) {
  return useQuery({
    queryKey: ['files', params],
    queryFn: () => listFiles(params?.page ?? 0, params?.size ?? 20),
  })
}
