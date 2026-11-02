const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
const openedFromAnotherComputer = !['localhost', '127.0.0.1', '::1'].includes(window.location.hostname)
const configuredLocalApi = /^https?:\/\/(localhost|127\.0\.0\.1)(?::\d+)?$/i.test(configuredApiBaseUrl)

// 외부 Tunnel로 접속한 브라우저에서 localhost는 방문자 자신의 PC를 뜻한다.
// 이 경우 Vite의 /api 프록시를 거쳐, 프로그램을 실행 중인 PC의 백엔드로 연결한다.
const apiBaseUrl = openedFromAnotherComputer && configuredLocalApi ? '' : configuredApiBaseUrl

export function apiUrl(path: string) {
  return `${apiBaseUrl}${path}`
}

let epoch = 0
let sessionRequests = new AbortController()
let requestAllowed: ((key: string) => boolean) | null = null
export class ApiError extends Error {
  readonly status: number
  readonly code?: string
  constructor(status: number, message: string, code?: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}
export function setRequestPermissions(check: ((key: string) => boolean) | null) { requestAllowed = check }
function mutationPermission(input: RequestInfo | URL, method: string): string | null {
 const path = new URL(String(input), window.location.origin).pathname
 if (!path.startsWith('/api/v1/projects') && !path.startsWith('/api/v1/approvals')) return null
 if (path.startsWith('/api/v1/approvals')) return 'APPROVAL_DECIDE'
 if (path === '/api/v1/projects') return 'PROJECT_CREATE'
 if (/^\/api\/v1\/projects\/[^/]+$/.test(path)) return 'PROJECT_DELETE'
 if (/\/(start-construction|synchronize-preparation-status)$/.test(path)) return 'PROJECT_STATUS'
 if (path.includes('/settlement/')) return 'SETTLEMENT_REQUEST'
 if (path.endsWith('/payment-requests')) return 'PAYMENT_REQUEST'
 if (path.endsWith('/payments')) return 'PAYMENT_EXECUTE'
 if (path.includes('/progress-claims')) return 'PROGRESS_WRITE'
 if (path.includes('/revisions') || path.includes('/change-orders')) return path.endsWith('/request-approval') ? 'CHANGE_REQUEST' : path.endsWith('/executive-correction') ? 'EXECUTIVE_CORRECT' : 'CHANGE_WRITE'
 if (path.includes('/daily-logs') || path.includes('/meeting-minutes')) return 'SITE_WRITE'
 if (path.includes('/comparisons')) return 'PROCUREMENT_WRITE'
 if (path.includes('/purchase-orders/draft')) return 'PROCUREMENT_REQUEST'
 if (path.includes('/execution-budget-details')) return path.endsWith('/request-approval') ? 'BUDGET_REQUEST' : 'BUDGET_WRITE'
 if (path.includes('/contracts') || path.includes('/contract-files')) return 'CONTRACT_WRITE'
 return method === 'GET' ? null : 'PROJECT_WRITE_UNKNOWN'
}
export function clearApiSession() {
  requestAllowed = null
  epoch++
  sessionRequests.abort()
  sessionRequests = new AbortController()
}
function requireSession(version: number) {
  if (version !== epoch) throw new DOMException('Session changed', 'AbortError')
}
export async function apiFetch(input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> {
  const requestEpoch = epoch
  const write = !['GET', 'HEAD', 'OPTIONS'].includes((init.method ?? 'GET').toUpperCase())
  const permission = write ? mutationPermission(input, init.method ?? 'GET') : null
  if (permission && requestAllowed && !requestAllowed(permission)) return Response.json({ code: 'FORBIDDEN', message: '이 작업에 대한 권한이 없습니다.' }, { status: 403 })
  requireSession(requestEpoch)
  const signal = init.signal ? AbortSignal.any([init.signal, sessionRequests.signal]) : sessionRequests.signal
  const response = await fetch(input, { ...init, signal, credentials: 'include', cache: 'no-store' })
  requireSession(requestEpoch)
  if (response.status === 401 && !String(input).endsWith('/auth/login') && !String(input).endsWith('/auth/me')) window.dispatchEvent(new Event('auth:expired'))
  if (response.status === 403) {
    const body = await response.clone().json().catch(() => ({})) as { code?: string; message?: string }
    requireSession(requestEpoch)
    window.dispatchEvent(new CustomEvent('auth:forbidden', { detail: body.message ?? '권한이 없습니다.' }))
  }
  return response
}
export async function apiJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  const requestEpoch = epoch
  const response = await apiFetch(apiUrl(path), init)
  if (!response.ok) { const body = await response.json().catch(() => ({})) as { code?: string; message?: string }; throw new ApiError(response.status, body.message ?? `요청 실패 (${response.status})`, body.code) }
  const text = response.status === 204 || response.headers.get('content-length') === '0' ? '' : await response.text()
  requireSession(requestEpoch)
  return text ? JSON.parse(text) as T : undefined as T
}
export function jsonRequest(method: string, data: unknown): RequestInit { return { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) } }
