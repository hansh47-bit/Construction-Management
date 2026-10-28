import { apiUrl, apiFetch } from '../../lib/api-client'

export function getExecutiveDashboard() {
  return apiFetch(apiUrl('/api/dashboard'))
}
