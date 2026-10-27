import { apiUrl } from '../../lib/api-client'

export const revisionApi = {
  list(projectId: string) { return fetch(apiUrl('/api/v1/projects/' + projectId + '/revisions')) },
  history(projectId: string, revisionId: string) { return fetch(apiUrl('/api/v1/projects/' + projectId + '/revisions/' + revisionId + '/history')) },
  procurementItems(projectId: string) { return fetch(apiUrl('/api/v1/projects/' + projectId + '/revisions/procurement-items')) },
  create(projectId: string, payload: unknown) { return fetch(apiUrl('/api/v1/projects/' + projectId + '/revisions'), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
  saveContract(projectId: string, revisionId: string, payload: unknown) { return fetch(apiUrl('/api/v1/projects/' + projectId + '/revisions/' + revisionId + '/contract'), { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
  saveBudget(projectId: string, revisionId: string, payload: unknown) { return fetch(apiUrl('/api/v1/projects/' + projectId + '/revisions/' + revisionId + '/execution-budget'), { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
  requestApproval(projectId: string, revisionId: string) { return fetch(apiUrl('/api/v1/projects/' + projectId + '/revisions/' + revisionId + '/request-approval'), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ request_user: 'system' }) }) },
}
