import { apiUrl } from '../../lib/api-client'

export const settlementApi = {
  get(projectId: string) { return fetch(apiUrl(`/api/v1/projects/${projectId}/settlement`)) },
  requestApproval(projectId: string, payload: unknown) { return fetch(apiUrl(`/api/v1/projects/${projectId}/settlement/request-approval`), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
}
