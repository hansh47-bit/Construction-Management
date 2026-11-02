import assert from 'node:assert/strict'
import { beforeEach, test } from 'node:test'

globalThis.window = Object.assign(new EventTarget(), { location: { hostname: 'office.test', origin: 'http://office.test' }, confirm: () => false })
const { apiFetch, clearApiSession, refreshCsrf, setRequestPermissions } = await import('../src/lib/api-client.ts')
const { DATA_CHANGED_EVENT, FEEDBACK_EVENT, hasPendingMutation, mutationFeedback, pendingLabels, userAction } = await import('../src/lib/feedback.ts')
let messages = []
let changes = []
window.addEventListener(FEEDBACK_EVENT, event => messages.push(event.detail))
window.addEventListener(DATA_CHANGED_EVENT, event => changes.push(event.detail))

beforeEach(() => { clearApiSession(); setRequestPermissions(() => true); messages = []; changes = [] })
const csrf = () => Response.json({ token: 'fixture-token', header_name: 'X-CSRF-TOKEN' })
const request = { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ items: [] }) }

test('successful writes emit one completion and refresh event; duplicate clicks share one request', async () => {
  let release
  const gate = new Promise(resolve => { release = resolve })
  let writes = 0
  globalThis.fetch = async (url, init) => {
    if (String(url).endsWith('/auth/csrf')) return csrf()
    writes++
    assert.equal(new Headers(init.headers).get('X-CSRF-TOKEN'), 'fixture-token')
    await gate
    return Response.json({ saved: true })
  }
  const first = apiFetch('/api/v1/projects/p1/contracts', request)
  const second = apiFetch('/api/v1/projects/p1/contracts', request)
  assert.equal(hasPendingMutation(), true)
  release()
  const responses = await Promise.all([first, second])
  assert.deepEqual(await responses[0].json(), { saved: true })
  assert.deepEqual(await responses[1].json(), { saved: true })
  assert.equal(writes, 1)
  assert.equal(messages.length, 1)
  assert.equal(messages[0].kind, 'success')
  assert.equal(messages[0].dialogTitle, '계약 저장 완료')
  assert.equal(changes.length, 1)
  assert.equal(changes[0].projectId, 'p1')
  assert.equal(hasPendingMutation(), false)
})

test('server rejection shows its reason without a false success or refresh event', async () => {
  globalThis.fetch = async url => String(url).endsWith('/auth/csrf') ? csrf() : Response.json({ message: '이미 결재 요청된 내용입니다.' }, { status: 409 })
  const response = await apiFetch('/api/v1/projects/p1/contracts', request)
  assert.equal(response.status, 409)
  assert.equal(messages.length, 1)
  assert.equal(messages[0].kind, 'error')
  assert.match(messages[0].message, /이미 결재 요청/)
  assert.equal(changes.length, 0)
  assert.equal(hasPendingMutation(), false)
})

test('CSRF recovery retries once without duplicate completion messages', async () => {
  let writes = 0
  let tokens = 0
  globalThis.fetch = async url => {
    if (String(url).endsWith('/auth/csrf')) { tokens++; return csrf() }
    writes++
    return writes === 1 ? Response.json({ code: 'CSRF_INVALID' }, { status: 403 }) : Response.json({ saved: true })
  }
  assert.equal((await apiFetch('/api/v1/projects/p1/contracts', request)).status, 200)
  assert.equal(writes, 2)
  assert.equal(tokens, 2)
  assert.equal(messages.length, 1)
  assert.equal(changes.length, 1)
})

test('a response from a previous login cannot update the new session or show success', async () => {
  let release
  let entered
  const gate = new Promise(resolve => { release = resolve })
  const started = new Promise(resolve => { entered = resolve })
  globalThis.fetch = async url => {
    if (String(url).endsWith('/auth/csrf')) return csrf()
    entered()
    await gate
    return Response.json({ saved: true })
  }
  const operation = apiFetch('/api/v1/projects/p1/contracts', request)
  await started
  clearApiSession()
  release()
  await assert.rejects(operation, { name: 'AbortError' })
  assert.equal(messages.length, 0)
  assert.equal(changes.length, 0)
  assert.equal(pendingLabels().length, 0)
})

test('network failures release the busy state and warn that the result must be checked', async () => {
  globalThis.fetch = async url => { if (String(url).endsWith('/auth/csrf')) return csrf(); throw new TypeError('network down') }
  const response = await apiFetch('/api/v1/projects/p1/contracts', request)
  assert.equal(response.status, 503)
  assert.equal(messages[0].kind, 'error')
  assert.match(messages[0].message, /처리 여부를 확인/)
  assert.equal(changes.length, 0)
  assert.equal(hasPendingMutation(), false)
})

test('front-end permissions still block an unauthorized write before network access', async () => {
  setRequestPermissions(() => false)
  let calls = 0
  globalThis.fetch = async () => { calls++; return csrf() }
  assert.equal((await apiFetch('/api/v1/projects/p1/contracts', request)).status, 403)
  assert.equal(calls, 0)
  assert.equal(changes.length, 0)
  assert.equal(messages[0].kind, 'error')
})

test('an action stays busy through its follow-up refresh and duplicate clicks do not rerun it', async () => {
  let release
  let calls = 0
  const gate = new Promise(resolve => { release = resolve })
  const save = userAction('fixture-save', async () => { calls++; await gate; return true }, false)
  const first = save()
  const second = save()
  assert.equal(hasPendingMutation(), true)
  release()
  assert.deepEqual(await Promise.all([first, second]), [true, true])
  assert.equal(calls, 1)
  assert.equal(hasPendingMutation(), false)
})

test('authentication and reads do not masquerade as business-data changes', () => {
  assert.equal(mutationFeedback('/api/v1/auth/login', 'POST'), null)
  assert.equal(mutationFeedback('/api/v1/projects/p1/contracts', 'GET'), null)
  assert.match(mutationFeedback('/api/v1/approvals/a1/approve', 'POST').success, /결재 승인/)
  assert.match(mutationFeedback('/api/v1/projects/p1/progress-claims/c1/payments', 'POST').success, /실제 지급/)
})

test('save and submission routes request acknowledgement dialogs, while attachments do not', () => {
  for (const path of [
    '/api/v1/projects/p1/contracts',
    '/api/v1/projects/p1/execution-budget-details',
    '/api/v1/projects/p1/execution-items/EX-001/comparisons',
    '/api/v1/projects/p1/revisions/r1/contract',
    '/api/v1/projects/p1/daily-logs/d1',
    '/api/v1/projects/p1/meeting-minutes/m1',
  ]) assert.match(mutationFeedback(path, 'PUT').dialogTitle, /저장 완료$/)
  for (const path of [
    '/api/v1/projects/p1/execution-budget-details/request-approval',
    '/api/v1/projects/p1/purchase-orders/draft/EX-001/request',
    '/api/v1/projects/p1/revisions/r1/request-approval',
    '/api/v1/projects/p1/settlement/request-approval',
    '/api/v1/projects/p1/payment-requests',
  ]) assert.match(mutationFeedback(path, 'POST').dialogTitle, /요청 완료$/)
  assert.equal(mutationFeedback('/api/v1/projects/p1/contract-files', 'POST').dialogTitle, undefined)
  assert.equal(mutationFeedback('/api/v1/projects/p1/contracts', 'DELETE').dialogTitle, undefined)
})

test('an internal autosave refreshes data without an extra success dialog, but still reports errors', async () => {
  let rejected = false
  globalThis.fetch = async url => String(url).endsWith('/auth/csrf') ? csrf() : rejected ? Response.json({ message: '비교견적 저장 실패' }, { status: 409 }) : Response.json({ saved: true })
  const path = '/api/v1/projects/p1/execution-items/EX-001/comparisons'
  assert.equal((await apiFetch(path, request, true, { success: 'none' })).status, 200)
  assert.equal(messages.length, 0)
  assert.equal(changes.length, 1)
  rejected = true
  assert.equal((await apiFetch(path, request, true, { success: 'none' })).status, 409)
  assert.equal(messages.length, 1)
  assert.equal(messages[0].kind, 'error')
  assert.equal(messages[0].dialogTitle, undefined)
  assert.equal(changes.length, 1)
})

test('a previous account action cannot lock or clear a new account action with the same label', async () => {
  let releaseOld
  let releaseNew
  let newCalls = 0
  const oldGate = new Promise(resolve => { releaseOld = resolve })
  const newGate = new Promise(resolve => { releaseNew = resolve })
  const oldTask = userAction('shared-label', async () => { await oldGate; return true }, false)()
  clearApiSession()
  assert.equal(hasPendingMutation(), false)
  const newAction = userAction('shared-label', async () => { newCalls++; await newGate; return true }, false)
  const newTask = newAction()
  releaseOld()
  await oldTask
  assert.equal(hasPendingMutation(), true)
  const duplicate = newAction()
  releaseNew()
  await Promise.all([newTask, duplicate])
  assert.equal(newCalls, 1)
  assert.equal(hasPendingMutation(), false)
})

test('a late CSRF response from the old account neither replaces nor unlocks the new token request', async () => {
  let releaseOld
  let releaseNew
  let tokenCalls = 0
  const oldGate = new Promise(resolve => { releaseOld = resolve })
  const newGate = new Promise(resolve => { releaseNew = resolve })
  globalThis.fetch = async (url, init) => {
    if (String(url).endsWith('/auth/csrf')) {
      const count = ++tokenCalls
      await (count === 1 ? oldGate : newGate)
      return Response.json({ token: count === 1 ? 'old-token' : 'new-token', header_name: 'X-CSRF-TOKEN' })
    }
    assert.equal(new Headers(init.headers).get('X-CSRF-TOKEN'), 'new-token')
    return Response.json({ saved: true })
  }
  const oldRequest = refreshCsrf()
  clearApiSession()
  const newRequest = refreshCsrf()
  releaseOld()
  await assert.rejects(oldRequest, { name: 'AbortError' })
  const duplicate = refreshCsrf()
  releaseNew()
  await Promise.all([newRequest, duplicate])
  assert.equal(tokenCalls, 2)
  assert.equal((await apiFetch('/api/v1/projects/p1/contracts', request)).status, 200)
  assert.equal(tokenCalls, 2)
})
