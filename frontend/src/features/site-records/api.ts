import { apiUrl } from '../../lib/api-client'

export const siteRecordsApi = {
  dailyLogs(projectId: string) { return fetch(apiUrl(`/api/v1/projects/${projectId}/daily-logs`)) },
  createDailyLog(projectId: string, payload: unknown) { return fetch(apiUrl(`/api/v1/projects/${projectId}/daily-logs`), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
  updateDailyLog(projectId: string, dailyLogId: string, payload: unknown) { return fetch(apiUrl(`/api/v1/projects/${projectId}/daily-logs/${dailyLogId}`), { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
  deleteDailyLog(projectId: string, dailyLogId: string) { return fetch(apiUrl(`/api/v1/projects/${projectId}/daily-logs/${dailyLogId}`), { method: 'DELETE' }) },
  uploadPhoto(projectId: string, dailyLogId: string, body: FormData) { return fetch(apiUrl(`/api/v1/projects/${projectId}/daily-logs/${dailyLogId}/photos`), { method: 'POST', body }) },
  deletePhoto(projectId: string, dailyLogId: string, photoId: string) { return fetch(apiUrl(`/api/v1/projects/${projectId}/daily-logs/${dailyLogId}/photos/${photoId}`), { method: 'DELETE' }) },
  meetingMinutes(projectId: string) { return fetch(apiUrl(`/api/v1/projects/${projectId}/meeting-minutes`)) },
  createMeetingMinutes(projectId: string, payload: unknown) { return fetch(apiUrl(`/api/v1/projects/${projectId}/meeting-minutes`), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
  updateMeetingMinutes(projectId: string, meetingId: string, payload: unknown) { return fetch(apiUrl(`/api/v1/projects/${projectId}/meeting-minutes/${meetingId}`), { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }) },
  deleteMeetingMinutes(projectId: string, meetingId: string) { return fetch(apiUrl(`/api/v1/projects/${projectId}/meeting-minutes/${meetingId}`), { method: 'DELETE' }) },
  uploadAttachment(projectId: string, meetingId: string, body: FormData) { return fetch(apiUrl(`/api/v1/projects/${projectId}/meeting-minutes/${meetingId}/attachments`), { method: 'POST', body }) },
  deleteAttachment(projectId: string, meetingId: string, attachmentId: string) { return fetch(apiUrl(`/api/v1/projects/${projectId}/meeting-minutes/${meetingId}/attachments/${attachmentId}`), { method: 'DELETE' }) },
}
