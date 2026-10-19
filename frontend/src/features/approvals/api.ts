import { apiUrl } from '../../lib/api-client'

export const approvalApi = {
  list(params: URLSearchParams) { return fetch(apiUrl(`/api/v1/approvals?${params.toString()}`)) },
  detail(approvalId: string) { return fetch(apiUrl(`/api/v1/approvals/${approvalId}`)) },
  decide(approvalId: string, decision: 'approve' | 'reject' | 'cancel', payload: unknown) { return fetch(apiUrl(`/api/v1/approvals/${approvalId}/${decision}`), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
}
