import { apiUrl, apiFetch } from '../../lib/api-client'

export const executionBudgetApi = {
  get(projectId: string) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/execution-budget-details`)) },
  save(projectId: string, payload: unknown) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/execution-budget-details`), { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
  requestApproval(projectId: string, body: FormData) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/execution-budget-details/request-approval`), { method: 'POST', body }) },
}
