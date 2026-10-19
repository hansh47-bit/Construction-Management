import { apiUrl } from '../../lib/api-client'
import type { ProjectForm } from '../../types/domain'

export const projectApi = {
  list(keyword = '') {
    const query = keyword.trim() ? `?keyword=${encodeURIComponent(keyword.trim())}` : ''
    return fetch(apiUrl(`/api/v1/projects${query}`))
  },
  create(payload: Omit<ProjectForm, 'target_profit_rate'> & { target_profit_rate: number }) {
    return fetch(apiUrl('/api/v1/projects'), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })
  },
  remove(projectId: string) {
    return fetch(apiUrl(`/api/v1/projects/${projectId}`), { method: 'DELETE' })
  },
  integratedSummary(projectId: string, signal?: AbortSignal) {
    return fetch(apiUrl(`/api/v1/projects/${projectId}/integrated-summary`), { signal })
  },
}
