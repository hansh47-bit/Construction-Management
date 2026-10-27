import { apiUrl } from '../../lib/api-client'

export const paymentApi = {
  status(projectId: string) { return fetch(apiUrl(`/api/v1/projects/${projectId}/progress-payments`)) },
  createClaim(projectId: string, payload: unknown) { return fetch(apiUrl(`/api/v1/projects/${projectId}/progress-claims`), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
  deleteClaim(projectId: string, claimId: string) { return fetch(apiUrl(`/api/v1/projects/${projectId}/progress-claims/${claimId}`), { method: 'DELETE' }) },
  requestPayments(projectId: string, payload: unknown) { return fetch(apiUrl(`/api/v1/projects/${projectId}/payment-requests`), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
}
