import type { ApprovalItem, Project } from '../../types/domain'

export type ApprovalProject = Pick<Project, 'project_id' | 'project_code' | 'project_name'>
export type ApprovalProjectOption = ApprovalProject & { count: number }
type ApprovalFilters = { projectId?: string; status?: string; type?: string }

export function matchesApproval(approval: ApprovalItem, filters: ApprovalFilters) {
  return (!filters.projectId || approval.project_id === filters.projectId)
    && (!filters.status || approval.status === filters.status)
    && (!filters.type || approval.approval_type === filters.type)
}

export function approvalProjectOptions(projects: ApprovalProject[], approvals: ApprovalItem[], selectedId = ''): ApprovalProjectOption[] {
  const options = new Map<string, ApprovalProjectOption>()
  for (const project of projects) options.set(project.project_id, { ...project, count: 0 })
  for (const approval of approvals) {
    const project = options.get(approval.project_id) ?? {
      project_id: approval.project_id,
      project_code: approval.project_code,
      project_name: approval.project_name,
      count: 0,
    }
    options.set(project.project_id, { ...project, count: project.count + 1 })
  }
  if (selectedId && !options.has(selectedId)) options.set(selectedId, { project_id: selectedId, project_code: '', project_name: '선택한 현장 (조회 자료 없음)', count: 0 })
  return [...options.values()].sort((a, b) => a.project_name.localeCompare(b.project_name, 'ko') || a.project_code.localeCompare(b.project_code) || a.project_id.localeCompare(b.project_id))
}
