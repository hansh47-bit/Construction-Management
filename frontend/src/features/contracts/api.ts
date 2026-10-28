import { apiUrl, apiFetch } from '../../lib/api-client'

export const contractApi = {
  get(projectId: string) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/contracts`)) },
  save(projectId: string, payload: unknown) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/contracts`), { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
  upload(projectId: string, body: FormData) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/contract-files`), { method: 'POST', body }) },
  deleteFile(projectId: string, fileId: string) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/contract-files/${fileId}`), { method: 'DELETE' }) },
  executiveCorrection(projectId: string, payload: unknown) { return apiFetch(apiUrl(`/api/v1/projects/${projectId}/change-orders/executive-correction`), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
}
