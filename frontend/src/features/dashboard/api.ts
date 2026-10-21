import { apiUrl } from '../../lib/api-client'

export function getExecutiveDashboard() {
  return fetch(apiUrl('/api/dashboard'))
}
