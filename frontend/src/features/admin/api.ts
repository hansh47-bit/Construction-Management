import { apiJson, jsonRequest } from '../../lib/api-client'
import type { Position } from '../auth/auth'
export type Account = { id: string; login_id: string; name: string; account_type: 'ADMIN' | 'USER'; position: Position | null; status: 'ACTIVE' | 'INACTIVE' | 'DELETED'; phone: string; email: string; locked: boolean; last_login_at: string | null; created_at: string; version: number }
export type Page<T> = { content: T[]; totalPages: number; totalElements: number }
export type Permission = { key: string; label: string; allowed: boolean; editable: boolean; dependency: string | null; locked_reason: string | null; version: number }
export type PositionPermissions = { position: Position; permissions: Permission[] }
export type AuditLog = { id: number; actorName: string | null; actorUserId: string | null; actorPosition: string | null; action: string; targetId: string | null; beforeValue: string | null; afterValue: string | null; reason: string | null; result: string; createdAt: string; requestId: string }
export const adminApi = {
 users: (query: URLSearchParams) => apiJson<Page<Account>>('/api/v1/admin/users?' + query),
 create: (data: unknown) => apiJson<Account>('/api/v1/admin/users', jsonRequest('POST', data)),
 update: (id: string, data: unknown) => apiJson<Account>('/api/v1/admin/users/' + id, jsonRequest('PATCH', data)),
 action: (id: string, action: string, data: unknown) => apiJson<Account | void>('/api/v1/admin/users/' + id + (action === 'delete' ? '' : '/' + action), jsonRequest(action === 'delete' ? 'DELETE' : 'POST', data)),
 positions: () => apiJson<PositionPermissions[]>('/api/v1/admin/positions'),
 savePermissions: (position: Position, data: unknown) => apiJson<PositionPermissions>('/api/v1/admin/positions/' + position + '/permissions', jsonRequest('PUT', data)),
 logs: (query: URLSearchParams) => apiJson<Page<AuditLog>>('/api/v1/admin/audit-logs?' + query),
}
