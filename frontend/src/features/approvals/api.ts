import { apiUrl, apiFetch } from '../../lib/api-client'

export const approvalApi = {
  list(params: URLSearchParams) { return apiFetch(apiUrl(`/api/v1/approvals?${params.toString()}`)) },
  detail(approvalId: string) { return apiFetch(apiUrl(`/api/v1/approvals/${approvalId}`)) },
  decide(approvalId: string, decision: 'approve' | 'reject' | 'cancel', payload: unknown) { return apiFetch(apiUrl(`/api/v1/approvals/${approvalId}/${decision}`), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
}
