import { apiUrl } from '../../lib/api-client'

export const procurementApi = {
  comparisons(projectId: string, itemId: string) { return fetch(apiUrl(`/api/v1/projects/${projectId}/execution-items/${itemId}/comparisons`)) },
  saveComparisons(projectId: string, itemId: string, payload: unknown) { return fetch(apiUrl(`/api/v1/projects/${projectId}/execution-items/${itemId}/comparisons`), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
  draft(projectId: string, itemId: string) { return fetch(apiUrl(`/api/v1/projects/${projectId}/purchase-orders/draft/${itemId}`)) },
  requestDraft(projectId: string, itemId: string) { return fetch(apiUrl(`/api/v1/projects/${projectId}/purchase-orders/draft/${itemId}/request`), { method: 'POST' }) },
  orders(projectId: string) { return fetch(apiUrl(`/api/v1/projects/${projectId}/purchase-orders`)) },
}
