import { announceDataChanged, beginPending, endPending, mutationFeedback, notify, resetFeedbackSession, SESSION_RESET_EVENT } from './feedback.ts'

const configuredApiBaseUrl = import.meta.env?.VITE_API_BASE_URL ?? ''
const openedFromAnotherComputer = !['localhost', '127.0.0.1', '::1'].includes(window.location.hostname)
const configuredLocalApi = /^https?:\/\/(localhost|127\.0\.0\.1)(?::\d+)?$/i.test(configuredApiBaseUrl)

// 외부 Tunnel로 접속한 브라우저에서 localhost는 방문자 자신의 PC를 뜻한다.
// 이 경우 Vite의 /api 프록시를 거쳐, 프로그램을 실행 중인 PC의 백엔드로 연결한다.
const apiBaseUrl = openedFromAnotherComputer && configuredLocalApi ? '' : configuredApiBaseUrl

export function apiUrl(path: string) {
  return `${apiBaseUrl}${path}`
}

let csrf: { token: string; header_name: string } | null = null
let csrfRequest: Promise<void> | null = null
let epoch = 0
let requestAllowed: ((key: string) => boolean) | null = null
export function setRequestPermissions(check: ((key: string) => boolean) | null) { requestAllowed = check }
function mutationPermission(input: RequestInfo | URL, method: string): string | null {
 const path = new URL(String(input), window.location.origin).pathname
 if (!path.startsWith('/api/v1/projects') && !path.startsWith('/api/v1/approvals')) return null
 if (path.startsWith('/api/v1/approvals')) {
  if (/\/(approve|reject)$/.test(path) && requestAllowed?.('APPROVAL_REVIEW')) return 'APPROVAL_REVIEW'
  return 'APPROVAL_DECIDE'
 }
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
export function clearApiSession() { requestAllowed = null; epoch++; csrf = null; csrfRequest = null; resetFeedbackSession(); window.dispatchEvent(new Event(SESSION_RESET_EVENT)) }
export async function refreshCsrf() {
  if (!csrfRequest) {
    const csrfEpoch = epoch
    const operation = (async () => {
    const response = await fetch(apiUrl('/api/v1/auth/csrf'), { credentials: 'include', cache: 'no-store', signal: AbortSignal.timeout(15000) })
    if (!response.ok) throw new Error('보안 토큰을 받지 못했습니다.')
    const next = await response.json()
    if (csrfEpoch !== epoch) throw new DOMException('Session changed', 'AbortError')
    csrf = next
    })()
    csrfRequest = operation
    void operation.finally(() => { if (csrfRequest === operation) csrfRequest = null }).catch(() => {})
  }
  await csrfRequest
}
async function sendRequest(input: RequestInfo | URL, init: RequestInit, retry: boolean, requestEpoch: number): Promise<Response> {
  const write = !['GET', 'HEAD', 'OPTIONS'].includes((init.method ?? 'GET').toUpperCase())
  const permission = write ? mutationPermission(input, init.method ?? 'GET') : null
  if (permission && requestAllowed && !requestAllowed(permission)) return Response.json({ code: 'FORBIDDEN', message: '이 작업에 대한 권한이 없습니다.' }, { status: 403 })
  if (write && !csrf) await refreshCsrf()
  if (requestEpoch !== epoch) throw new DOMException('Session changed', 'AbortError')
  const headers = new Headers(init.headers)
  if (write && csrf) headers.set(csrf.header_name, csrf.token)
  const timeout = AbortSignal.timeout(30000)
  const signal = init.signal ? AbortSignal.any([init.signal, timeout]) : timeout
  const response = await fetch(input, { ...init, signal, headers, credentials: 'include', cache: 'no-store' })
  if (requestEpoch !== epoch) throw new DOMException('Session changed', 'AbortError')
  if (response.status === 401 && !String(input).endsWith('/auth/login') && !String(input).endsWith('/auth/me')) window.dispatchEvent(new Event('auth:expired'))
  if (response.status === 403) {
    const body = await response.clone().json().catch(() => ({})) as { code?: string; message?: string }
    if (body.code === 'CSRF_INVALID' && retry) { await refreshCsrf(); return sendRequest(input, init, false, requestEpoch) }
    window.dispatchEvent(new CustomEvent('auth:forbidden', { detail: body.message ?? '권한이 없습니다.' }))
  }
  return response
}
const inFlightWrites = new Map<string, Promise<Response>>()
function bodyKey(body: BodyInit | null | undefined): string {
  if (typeof body === 'string') return body
  if (body instanceof FormData) return JSON.stringify([...body.entries()].map(([key, value]) => [key, typeof value === 'string' ? value : [value.name, value.size, value.type, value.lastModified]]))
  return body == null ? '' : String(body)
}
export async function apiFetch(input: RequestInfo | URL, init: RequestInit = {}, retry = true, feedbackOptions: { success?: 'default' | 'none' } = {}): Promise<Response> {
  const requestEpoch = epoch
  const method = (init.method ?? 'GET').toUpperCase()
  const write = !['GET', 'HEAD', 'OPTIONS'].includes(method)
  const path = new URL(String(input), window.location.origin).pathname
  const feedback = write ? mutationFeedback(path, method) : null
  const key = write ? `${requestEpoch}:${method}:${String(input)}:${bodyKey(init.body)}` : ''
  if (write && inFlightWrites.has(key)) return (await inFlightWrites.get(key)!).clone()

  async function perform() {
    const pendingId = feedback ? beginPending(feedback.pending) : null
    let changed = false
    try {
      const response = await sendRequest(input, init, retry, requestEpoch)
      if (feedback && response.ok) {
        changed = true
        if (feedbackOptions.success !== 'none') notify(feedback.success, 'success', feedback.dialogTitle)
      } else if (feedback) {
        const body = await response.clone().json().catch(() => ({})) as { message?: string; detail?: string }
        const reason = body.message ?? body.detail ?? `서버 응답: ${response.status}`
        notify(`처리하지 못했습니다.\n${reason}`, 'error')
      }
      return response
    } catch (error) {
      if (requestEpoch !== epoch) throw new DOMException('Session changed', 'AbortError')
      if (error instanceof DOMException && error.name === 'AbortError') throw error
      const message = '서버에 연결하지 못했습니다. 처리 여부를 확인한 뒤 다시 시도해주세요.'
      if (feedback) notify(message, 'error')
      return Response.json({ code: 'NETWORK_ERROR', message }, { status: 503 })
    } finally {
      if (pendingId !== null) endPending(pendingId)
      if (changed) announceDataChanged(path)
    }
  }

  const operation = perform()
  if (write) inFlightWrites.set(key, operation)
  try { return (await operation).clone() }
  finally { if (write && inFlightWrites.get(key) === operation) inFlightWrites.delete(key) }
}
export async function apiJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await apiFetch(apiUrl(path), init)
  if (!response.ok) { const body = await response.json().catch(() => ({})) as { message?: string }; throw new Error(body.message ?? `요청 실패 (${response.status})`) }
  return response.status === 204 || response.headers.get('content-length') === '0' ? undefined as T : response.text().then(t => t ? JSON.parse(t) as T : undefined as T)
}
export function jsonRequest(method: string, data: unknown): RequestInit { return { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) } }
