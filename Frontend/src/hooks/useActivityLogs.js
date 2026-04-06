import { useQuery } from '@tanstack/react-query'
import { activityApi } from '../api/activityApi'

// Hook for fetching paginated, filtered activity logs.
export function useActivityLogs(params) {
  return useQuery({
    queryKey: ['activity-logs', params],
    queryFn: () => activityApi.getLogs(params).then((r) => r.data.data),
  })
}
