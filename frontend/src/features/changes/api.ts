import { apiUrl, apiFetch } from '../../lib/api-client'

export function getChangeOrders(projectId: string) {
  return apiFetch(apiUrl(`/api/v1/projects/${projectId}/change-orders`))
}

export const changeApi = {
  create(projectId: string, payload: unknown) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/change-orders`), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
  saveContract(projectId: string, changeOrderId: string, payload: unknown) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/change-orders/${changeOrderId}/contract-changes`), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
  saveBudgetProposal(projectId: string, changeOrderId: string, payload: unknown) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/change-orders/${changeOrderId}/budget-proposals`), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
  requestApproval(projectId: string, changeOrderId: string) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/change-orders/${changeOrderId}/budget-changes/request-approval`), { method: 'POST' }) },
  impact(projectId: string, changeOrderId: string) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/change-orders/${changeOrderId}/impact-summary`)) },
  reconciliation(projectId: string, changeOrderId: string) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/change-orders/${changeOrderId}/reconciliation`)) },
}
