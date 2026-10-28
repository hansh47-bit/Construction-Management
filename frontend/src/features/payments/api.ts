import { apiUrl, apiFetch } from '../../lib/api-client'

export const paymentApi = {
  createPayment(projectId: string, claimId: string, payload: unknown) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/progress-claims/${claimId}/payments`), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
  status(projectId: string) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/progress-payments`)) },
  createClaim(projectId: string, payload: unknown) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/progress-claims`), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
  deleteClaim(projectId: string, claimId: string) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/progress-claims/${claimId}`), { method: 'DELETE' }) },
  requestPayments(projectId: string, payload: unknown) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/payment-requests`), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
}
