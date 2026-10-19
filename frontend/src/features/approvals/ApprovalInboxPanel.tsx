import { Download } from 'lucide-react'
import { apiUrl } from '../../lib/api-client'
import { formatDate, formatDateTime, formatWon } from '../../lib/format'
import type { ApprovalItem } from '../../types/domain'

function varianceClass(value: number) {
  if (value > 0) return 'text-rose-700'
  if (value < 0) return 'text-blue-700'
  return 'text-slate-900'
}

export function ApprovalInboxPanel({
  approvals,
  selectedApproval,
  statusFilter,
  typeFilter,
  rejectComment,
  setStatusFilter,
  setTypeFilter,
  setRejectComment,
  loadApprovalDetail,
  approveApproval,
  rejectApproval,
  cancelApproval,
}: {
  approvals: ApprovalItem[]
  selectedApproval: ApprovalItem | null
  statusFilter: string
  typeFilter: string
  rejectComment: string
  setStatusFilter: (value: string) => void
  setTypeFilter: (value: string) => void
  setRejectComment: (value: string) => void
  loadApprovalDetail: (approvalId: string) => void
  approveApproval: (approvalId: string) => void
  rejectApproval: (approvalId: string) => void
  cancelApproval: (approvalId: string) => void
}) {
  const pending = selectedApproval?.status === 'REQUESTED'
  const canCancelApprovedExecutionBudget = selectedApproval?.status === 'APPROVED'
    && selectedApproval.approval_type === 'EXECUTION_BUDGET'

  return (
    <div>
      <div className="flex flex-col gap-4 border-b border-slate-200 pb-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 className="text-lg font-semibold text-slate-950">통합결재함</h3>
          <p className="mt-1 text-sm text-slate-500">실행예산과 발주품의 결재를 한 화면에서 승인하거나 반려합니다.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)} className="input w-36">
            <option value="">전체 상태</option>
            <option value="REQUESTED">결재대기</option>
            <option value="APPROVED">승인</option>
            <option value="REJECTED">반려</option>
            <option value="CANCELLED">승인취소</option>
          </select>
          <select value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)} className="input w-44">
            <option value="">전체 유형</option>
            <option value="EXECUTION_BUDGET">실행예산</option>
            <option value="PURCHASE_ORDER">발주</option>
          </select>
        </div>
      </div>

      <div className="grid gap-5 py-5 xl:grid-cols-[minmax(0,1.4fr)_minmax(360px,0.8fr)]">
        <div className="overflow-x-auto">
          <table className="min-w-[980px] text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-3 py-3">요청일</th>
                <th className="px-3 py-3">프로젝트</th>
                <th className="px-3 py-3">유형</th>
                <th className="px-3 py-3">제목</th>
                <th className="px-3 py-3">요청금액</th>
                <th className="px-3 py-3">증감</th>
                <th className="px-3 py-3">상태</th>
                <th className="px-3 py-3"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {approvals.map((approval, index) => (
                <tr key={approval.approval_id}>
				  <td className="px-3 py-3 text-slate-500">{index + 1}</td>
                  <td className="px-3 py-3 text-slate-600">{formatDateTime(approval.requested_at)}</td>
                  <td className="px-3 py-3 font-semibold text-slate-900">{approval.project_code}</td>
                  <td className="px-3 py-3 text-slate-600">{approval.approval_type}</td>
                  <td className="px-3 py-3 text-slate-600">{approval.title}</td>
                  <td className="px-3 py-3 font-semibold text-slate-900">{formatWon(approval.requested_amount)}</td>
                  <td className={`px-3 py-3 font-semibold ${varianceClass(approval.variance_amount)}`}>{formatWon(approval.variance_amount)}</td>
                  <td className="px-3 py-3 text-slate-600">{approval.status}</td>
                  <td className="px-3 py-3">
                    <button type="button" onClick={() => loadApprovalDetail(approval.approval_id)} className="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50">
                      상세
                    </button>
                  </td>
                </tr>
              ))}
              {approvals.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-3 py-8 text-center text-slate-500">표시할 결재 건이 없습니다.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <aside className="rounded-md border border-slate-200 bg-slate-50 p-4">
          {selectedApproval ? (
            <div>
              <div className="border-b border-slate-200 pb-3">
                <p className="text-xs font-semibold text-slate-500">{selectedApproval.approval_type}</p>
                <h4 className="mt-1 text-base font-semibold text-slate-950">{selectedApproval.title}</h4>
                <p className="mt-1 text-sm text-slate-500">{selectedApproval.project_code} · {selectedApproval.status}</p>
              </div>
              <div className="space-y-2 py-4 text-sm text-slate-700">
                <p>요청금액: <strong>{formatWon(selectedApproval.requested_amount)}</strong></p>
                <p>기준금액: <strong>{formatWon(selectedApproval.previous_amount)}</strong></p>
                <p>증감액: <strong className={varianceClass(selectedApproval.variance_amount)}>{formatWon(selectedApproval.variance_amount)}</strong></p>
                <p>요청자: {selectedApproval.request_user}</p>
                <p>요청사유: {selectedApproval.request_reason || '-'}</p>
                {selectedApproval.comment && <p>처리의견: {selectedApproval.comment}</p>}
                {selectedApproval.cancellation_reason && (
                  <p>승인취소 사유: {selectedApproval.cancellation_reason} ({selectedApproval.cancelled_by || '-'} / {formatDate(selectedApproval.cancelled_at?.slice(0, 10))})</p>
                )}
                {(selectedApproval.attachments ?? []).length > 0 && (
                  <div className="pt-2">
                    <p className="mb-2 font-semibold text-slate-800">결재 첨부파일</p>
                    <div className="divide-y rounded-md border border-slate-200 bg-white">
                      {selectedApproval.attachments?.map((attachment) => (
                        <a
                          key={attachment.attachment_id}
                          href={apiUrl(attachment.download_url)}
                          className="flex items-center justify-between gap-3 px-3 py-2 text-sm text-cyan-800 hover:bg-cyan-50"
                        >
                          <span className="truncate">{attachment.file_name}</span>
                          <Download className="h-4 w-4 shrink-0" aria-hidden="true" />
                        </a>
                      ))}
                    </div>
                  </div>
                )}
              </div>
              {pending && (
                <div className="border-t border-slate-200 pt-4">
                  <textarea value={rejectComment} onChange={(event) => setRejectComment(event.target.value)} placeholder="반려 사유" className="input min-h-24" />
                  <div className="mt-3 flex justify-end gap-2">
                    <button type="button" onClick={() => rejectApproval(selectedApproval.approval_id)} disabled={!rejectComment.trim()} className="rounded-md border border-rose-300 px-3 py-2 text-sm font-semibold text-rose-700 hover:bg-rose-50 disabled:cursor-not-allowed disabled:opacity-40">
                      반려
                    </button>
                    <button type="button" onClick={() => approveApproval(selectedApproval.approval_id)} className="rounded-md bg-cyan-700 px-3 py-2 text-sm font-semibold text-white hover:bg-cyan-800">
                      승인
                    </button>
                  </div>
                </div>
              )}
              {canCancelApprovedExecutionBudget && (
                <div className="border-t border-amber-200 pt-4">
                  <p className="mb-2 text-sm font-semibold text-amber-900">승인취소 후 재작업</p>
                  <textarea value={rejectComment} onChange={(event) => setRejectComment(event.target.value)} placeholder="승인취소 사유" className="input min-h-24" />
                  <div className="mt-3 flex justify-end">
                    <button type="button" onClick={() => cancelApproval(selectedApproval.approval_id)} disabled={!rejectComment.trim()} className="rounded-md border border-amber-400 px-3 py-2 text-sm font-semibold text-amber-800 hover:bg-amber-50 disabled:cursor-not-allowed disabled:opacity-40">
                      승인취소 후 재작업
                    </button>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <p className="py-12 text-center text-sm text-slate-500">결재 건을 선택하세요.</p>
          )}
        </aside>
      </div>
    </div>
  )
}

