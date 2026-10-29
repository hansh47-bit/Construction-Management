import assert from 'node:assert/strict'
import { test } from 'node:test'
import { approvalProjectOptions, matchesApproval } from '../src/features/approvals/project-filter.ts'

const project = (project_id, project_name = project_id) => ({ project_id, project_code: `CODE-${project_id}`, project_name })
const approval = (approval_id, project_id, status = 'REQUESTED', approval_type = 'EXECUTION_BUDGET') => ({ ...project(project_id), approval_id, status, approval_type })

test('project, status and type filters combine without changing the underlying approval order', () => {
  const rows = [approval('a1', 'p1'), approval('a2', 'p2'), approval('a3', 'p2', 'APPROVED'), approval('a4', 'p2', 'REQUESTED', 'PAYMENT')]
  assert.deepEqual(rows.filter(row => matchesApproval(row, { projectId: 'p2', status: 'REQUESTED' })).map(row => row.approval_id), ['a2', 'a4'])
  assert.deepEqual(rows.filter(row => matchesApproval(row, { projectId: 'p2', status: 'REQUESTED', type: 'PAYMENT' })).map(row => row.approval_id), ['a4'])
  assert.equal(rows.filter(row => matchesApproval(row, {})).length, 4)
})

test('projects with the same name remain distinct and counts are based on project IDs', () => {
  const options = approvalProjectOptions([project('p1', '같은 현장명'), project('p2', '같은 현장명')], [approval('a1', 'p1'), approval('a2', 'p2'), approval('a3', 'p2')])
  assert.equal(options.find(row => row.project_id === 'p1').count, 1)
  assert.equal(options.find(row => row.project_id === 'p2').count, 2)
  assert.equal(options.length, 2)
})

test('catalog names are retained and projects without matching approvals remain selectable', () => {
  const options = approvalProjectOptions([project('p1', '현재 현장명'), project('p2', '결재 없는 현장')], [approval('a1', 'p1')])
  assert.equal(options.find(row => row.project_id === 'p1').project_name, '현재 현장명')
  assert.equal(options.find(row => row.project_id === 'p2').count, 0)
})

test('visible approvals supply project options even when a project catalog cannot be read', () => {
  const options = approvalProjectOptions([], [approval('a1', 'p1')])
  assert.deepEqual(options, [{ ...project('p1'), count: 1 }])
})

test('an obsolete selection stays an explicit empty selection instead of silently showing other projects', () => {
  const options = approvalProjectOptions([project('p1')], [approval('a1', 'p1')], 'deleted')
  assert.equal(options.find(row => row.project_id === 'deleted').count, 0)
  assert.match(options.find(row => row.project_id === 'deleted').project_name, /조회 자료 없음/)
  assert.equal(matchesApproval(approval('a1', 'p1'), { projectId: 'deleted' }), false)
})
