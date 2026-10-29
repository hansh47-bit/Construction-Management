import assert from 'node:assert/strict'
import test from 'node:test'
import { cleanViewValue, initialViewUrl, updateViewUrl, viewFromUrl } from '../src/lib/workspace-navigation.ts'

test('project, tab, item, approval filters and search survive a page reload', () => {
  const current = updateViewUrl('http://office.test/', 'employee-1', { page: 'projects', project: 'project-1', tab: '업체 비교견적', item: 'EX-002', approvalProject: 'approval-project-2', approvalStatus: 'REQUESTED', search: '현장 A' })
  const restored = initialViewUrl(current.href, 'employee-1', {})
  assert.deepEqual(viewFromUrl(restored), viewFromUrl(current))
  assert.equal(restored.searchParams.get('item'), 'EX-002')
  assert.equal(restored.searchParams.get('approvalProject'), 'approval-project-2')
})

test('the URL is authoritative over an older session preference', () => {
  const restored = initialViewUrl('http://office.test/?viewAccount=user-a&page=approvals&approval=approval-1', 'user-a', { page: 'projects', project: 'old-project' })
  assert.equal(restored.searchParams.get('page'), 'approvals')
  assert.equal(restored.searchParams.get('approval'), 'approval-1')
  assert.equal(restored.searchParams.get('project'), null)
})

test('another account never inherits the previous account navigation', () => {
  const restored = initialViewUrl('http://office.test/?viewAccount=other-user&page=changes&project=private-project&approvalProject=private-project', 'current-user', { page: 'projects', search: 'my-search' })
  assert.equal(restored.searchParams.get('project'), null)
  assert.equal(restored.searchParams.get('approvalProject'), null)
  assert.equal(restored.searchParams.get('page'), 'projects')
  assert.equal(restored.searchParams.get('viewAccount'), 'current-user')
})

test('only navigation preferences, not form data or passwords, enter the URL', () => {
  const updated = updateViewUrl('http://office.test/?tracking=keep', 'user-a', { page: 'projects', password: 'must-not-store', contract_amount: '123', unknown: 'ignored' })
  assert.equal(updated.searchParams.get('password'), null)
  assert.equal(updated.searchParams.get('contract_amount'), null)
  assert.equal(updated.searchParams.get('unknown'), null)
  assert.equal(updated.searchParams.get('tracking'), 'keep')
  assert.equal(cleanViewValue('bad\nvalue'), null)
  assert.equal(cleanViewValue('x'.repeat(201)), null)
})

test('clearing a project or selection removes the URL parameter', () => {
  const updated = updateViewUrl('http://office.test/?project=p1&item=EX-001&approval=a1', 'user-a', { project: null, item: null, approval: null })
  assert.equal(updated.searchParams.has('project'), false)
  assert.equal(updated.searchParams.has('item'), false)
  assert.equal(updated.searchParams.has('approval'), false)
})
