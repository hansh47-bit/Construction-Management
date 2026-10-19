import { apiUrl } from '../../lib/api-client'

export function getChangeOrders(projectId: string) {
  return fetch(apiUrl(`/api/v1/projects/${projectId}/change-orders`))
}
