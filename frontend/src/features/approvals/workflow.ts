import type { User } from '../auth/auth'
import type { ApprovalItem } from '../../types/domain'
import { formatWorkflowStatus } from '../../lib/format'

export function canDecideApproval(approval: ApprovalItem | null | undefined, user: User | null, can: (key: string) => boolean) {
  if (!approval || approval.status !== 'REQUESTED' || user?.account_type !== 'USER') return false
  if (approval.approval_stage === 'TEAM_LEADER') {
    return user.position === 'TEAM_LEADER' && approval.team_leader_id === user.id && can('APPROVAL_REVIEW')
  }
  return user.position === 'CEO' && can('APPROVAL_DECIDE')
}

export function approvalStatusLabel(approval: ApprovalItem) {
  if (approval.status === 'REQUESTED') return approval.approval_stage === 'TEAM_LEADER' ? '팀장 결재 대기' : '대표 결재 대기'
  if (approval.status === 'APPROVED') return '최종 승인 · 전체 공유'
  if (approval.status === 'REJECTED') return approval.approval_stage === 'TEAM_LEADER' ? '팀장 반려' : '대표 반려'
  return formatWorkflowStatus(approval.status)
}
