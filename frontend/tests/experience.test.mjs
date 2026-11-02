import assert from 'node:assert/strict'
import { afterEach, beforeEach, test } from 'node:test'
import { JSDOM } from 'jsdom'
import React, { act } from 'react'
import { setImmediate as nextTurn } from 'node:timers/promises'

let dom
let root
let state
let App
let createRoot
let AuthContext
let FeedbackProvider
let WorkspaceProvider
let clearApiSession
let setRequestPermissions
const permissions = ['PROJECT_READ', 'PROJECT_CREATE', 'PROJECT_DELETE', 'CONTRACT_READ', 'CONTRACT_WRITE', 'BUDGET_READ', 'BUDGET_WRITE', 'BUDGET_REQUEST', 'PROCUREMENT_READ', 'PROCUREMENT_WRITE', 'PROCUREMENT_REQUEST', 'APPROVAL_READ', 'APPROVAL_DECIDE', 'CHANGE_READ', 'PROGRESS_READ', 'PROGRESS_WRITE', 'SETTLEMENT_READ', 'SITE_READ']
const user = { id: 'fixture-ceo', login_id: 'fixture', name: '테스트 대표', position: 'CEO', account_type: 'USER', permissions, must_change_password: false }
const can = key => permissions.includes(key)
const zeroAmounts = { initial_amount: 0, change_amount: 0, final_amount: 0 }

function fixtures() {
  const project = { project_id: 'p1', project_code: 'P-FIXTURE-001', project_name: '테스트 현장', status: '계약', target_profit_rate: 20 }
  const contract = { ...project, total_contract_supply_amount: 1000000, current_contract_supply_amount: 1000000, total_vat_amount: 100000, total_contract_with_vat: 1100000, current_contract_with_vat: 1100000, target_cost_limit: 800000, items: [{ item_id: 'i1', work_category: '원래 공종', quoted_amount: 1000000, contract_amount: 1000000, vat_amount: 100000, total_amount: 1100000, note: '' }], files: [] }
  const approval = { approval_id: 'a1', project_id: 'p1', project_code: project.project_code, project_name: project.project_name, approval_type: 'EXECUTION_BUDGET', target_id: 'p1', title: '테스트 실행예산 결재', requested_amount: 800000, previous_amount: 0, variance_amount: 800000, request_user: '테스트 팀장', status: 'REQUESTED', approval_stage: 'CEO', requester_position: 'TEAM_LEADER', requested_at: '2026-10-29T15:00:00', attachments: [], detail: { contract_supply_amount: 1000000, target_cost_limit: 800000, total_detail_amount: 800000, over_target_limit: false, summary_by_item: [], details: [] } }
  return { project, contract, approvals: [approval], writes: [], reads: [], unexpected: [], rejectSave: false, writeGate: null, summaryTotal: 1000000, missingProject: false, budgetStatus: 'APPROVED', budgetDetails: [], loggedIn: true, progressOrders: [] }
}

function progressClaim(degree, claim_amount, approval_status = 'APPROVED', paid_total = 0) {
  return { claim_id: `claim-${degree}`, degree, claim_amount, approval_status, payment_state: approval_status === 'REQUESTED' ? 'REQUESTED' : paid_total >= claim_amount ? 'PAID' : 'WAITING_FOR_PAYMENT', paid_total, remaining_payable: claim_amount - paid_total, payments: [] }
}
function progressOrder(po_id = 'PO-1', claims = []) {
  return updateProgressOrder({ po_id, vendor_name: `테스트 업체 ${po_id}`, execution_item_id: 'EX-001', work_category: '철거', execution_item_name: '철거공사', current_po_amount: 10000000, payment_request_status: 'READY', claims })
}
function updateProgressOrder(order) {
  order.approved_claim_total = order.claims.filter(claim => claim.approval_status === 'APPROVED').reduce((sum, claim) => sum + claim.claim_amount, 0)
  order.actual_paid_total = order.claims.reduce((sum, claim) => sum + claim.paid_total, 0)
  order.unclaimed_amount = order.current_po_amount - order.approved_claim_total
  order.approved_unpaid_amount = order.approved_claim_total - order.actual_paid_total
  order.total_unpaid_balance = order.current_po_amount - order.actual_paid_total
  return order
}
function progressResponse() {
  return { project_id: 'p1', project_code: state.project.project_code, all_payments_approved: false, pending_payment_approval_count: 0, purchaseOrders: state.progressOrders }
}

function approvalProjectFixtures() {
  const second = { ...state.project, project_id: 'p2', project_code: 'P-FIXTURE-002', project_name: '부산 현장' }
  state.projectCatalog = [state.project, second, { ...second, project_id: 'p3', project_code: 'P-FIXTURE-003', project_name: '결재 없는 현장' }]
  const original = state.approvals[0]
  const secondApproval = { ...original, project_id: second.project_id, project_code: second.project_code, project_name: second.project_name }
  state.approvals = [
    original,
    { ...secondApproval, approval_id: 'a2', title: '부산 현장 실행예산 결재' },
    { ...secondApproval, approval_id: 'a3', title: '부산 현장 발주품의', approval_type: 'PURCHASE_ORDER', detail: undefined },
    { ...secondApproval, approval_id: 'a4', title: '부산 현장 승인 문서', status: 'APPROVED', approval_stage: 'COMPLETE' },
  ]
}
function approvalRows() { return [...document.querySelectorAll('table[aria-label="통합결재 목록"] tbody tr')].filter(row => row.querySelector('button')) }

async function fakeFetch(input, init = {}) {
  const url = new URL(String(input), 'http://office.test')
  const path = url.pathname
  const method = init.method ?? 'GET'
  if (path.endsWith('/auth/csrf')) return Response.json({ token: 'fixture-token', header_name: 'X-CSRF-TOKEN' })
  if (path.endsWith('/auth/me')) return state.loggedIn ? Response.json(user) : Response.json({ message: '로그인이 필요합니다.' }, { status: 401 })
  if (path.endsWith('/auth/login')) { state.loggedIn = true; return Response.json(user) }
  if (path.endsWith('/auth/logout')) { state.loggedIn = false; return new Response(null, { status: 204 }) }
  if (method !== 'GET') {
    state.writes.push({ path, method, body: init.body })
    if (state.writeGate) await state.writeGate
    if (path.endsWith('/contracts')) {
      if (state.rejectSave) return Response.json({ message: '다른 사용자가 먼저 처리했습니다.' }, { status: 409 })
      const payload = JSON.parse(init.body)
      state.contract = { ...state.contract, ...payload }
      return Response.json(state.contract)
    }
    if (path.endsWith('/approve')) {
      state.budgetStatus = 'APPROVED'
      const index = state.approvals.findIndex(item => item.approval_id === path.split('/').at(-2))
      state.approvals[index] = { ...state.approvals[index], status: 'APPROVED', approval_stage: 'COMPLETE' }
      return Response.json(state.approvals[index])
    }
    if (path.endsWith('/execution-budget-details/request-approval')) {
      if (state.rejectRequest) return Response.json({ message: '이미 결재 요청된 실행예산입니다.' }, { status: 409 })
      state.budgetStatus = 'REQUESTED'
      return Response.json(state.approvals[0])
    }
    if (path.endsWith('/progress-claims') && method === 'POST') {
      if (state.rejectClaim) return Response.json({ message: '기성 작성 가능 잔액이 변경되었습니다.' }, { status: 409 })
      const payload = JSON.parse(init.body)
      const order = state.progressOrders.find(item => item.po_id === payload.po_id)
      order.claims.push(progressClaim(payload.degree, payload.claim_amount, 'REQUESTED'))
      updateProgressOrder(order)
      return Response.json(progressResponse())
    }
    state.unexpected.push(`${method} ${path}`)
    return Response.json({ message: 'Unexpected fixture write' }, { status: 400 })
  }
  state.reads.push(path)
  if (path === '/api/v1/projects') return Response.json((state.projectCatalog ?? [state.project]).filter(item => !url.searchParams.get('keyword') || item.project_name.includes(url.searchParams.get('keyword'))))
  if (path === '/api/v1/approvals') return Response.json(state.approvals.filter(item => (!url.searchParams.get('status') || item.status === url.searchParams.get('status')) && (!url.searchParams.get('type') || item.approval_type === url.searchParams.get('type')) && (!url.searchParams.get('projectId') || item.project_id === url.searchParams.get('projectId'))))
  if (path.startsWith('/api/v1/approvals/')) {
    const response = Response.json(state.approvals.find(item => item.approval_id === path.split('/').at(-1)))
    const gate = state.detailGate
    state.detailGate = null
    if (gate) await gate
    return response
  }
  if (state.missingProject && path.includes('/projects/p1/')) return Response.json({ message: '현장이 없습니다.' }, { status: 404 })
  if (path.endsWith('/integrated-summary')) {
    const response = Response.json({ project_info: state.project, flow_summary: { current_contract_amount: state.summaryTotal, current_execution_budget: 0, current_purchase_amount: 0, accumulated_completed_amount: 0, actual_paid_amount: 0 }, profit_and_balance: { target_cost_limit: 800000, unpurchased_budget_amount: 0, unpurchased_procurement_budget_amount: 0, unexecuted_direct_settlement_amount: 0, expected_final_cost: 0, expected_profit: state.summaryTotal, expected_profit_rate: 100, unbilled_amount: 0, approved_unpaid_amount: 0, total_unpaid_balance: 0 }, cost_breakdown: [] })
    const gate = state.summaryGate
    state.summaryGate = null
    if (gate) await gate
    return response
  }
  if (path.endsWith('/contracts')) return Response.json(state.contract)
  if (path.endsWith('/access')) return Response.json({ CONTRACT_WRITE: true, BUDGET_WRITE: true, PROCUREMENT_WRITE: true })
  if (path.endsWith('/revisions/procurement-items')) return Response.json({ has_approved_revision: false, item_ids: [] })
  if (path.endsWith('/execution-budget-details')) return Response.json({ ...state.project, contract_supply_amount: 1000000, target_cost_limit: 800000, approval_status: state.budgetStatus, execution_budget_rewrite_available: false, total_detail_amount: 0, over_target_limit: false, summary_by_item: ['EX-001', 'EX-002'].map(budget_item_id => ({ budget_item_id, category_name: budget_item_id, item_name: budget_item_id, approval_status: state.budgetStatus, purchase_order_status: 'DRAFT', procurement_required: true, rolled_up_amount: 0 })), details: state.budgetDetails })
  if (path.endsWith('/comparisons')) return Response.json({ project_id: 'p1', project_code: state.project.project_code, execution_item_id: path.split('/').at(-2), work_category: '목공', execution_item_name: '목공', approved_execution_budget: 0, comparisons: [] })
  if (path.includes('/purchase-orders/draft/')) return Response.json({ project_code: state.project.project_code, execution_item_id: path.split('/').at(-1), work_category: '목공', execution_item_name: '목공', approved_execution_budget: 0, selected_vendor_name: '', proposed_po_amount: 0, budget_variance: 0, variance_type: '', display_color: '', selection_reason: '', approval_status: 'DRAFT' })
  if (path.endsWith('/purchase-orders')) return Response.json({ project_id: 'p1', project_code: state.project.project_code, items: [] })
  if (path.endsWith('/progress-payments')) return Response.json(progressResponse())
  if (path.endsWith('/settlement')) return Response.json({ ...state.project, contract: zeroAmounts, execution_budget: zeroAmounts, purchase_orders: zeroAmounts, settlement_expense: 0, settlement_site_expense: 0, approved_site_expense_amount: 0, settlement_labor_expense: 0, approved_labor_expense_amount: 0, approved_direct_settlement_amount: 0, settlement_tax: 0, payments: { approved_claims_total: 0, actual_paid_total: 0, unpaid_balance: 0, uncollected_receivable: 0 }, profitability: { final_cost: 0, final_profit: 0, profit_margin_rate: 0 }, is_settled: false })
  state.unexpected.push(`GET ${path}`)
  return Response.json({ message: 'Unexpected fixture read' }, { status: 404 })
}

async function flush() { await act(async () => { await nextTurn() }) }
async function until(predicate) {
  for (let i = 0; i < 30; i++) { await flush(); if (predicate()) return }
  assert.fail(`UI condition did not settle. ${document.body.textContent.slice(-2000)}`)
}
function button(text, within = document) { return [...within.querySelectorAll('button')].find(item => item.textContent.trim() === text) }
async function click(element) { assert.ok(element, 'Button must exist'); await act(async () => { element.click() }) }
async function type(element, value) {
  assert.ok(element, 'Input must exist')
  await act(async () => { const prototype = element instanceof window.HTMLTextAreaElement ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype; Object.getOwnPropertyDescriptor(prototype, 'value').set.call(element, value); element.dispatchEvent(new window.Event('input', { bubbles: true })) })
}
async function choose(element, value) {
  assert.ok(element, 'Select must exist')
  await act(async () => { element.value = value; element.dispatchEvent(new window.Event('change', { bubbles: true })) })
}
function claimEntry() { return document.querySelector('section[aria-label="기성금액 작성"]') }
function claimAmount() { return claimEntry()?.querySelector('input[inputmode="numeric"]') }
function entryMetric(label) {
  return [...claimEntry().querySelectorAll('article')].find(article => article.querySelector('p').textContent === label)?.querySelectorAll('p')[1].textContent
}
async function mount(query = '') {
  if (query) window.history.replaceState(null, '', `http://office.test/${query}`)
  const auth = { user, loading: false, notice: '', can, logout: async () => {}, refresh: async () => {}, setPasswordOpen: () => {}, passwordOpen: false }
  root = createRoot(document.getElementById('root'))
  await act(async () => { root.render(React.createElement(AuthContext, { value: auth }, React.createElement(FeedbackProvider, null, React.createElement(WorkspaceProvider, { userId: user.id }, React.createElement(App))))) })
  await flush()
}

beforeEach(async () => {
  dom = new JSDOM('<!doctype html><html lang="ko"><body><div id="root"></div></body></html>', { url: 'http://office.test/', pretendToBeVisual: true })
  for (const key of ['window', 'document', 'HTMLElement', 'HTMLInputElement', 'HTMLDialogElement', 'Event', 'CustomEvent', 'sessionStorage']) Object.defineProperty(globalThis, key, { value: dom.window[key], configurable: true, writable: true })
  Object.defineProperty(globalThis, 'navigator', { value: dom.window.navigator, configurable: true })
  globalThis.IS_REACT_ACT_ENVIRONMENT = true
  window.scrollTo = options => { Object.defineProperty(window, 'scrollY', { value: typeof options === 'object' ? options.top ?? 0 : 0, configurable: true }) }
  window.HTMLElement.prototype.scrollIntoView = () => {}
  if (!window.HTMLDialogElement.prototype.showModal) window.HTMLDialogElement.prototype.showModal = function () { this.open = true }
  window.confirm = () => false
  state = fixtures()
  globalThis.fetch = fakeFetch
  ;({ createRoot } = await import('react-dom/client'))
  ;({ default: App } = await import('../src/App.tsx'))
  ;({ AuthContext } = await import('../src/features/auth/auth.ts'))
  ;({ FeedbackProvider } = await import('../src/components/ui/FeedbackProvider.tsx'))
  ;({ WorkspaceProvider } = await import('../src/components/ui/WorkspaceProvider.tsx'))
  ;({ clearApiSession, setRequestPermissions } = await import('../src/lib/api-client.ts'))
  clearApiSession()
  setRequestPermissions(can)
})
afterEach(async () => {
  if (root) { await act(async () => root.unmount()); root = null }
  dom.window.close()
  assert.deepEqual(state.unexpected, [])
})

test('contract save shows a persistent acknowledgement dialog only after success and refresh', async context => {
  context.mock.timers.enable({ apis: ['setTimeout', 'setInterval'] })
  await mount('?project=p1&tab=고객%20견적·계약%20관리')
  await until(() => [...document.querySelectorAll('input')].some(input => input.value === '원래 공종'))
  const field = [...document.querySelectorAll('input')].find(input => input.value === '원래 공종')
  await type(field, '저장할 공종')
  assert.equal(field.value, '저장할 공종')
  let release
  state.summaryGate = new Promise(resolve => { release = resolve })
  await click(button('계약 저장'))
  await until(() => state.writes.length === 1 && state.summaryGate === null)
  assert.equal(document.querySelector('dialog[open]'), null)
  assert.ok(document.querySelector('fieldset[aria-busy="true"]'))
  release()
  await until(() => document.querySelector('dialog[open]')?.textContent.includes('계약 저장이 완료'))
  const completion = document.querySelector('dialog[open]')
  assert.equal(completion.querySelector('h2').textContent, '계약 저장 완료')
  assert.equal(completion.querySelectorAll('button').length, 1)
  assert.ok(button('확인', completion))
  assert.equal(state.contract.items[0].work_category, '저장할 공종')
  assert.equal(new URL(window.location.href).searchParams.get('project'), 'p1')
  assert.equal(new URL(window.location.href).searchParams.get('tab'), '고객 견적·계약 관리')
  assert.equal(document.querySelector('fieldset[aria-busy="true"]'), null)
  await act(async () => context.mock.timers.tick(10000))
  await flush()
  assert.equal(document.querySelector('dialog[open]'), completion)
  await click(button('계약 저장'))
  assert.equal(state.writes.length, 1)
  await click(button('확인', completion))
  assert.equal(document.querySelector('dialog'), null)
  assert.equal(document.querySelector('fieldset[disabled]'), null)
  assert.equal(state.writes.length, 1)
})

test('other-user changes refresh after eight seconds without overwriting an edited contract', async context => {
  context.mock.timers.enable({ apis: ['setTimeout', 'setInterval'] })
  await mount('?project=p1&tab=고객%20견적·계약%20관리')
  await until(() => [...document.querySelectorAll('input')].some(input => input.value === '원래 공종'))
  const field = [...document.querySelectorAll('input')].find(input => input.value === '원래 공종')
  await type(field, '아직 저장하지 않은 내용')
  state.contract.items[0].work_category = '다른 사람이 저장한 내용'
  state.approvals.push({ ...state.approvals[0], approval_id: 'a2', title: '새 결재' })
  state.summaryTotal = 98765432
  await act(async () => context.mock.timers.tick(8000))
  await until(() => document.body.textContent.includes('98,765,432'))
  assert.equal(field.value, '아직 저장하지 않은 내용')
  assert.ok([...document.querySelectorAll('nav button')].some(item => item.textContent.includes('통합결재함') && item.textContent.includes('2')))
  const unload = new window.Event('beforeunload', { cancelable: true })
  window.dispatchEvent(unload)
  assert.equal(unload.defaultPrevented, true)
})

test('approval confirmation can be cancelled, then succeeds once and keeps the approval page', async () => {
  await mount('?page=approvals')
  await until(() => !!button('상세'))
  await click(button('상세'))
  await until(() => !!button('대표 최종 승인'))
  await click(button('대표 최종 승인'))
  await until(() => !!document.querySelector('dialog[open]'))
  assert.equal(state.writes.length, 0)
  await click(button('취소', document.querySelector('dialog')))
  assert.equal(state.writes.length, 0)
  assert.equal(document.querySelector('dialog'), null)
  await click(button('대표 최종 승인'))
  await until(() => !!document.querySelector('dialog[open]'))
  let release
  state.writeGate = new Promise(resolve => { release = resolve })
  await click(button('승인', document.querySelector('dialog')))
  await until(() => state.writes.length === 1)
  assert.ok(document.querySelector('fieldset[aria-busy="true"][disabled]'))
  release()
  await until(() => document.body.textContent.includes('결재 승인이 완료') && document.body.textContent.includes('표시할 결재 건이 없습니다.'))
  assert.equal(state.writes.length, 1)
  assert.equal(new URL(window.location.href).searchParams.get('page'), 'approvals')
})

test('refresh restores the same project, tab and execution item, and browser back changes the tab', async () => {
  await mount('?project=p1&tab=업체%20비교견적&item=EX-002')
  await until(() => !!document.querySelector('h3') && document.body.textContent.includes('업체 비교견적 및 발주품의'))
  assert.equal(new URL(window.location.href).searchParams.get('item'), 'EX-002')
  await act(async () => root.unmount()); root = null
  await mount()
  await until(() => document.body.textContent.includes('업체 비교견적 및 발주품의'))
  assert.equal(new URL(window.location.href).searchParams.get('project'), 'p1')
  assert.equal(new URL(window.location.href).searchParams.get('item'), 'EX-002')
  await click(button('세부실행예산'))
  await flush()
  assert.equal(new URL(window.location.href).searchParams.get('tab'), '세부실행예산')
  await act(async () => { const url = new URL(window.location.href); url.searchParams.set('tab', '업체 비교견적'); window.history.replaceState(null, '', url); window.dispatchEvent(new window.PopStateEvent('popstate')) })
  await until(() => document.body.textContent.includes('업체 비교견적 및 발주품의'))
})

test('a rejected contract save keeps the draft and shows the server reason, not success', async () => {
  await mount('?project=p1')
  await until(() => [...document.querySelectorAll('input')].some(input => input.value === '원래 공종'))
  const field = [...document.querySelectorAll('input')].find(input => input.value === '원래 공종')
  await type(field, '실패해도 유지할 내용')
  state.rejectSave = true
  await click(button('계약 저장'))
  await until(() => !!document.querySelector('[role="alert"]'))
  assert.match(document.querySelector('[role="alert"]').textContent, /다른 사용자가 먼저 처리/)
  assert.equal(field.value, '실패해도 유지할 내용')
  assert.ok(!document.body.textContent.includes('계약 저장이 완료'))
  assert.equal(document.querySelector('dialog[open]'), null)
})

test('budget submission can be cancelled, then shows one completion dialog after the request succeeds', async () => {
  state.budgetStatus = 'DRAFT'
  state.budgetDetails = [{ detail_id: 'd1', budget_item_id: 'EX-001', category_name: '요청할 공종', item_name: '요청할 공종', cost_type: '외주비', quantity: 1, unit_price: 1000, amount: 1000 }]
  await mount('?project=p1&tab=세부실행예산')
  await until(() => !!button('결재요청') && !button('결재요청').disabled)
  await click(button('결재요청'))
  await until(() => !!document.querySelector('dialog[open]'))
  assert.equal(state.writes.length, 0)
  assert.match(document.querySelector('dialog h2').textContent, /실행예산 결재 요청/)
  await click(button('취소', document.querySelector('dialog')))
  assert.equal(state.writes.length, 0)
  await click(button('결재요청'))
  await until(() => !!document.querySelector('dialog[open]'))
  let release
  state.writeGate = new Promise(resolve => { release = resolve })
  await click(button('결재 요청', document.querySelector('dialog')))
  await until(() => state.writes.length === 1)
  assert.equal(document.querySelector('dialog[open]'), null)
  release()
  await until(() => document.querySelector('dialog[open]')?.textContent.includes('결재 요청이 완료'))
  assert.equal(document.querySelector('dialog').querySelectorAll('button').length, 1)
  await click(button('확인', document.querySelector('dialog')))
  assert.equal(document.querySelector('dialog'), null)
  assert.equal(state.writes.length, 1)
  assert.ok(state.writes[0].path.endsWith('/request-approval'))
  assert.equal(new URL(window.location.href).searchParams.get('tab'), '세부실행예산')
})

test('a failed submission does not display a completion dialog or leave the form locked', async () => {
  state.budgetStatus = 'DRAFT'
  state.budgetDetails = [{ detail_id: 'd1', budget_item_id: 'EX-001', category_name: '요청할 공종', item_name: '요청할 공종', cost_type: '외주비', quantity: 1, unit_price: 1000, amount: 1000 }]
  state.rejectRequest = true
  await mount('?project=p1&tab=세부실행예산')
  await until(() => !!button('결재요청') && !button('결재요청').disabled)
  await click(button('결재요청'))
  await until(() => !!document.querySelector('dialog[open]'))
  await click(button('결재 요청', document.querySelector('dialog')))
  await until(() => !!document.querySelector('[role="alert"]') && !document.querySelector('fieldset[aria-busy="true"]'))
  assert.match(document.querySelector('[role="alert"]').textContent, /이미 결재 요청된 실행예산/)
  assert.equal(document.querySelector('dialog[open]'), null)
  assert.equal(document.querySelector('fieldset[disabled]'), null)
  assert.equal(state.writes.length, 1)
})

test('a slow pre-save summary cannot overwrite the freshly saved server state', async context => {
  context.mock.timers.enable({ apis: ['setTimeout', 'setInterval'] })
  await mount('?project=p1')
  await until(() => [...document.querySelectorAll('input')].some(input => input.value === '원래 공종'))
  await act(async () => context.mock.timers.tick(250))
  await flush()
  let release
  state.summaryGate = new Promise(resolve => { release = resolve })
  await act(async () => context.mock.timers.tick(8000))
  await until(() => state.summaryGate === null)
  await type([...document.querySelectorAll('input')].find(input => input.value === '원래 공종'), '최신 계약 내용')
  state.summaryTotal = 87654321
  await click(button('계약 저장'))
  await until(() => document.body.textContent.includes('87,654,321') && !document.querySelector('fieldset[aria-busy="true"]'))
  release()
  await flush()
  await flush()
  assert.ok(document.body.textContent.includes('87,654,321'))
})

test('a deleted or inaccessible restored project falls back to the list safely', async () => {
  state.missingProject = true
  await mount('?project=p1')
  await until(() => new URL(window.location.href).searchParams.get('project') === null)
  assert.ok(document.body.textContent.includes('프로젝트 목록'))
})

test('returning to the window refreshes immediately and a hidden tab does not poll', async context => {
  context.mock.timers.enable({ apis: ['setTimeout', 'setInterval'] })
  await mount('?page=approvals')
  await until(() => !!button('상세'))
  // Let the initial search debounce settle before counting background polling.
  await act(async () => context.mock.timers.tick(250))
  await flush()
  const initialReads = state.reads.length
  Object.defineProperty(document, 'visibilityState', { value: 'hidden', configurable: true })
  state.approvals.push({ ...state.approvals[0], approval_id: 'a2', title: '복귀 시 새 결재' })
  await act(async () => context.mock.timers.tick(8000))
  await flush()
  assert.equal(state.reads.length, initialReads)
  assert.ok(!document.body.textContent.includes('복귀 시 새 결재'))
  Object.defineProperty(document, 'visibilityState', { value: 'visible', configurable: true })
  await act(async () => { window.dispatchEvent(new window.Event('focus')); context.mock.timers.tick(250) })
  await until(() => document.body.textContent.includes('복귀 시 새 결재'))
})

test('an open approval detail and its attachments refresh instead of retaining the old snapshot', async context => {
  context.mock.timers.enable({ apis: ['setTimeout', 'setInterval'] })
  await mount('?page=approvals&approval=a1')
  await until(() => !!button('대표 최종 승인'))
  state.approvals[0].attachments = [{ attachment_id: 'new-file', file_name: '추가된 견적서.pdf', download_url: '/fixture-only/new-file' }]
  state.approvals[0].detail.total_detail_amount = 7654321
  await act(async () => context.mock.timers.tick(8000))
  await until(() => document.body.textContent.includes('추가된 견적서.pdf'))
  assert.ok(document.body.textContent.includes('7,654,321'))
  assert.equal(new URL(window.location.href).searchParams.get('approval'), 'a1')
})

test('approving a document does not overwrite an unfinished draft kept on another tab', async () => {
  state.budgetStatus = 'DRAFT'
  state.budgetDetails = [{ detail_id: 'd1', budget_item_id: 'EX-001', category_name: '기존 공종', item_name: '기존 공종', cost_type: '외주비', quantity: 1, unit_price: 1000, amount: 1000 }]
  await mount('?project=p1&tab=세부실행예산')
  await until(() => [...document.querySelectorAll('input')].some(input => input.value === '기존 공종'))
  const field = [...document.querySelectorAll('input')].find(input => input.value === '기존 공종')
  await type(field, '저장 전 실행예산 초안')
  await click([...document.querySelectorAll('nav button')].find(item => item.textContent.startsWith('통합결재함')))
  await until(() => !!button('상세'))
  await click(button('상세'))
  await until(() => !!button('대표 최종 승인'))
  await click(button('대표 최종 승인'))
  await until(() => !!document.querySelector('dialog[open]'))
  await click(button('승인', document.querySelector('dialog')))
  await until(() => document.body.textContent.includes('결재 승인이 완료'))
  await click([...document.querySelectorAll('nav button')].find(item => item.textContent === '프로젝트 현황'))
  await until(() => [...document.querySelectorAll('input')].some(input => input.value === '저장 전 실행예산 초안'))
})

test('the actual login and session bootstrap restore the workspace after a reload', async () => {
  state.loggedIn = false
  const { AuthProvider } = await import('../src/features/auth/AuthProvider.tsx')
  const { AuthenticatedApp } = await import('../src/features/auth/AuthenticatedApp.tsx')
  const renderAuthenticated = async () => {
    root = createRoot(document.getElementById('root'))
    await act(async () => root.render(React.createElement(AuthProvider, null, React.createElement(FeedbackProvider, null, React.createElement(AuthenticatedApp)))))
  }
  await renderAuthenticated()
  await until(() => !!button('로그인'))
  await type(document.querySelector('input[autocomplete="username"]'), 'fixture')
  await type(document.querySelector('input[type="password"]'), 'Fixture-Only-Password!')
  await click(button('로그인'))
  await until(() => document.body.textContent.includes('프로젝트 목록'))
  await click(document.querySelector('tbody tr'))
  await until(() => !!button('업체 비교견적'))
  await click(button('업체 비교견적'))
  await until(() => document.body.textContent.includes('업체 비교견적 및 발주품의'))
  await act(async () => root.unmount()); root = null
  await renderAuthenticated()
  await until(() => document.body.textContent.includes('업체 비교견적 및 발주품의'))
  assert.equal(new URL(window.location.href).searchParams.get('project'), 'p1')
  assert.equal(new URL(window.location.href).searchParams.get('tab'), '업체 비교견적')
})

test('partial first and second claims automatically advance the degree and fill only the remaining amount', async () => {
  state.progressOrders = [progressOrder()]
  await mount('?project=p1&tab=기성%20및%20지급%20현황')
  await until(() => !!claimAmount())
  assert.equal(document.querySelector('[aria-label="기성 차수"]').value, '1')
  assert.equal(document.querySelector('[aria-label="기성 차수"]').readOnly, true)
  assert.equal(claimAmount().value, '10,000,000')
  await type(claimAmount(), '4000000')
  await click(button('기성금액 작성'))
  await until(() => !!document.querySelector('dialog[open]'))
  assert.match(document.querySelector('dialog').textContent, /1차 기성 4,000,000원/)
  await click(button('기성 등록', document.querySelector('dialog')))
  await until(() => document.querySelector('dialog[open]')?.textContent.includes('기성 등록이 완료'))
  await click(button('확인', document.querySelector('dialog')))
  assert.equal(document.querySelector('[aria-label="기성 차수"]').value, '2')
  assert.equal(claimAmount().value, '6,000,000')
  assert.equal(entryMetric('기성 작성 가능 잔액'), '6,000,000 원')
  assert.equal(entryMetric('총 지급잔액'), '10,000,000 원')
  assert.deepEqual(JSON.parse(state.writes[0].body), { po_id: 'PO-1', degree: 1, claim_amount: 4000000 })
  await click(button('기성금액 작성'))
  await until(() => !!document.querySelector('dialog[open]'))
  assert.match(document.querySelector('dialog').textContent, /2차 기성 6,000,000원/)
  await click(button('기성 등록', document.querySelector('dialog')))
  await until(() => document.querySelector('dialog[open]')?.textContent.includes('기성 등록이 완료'))
  await click(button('확인', document.querySelector('dialog')))
  assert.deepEqual(JSON.parse(state.writes[1].body), { po_id: 'PO-1', degree: 2, claim_amount: 6000000 })
  assert.equal(document.querySelector('[aria-label="기성 차수"]').value, '3')
  assert.equal(claimAmount().value, '0')
  assert.equal(button('기성금액 작성').disabled, true)
  assert.equal(entryMetric('총 지급잔액'), '10,000,000 원')
  assert.equal(state.writes.length, 2)
})

test('restoring or changing the selected purchase order restores that order’s degree and actual unpaid balance', async () => {
  state.progressOrders = [progressOrder('PO-1', [progressClaim(1, 4000000)]), progressOrder('PO-2', [progressClaim(3, 2000000, 'APPROVED', 1000000)])]
  await mount('?project=p1&tab=기성%20및%20지급%20현황&po=PO-2')
  await until(() => !!claimAmount())
  assert.equal(document.querySelector('[aria-label="기성 차수"]').value, '4')
  assert.equal(claimAmount().value, '8,000,000')
  assert.equal(entryMetric('총 지급잔액'), '9,000,000 원')
  await act(async () => root.unmount()); root = null
  await mount()
  await until(() => !!claimAmount())
  assert.equal(document.querySelector('[aria-label="기성 차수"]').value, '4')
  assert.equal(claimEntry().querySelector('select').value, 'PO-2')
  await choose(claimEntry().querySelector('select'), 'PO-1')
  await until(() => document.querySelector('[aria-label="기성 차수"]').value === '2')
  assert.equal(claimAmount().value, '6,000,000')
  assert.equal(entryMetric('총 지급잔액'), '10,000,000 원')
  assert.equal(state.writes.length, 0)
})

test('background updates advance the degree and balances without overwriting an edited claim amount', async context => {
  context.mock.timers.enable({ apis: ['setTimeout', 'setInterval'] })
  state.progressOrders = [progressOrder('PO-1', [progressClaim(1, 4000000, 'APPROVED', 2000000)])]
  await mount('?project=p1&tab=기성%20및%20지급%20현황')
  await until(() => !!claimAmount())
  assert.equal(entryMetric('기성 작성 가능 잔액'), '6,000,000 원')
  assert.equal(entryMetric('총 지급잔액'), '8,000,000 원')
  await type(claimAmount(), '500000')
  state.progressOrders[0].claims[0].paid_total = 3000000
  state.progressOrders[0].claims.push(progressClaim(2, 1000000, 'REQUESTED'))
  updateProgressOrder(state.progressOrders[0])
  await act(async () => context.mock.timers.tick(8000))
  await until(() => document.querySelector('[aria-label="기성 차수"]').value === '3')
  assert.equal(claimAmount().value, '500,000')
  assert.equal(entryMetric('기성 작성 가능 잔액'), '5,000,000 원')
  assert.equal(entryMetric('총 지급잔액'), '7,000,000 원')
  const unload = new window.Event('beforeunload', { cancelable: true })
  window.dispatchEvent(unload)
  assert.equal(unload.defaultPrevented, true)
  assert.equal(state.writes.length, 0)
})

test('a last-minute change to the available amount is checked before a claim is submitted', async () => {
  state.progressOrders = [progressOrder('PO-1', [progressClaim(1, 4000000)])]
  await mount('?project=p1&tab=기성%20및%20지급%20현황')
  await until(() => !!claimAmount())
  await type(claimAmount(), '5000000')
  state.progressOrders[0].claims.push(progressClaim(2, 2000000, 'REQUESTED'))
  updateProgressOrder(state.progressOrders[0])
  await click(button('기성금액 작성'))
  await until(() => !!document.querySelector('[role="alert"]'))
  assert.equal(document.querySelector('dialog[open]'), null)
  assert.equal(document.querySelector('[aria-label="기성 차수"]').value, '3')
  assert.equal(claimAmount().value, '5,000,000')
  assert.equal(entryMetric('기성 작성 가능 잔액'), '4,000,000 원')
  assert.equal(button('기성금액 작성').disabled, true)
  assert.equal(state.writes.length, 0)
})

test('a rejected claim keeps the entered amount and its automatic next degree without a false completion', async () => {
  state.progressOrders = [progressOrder('PO-1', [progressClaim(1, 4000000)])]
  state.rejectClaim = true
  await mount('?project=p1&tab=기성%20및%20지급%20현황')
  await until(() => !!claimAmount())
  await type(claimAmount(), '1200000')
  await click(button('기성금액 작성'))
  await until(() => !!document.querySelector('dialog[open]'))
  await click(button('기성 등록', document.querySelector('dialog')))
  await until(() => !!document.querySelector('[role="alert"]') && !document.querySelector('fieldset[aria-busy="true"]'))
  assert.equal(claimAmount().value, '1,200,000')
  assert.equal(document.querySelector('[aria-label="기성 차수"]').value, '2')
  assert.equal(document.querySelector('dialog[open]'), null)
  assert.equal(state.writes.length, 1)
})

test('changing the purchase order asks before discarding a manually edited claim amount', async () => {
  state.progressOrders = [progressOrder('PO-1'), progressOrder('PO-2', [progressClaim(1, 3000000)])]
  await mount('?project=p1&tab=기성%20및%20지급%20현황')
  await until(() => !!claimAmount())
  await type(claimAmount(), '1500000')
  await choose(claimEntry().querySelector('select'), 'PO-2')
  await until(() => !!document.querySelector('dialog[open]'))
  await click(button('취소', document.querySelector('dialog')))
  assert.equal(claimEntry().querySelector('select').value, 'PO-1')
  assert.equal(claimAmount().value, '1,500,000')
  await choose(claimEntry().querySelector('select'), 'PO-2')
  await until(() => !!document.querySelector('dialog[open]'))
  await click(button('발주 변경', document.querySelector('dialog')))
  await until(() => document.querySelector('[aria-label="기성 차수"]').value === '2')
  assert.equal(claimAmount().value, '7,000,000')
  assert.equal(new URL(window.location.href).searchParams.get('po'), 'PO-2')
  assert.equal(state.writes.length, 0)
})

test('the inbox combines project, status and type filters independently of the open project and project search', async () => {
  approvalProjectFixtures()
  await mount('?page=approvals&project=p1&search=테스트')
  await until(() => approvalRows().length === 3 && !!document.querySelector('[aria-label="결재 현장 선택"] option[value="p3"]'))
  const filter = document.querySelector('[aria-label="결재 현장 선택"]')
  assert.match(filter.querySelector('option[value="p2"]').textContent, /부산 현장.*2건/)
  assert.match(filter.querySelector('option[value="p1"]').textContent, /1건/)
  assert.match(filter.querySelector('option[value="p3"]').textContent, /0건/)
  await choose(filter, 'p2')
  assert.equal(approvalRows().length, 2)
  assert.ok(approvalRows().every(row => row.textContent.includes('부산 현장')))
  assert.match(document.querySelector('[aria-label="현재 결재 조회 현장"]').textContent, /부산 현장.*2건/)
  assert.equal(new URL(window.location.href).searchParams.get('project'), 'p1')
  assert.equal(new URL(window.location.href).searchParams.get('search'), '테스트')
  await choose(document.querySelector('[aria-label="결재 상태"]'), 'APPROVED')
  await until(() => approvalRows().length === 1 && approvalRows()[0].textContent.includes('부산 현장 승인 문서'))
  assert.equal(filter.value, 'p2')
  await choose(document.querySelector('[aria-label="결재 유형"]'), 'PURCHASE_ORDER')
  await until(() => document.body.textContent.includes('선택한 현장에 표시할 결재 건이 없습니다.'))
  assert.equal(approvalRows().length, 0)
  await click(button('전체 현장 보기'))
  assert.equal(filter.value, '')
  assert.equal(document.querySelector('[aria-label="결재 상태"]').value, 'APPROVED')
  assert.equal(document.querySelector('[aria-label="결재 유형"]').value, 'PURCHASE_ORDER')
  await choose(document.querySelector('[aria-label="결재 상태"]'), '')
  await until(() => approvalRows().length === 1 && approvalRows()[0].textContent.includes('부산 현장 발주품의'))
  assert.equal(state.writes.length, 0)
})

test('a project-filtered approval and detail survive reload and browser navigation', async () => {
  approvalProjectFixtures()
  await mount('?page=approvals&approvalProject=p2&approvalType=EXECUTION_BUDGET&approval=a2')
  await until(() => document.querySelector('aside h4')?.textContent === '부산 현장 실행예산 결재')
  assert.equal(approvalRows().length, 1)
  await act(async () => root.unmount()); root = null
  await mount()
  await until(() => document.querySelector('aside h4')?.textContent === '부산 현장 실행예산 결재' && approvalRows().length === 1)
  assert.equal(document.querySelector('[aria-label="결재 현장 선택"]').value, 'p2')
  assert.equal(document.querySelector('[aria-label="결재 유형"]').value, 'EXECUTION_BUDGET')
  assert.equal(approvalRows()[0].querySelector('td').textContent, '1')
  assert.match(approvalRows()[0].querySelectorAll('td')[1].textContent, /2026-10-29 15:00/)
  await act(async () => { const url = new URL(window.location.href); url.searchParams.set('approvalProject', 'p1'); url.searchParams.delete('approval'); window.history.replaceState(null, '', url); window.dispatchEvent(new window.PopStateEvent('popstate')) })
  await until(() => document.querySelector('[aria-label="결재 현장 선택"]').value === 'p1')
  assert.equal(approvalRows().length, 1)
  assert.match(approvalRows()[0].textContent, /테스트 현장/)
  assert.equal(document.querySelector('aside h4'), null)
  assert.equal(state.writes.length, 0)
})

test('changing projects closes the previous approval detail and ignores its late response', async () => {
  approvalProjectFixtures()
  await mount('?page=approvals&approvalProject=p1&approval=a1')
  await until(() => document.querySelector('aside h4')?.textContent === '테스트 실행예산 결재')
  let release
  state.detailGate = new Promise(resolve => { release = resolve })
  await click(button('상세', approvalRows()[0]))
  await until(() => state.detailGate === null)
  await choose(document.querySelector('[aria-label="결재 현장 선택"]'), 'p2')
  assert.equal(document.querySelector('aside h4'), null)
  assert.equal(new URL(window.location.href).searchParams.get('approval'), null)
  release()
  await flush()
  await flush()
  assert.equal(document.querySelector('aside h4'), null)
  assert.equal(button('대표 최종 승인'), undefined)
  assert.ok(approvalRows().every(row => row.textContent.includes('부산 현장')))
  assert.equal(state.writes.length, 0)
})

test('approving a filtered document affects only that document and retains the selected project', async () => {
  approvalProjectFixtures()
  await mount('?page=approvals&approvalProject=p2')
  await until(() => approvalRows().length === 2)
  await click(button('상세', approvalRows().find(row => row.textContent.includes('부산 현장 실행예산 결재'))))
  await until(() => !!button('대표 최종 승인'))
  await click(button('대표 최종 승인'))
  await until(() => !!document.querySelector('dialog[open]'))
  await click(button('승인', document.querySelector('dialog')))
  await until(() => document.body.textContent.includes('결재 승인이 완료') && approvalRows().length === 1)
  assert.equal(state.approvals.find(item => item.approval_id === 'a1').status, 'REQUESTED')
  assert.equal(state.approvals.find(item => item.approval_id === 'a2').status, 'APPROVED')
  assert.equal(state.writes.length, 1)
  assert.ok(state.writes[0].path.endsWith('/a2/approve'))
  assert.equal(document.querySelector('[aria-label="결재 현장 선택"]').value, 'p2')
  assert.equal(new URL(window.location.href).searchParams.get('approvalProject'), 'p2')
  assert.ok([...document.querySelectorAll('nav button')].some(item => item.textContent.startsWith('통합결재함') && item.textContent.includes('2')))
})

test('automatic refresh keeps the project filter while updating project counts and the global pending badge', async context => {
  context.mock.timers.enable({ apis: ['setTimeout', 'setInterval'] })
  approvalProjectFixtures()
  await mount('?page=approvals&approvalProject=p2')
  await until(() => approvalRows().length === 2)
  state.approvals.push({ ...state.approvals[0], approval_id: 'a5', title: '다른 현장 신규 요청' }, { ...state.approvals[1], approval_id: 'a6', title: '부산 현장 신규 요청' })
  await act(async () => context.mock.timers.tick(8000))
  await until(() => approvalRows().length === 3)
  const filter = document.querySelector('[aria-label="결재 현장 선택"]')
  assert.equal(filter.value, 'p2')
  assert.match(filter.querySelector('option[value=""]').textContent, /5건/)
  assert.match(filter.querySelector('option[value="p2"]').textContent, /3건/)
  assert.ok(!approvalRows().some(row => row.textContent.includes('다른 현장 신규 요청')))
  assert.ok([...document.querySelectorAll('nav button')].some(item => item.textContent.startsWith('통합결재함') && item.textContent.includes('5')))
  assert.equal(state.writes.length, 0)
})

test('an obsolete project filter is explicitly empty and can return to all projects', async () => {
  approvalProjectFixtures()
  await mount('?page=approvals&approvalProject=deleted&approval=a1')
  await until(() => document.body.textContent.includes('선택한 현장에 표시할 결재 건이 없습니다.') && new URL(window.location.href).searchParams.get('approval') === null)
  const filter = document.querySelector('[aria-label="결재 현장 선택"]')
  assert.equal(filter.value, 'deleted')
  assert.match(filter.selectedOptions[0].textContent, /조회 자료 없음/)
  assert.equal(approvalRows().length, 0)
  assert.equal(document.querySelector('aside h4'), null)
  await click(button('전체 현장 보기'))
  await until(() => approvalRows().length === 3)
  assert.equal(filter.value, '')
  assert.equal(state.writes.length, 0)
})

test('changing approval projects asks before discarding a rejection comment', async () => {
  approvalProjectFixtures()
  await mount('?page=approvals&approvalProject=p1&approval=a1')
  await until(() => !!document.querySelector('textarea[placeholder="반려 사유"]'))
  await type(document.querySelector('textarea[placeholder="반려 사유"]'), '서류 확인 필요')
  await choose(document.querySelector('[aria-label="결재 현장 선택"]'), 'p2')
  await until(() => !!document.querySelector('dialog[open]'))
  await click(button('취소', document.querySelector('dialog')))
  assert.equal(document.querySelector('[aria-label="결재 현장 선택"]').value, 'p1')
  assert.equal(document.querySelector('textarea[placeholder="반려 사유"]').value, '서류 확인 필요')
  await choose(document.querySelector('[aria-label="결재 현장 선택"]'), 'p2')
  await until(() => !!document.querySelector('dialog[open]'))
  await click(button('현장 변경', document.querySelector('dialog')))
  await until(() => document.querySelector('[aria-label="결재 현장 선택"]').value === 'p2')
  assert.equal(document.querySelector('aside h4'), null)
  assert.equal(document.querySelector('textarea[placeholder="반려 사유"]'), null)
  assert.equal(state.writes.length, 0)
})
