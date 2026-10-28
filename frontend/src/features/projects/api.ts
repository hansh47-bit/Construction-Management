import { apiUrl, apiFetch } from '../../lib/api-client'
import type { ProjectForm } from '../../types/domain'

export const projectApi = {
  list(keyword = '') {
    const query = keyword.trim() ? `?keyword=${encodeURIComponent(keyword.trim())}` : ''
    return apiFetch(apiUrl(`/api/v1/projects${query}`))
  },
  create(payload: Omit<ProjectForm, 'target_profit_rate'> & { target_profit_rate: number }) {
    return apiFetch(apiUrl('/api/v1/projects'), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })
  },
  remove(projectId: string) {
    return apiFetch(apiUrl(`/api/v1/projects/${projectId}`), { method: 'DELETE' })
  },
  startConstruction(projectId: string) {
    return apiFetch(apiUrl(`/api/v1/projects/${projectId}/start-construction`), { method: 'POST' })
  },
  synchronizePreparationStatus(projectId: string) {
    return apiFetch(apiUrl(`/api/v1/projects/${projectId}/synchronize-preparation-status`), { method: 'POST' })
  },
  integratedSummary(projectId: string, signal?: AbortSignal) {
    return apiFetch(apiUrl(`/api/v1/projects/${projectId}/integrated-summary`), { signal })
  },
}
