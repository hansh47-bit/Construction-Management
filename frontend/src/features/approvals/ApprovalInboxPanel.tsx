import { Download } from 'lucide-react'
import { apiUrl } from '../../lib/api-client'
import { formatDate, formatDateTime, formatWon } from '../../lib/format'
import type { ApprovalItem, ExecutionBudgetApprovalDetail, FinalSettlementApprovalDetail, ProgressClaimApprovalDetail, PurchaseOrderApprovalDetail } from '../../types/domain'

function varianceClass(value: number) {
  if (value > 0) return 'text-rose-700'
  if (value < 0) return 'text-blue-700'
  return 'text-slate-900'
}

function approvalTypeLabel(type: string) {
  if (type === 'PROGRESS_CLAIM') return '기성 승인'
  if (type === 'EXECUTION_BUDGET') return '실행예산'
  if (type === 'PURCHASE_ORDER') return '발주품의'
  if (type === 'PAYMENT') return '지급'
  if (type === 'PROJECT_REVISION') return '계약·실행예산 변경'
  if (type === 'FINAL_SETTLEMENT') return '준공정산 최종결재'
  return type
}

function isPurchaseOrderDetail(detail: ApprovalItem['detail']): detail is PurchaseOrderApprovalDetail {
  return !!detail && 'selected_vendor_name' in detail && 'comparisons' in detail
}

function isExecutionBudgetDetail(detail: ApprovalItem['detail']): detail is ExecutionBudgetApprovalDetail {
  return !!detail && 'summary_by_item' in detail && 'total_detail_amount' in detail
}

function isProgressClaimDetail(detail: ApprovalItem['detail']): detail is ProgressClaimApprovalDetail {
  return !!detail && 'claim_id' in detail && 'claim_amount' in detail
}

function isFinalSettlementDetail(detail: ApprovalItem['detail']): detail is FinalSettlementApprovalDetail {
  return !!detail && 'final_contract_amount' in detail && 'final_profit' in detail
}

function ApprovalDocument({ approval }: { approval: ApprovalItem }) {
  if (approval.approval_type === 'FINAL_SETTLEMENT' && isFinalSettlementDetail(approval.detail)) {
    const detail = approval.detail
    return (
      <section className="border-t border-slate-200 pt-4">
        <p className="text-sm font-bold text-slate-950">준공정산 최종 검토 내용</p>
        <div className="mt-3 grid grid-cols-2 gap-2 text-sm">
          <div className="rounded border border-slate-200 bg-white p-2"><p className="text-xs text-slate-500">최종 계약금액</p><strong>{formatWon(detail.final_contract_amount)}</strong></div>
          <div className="rounded border border-slate-200 bg-white p-2"><p className="text-xs text-slate-500">최종 실행예산</p><strong>{formatWon(detail.final_execution_budget)}</strong></div>
          <div className="rounded border border-slate-200 bg-white p-2"><p className="text-xs text-slate-500">최종 발주금액</p><strong>{formatWon(detail.final_purchase_order_amount)}</strong></div>
          <div className="rounded border border-slate-200 bg-white p-2"><p className="text-xs text-slate-500">준공경비·세금</p><strong>{formatWon(detail.settlement_expense + detail.settlement_tax)}</strong></div>
          <div className="rounded border border-slate-200 bg-white p-2"><p className="text-xs text-slate-500">최종 원가</p><strong>{formatWon(detail.final_cost)}</strong></div>
          <div className="rounded border border-cyan-200 bg-cyan-50 p-2"><p className="text-xs text-cyan-800">최종 이익 · 이익률</p><strong>{formatWon(detail.final_profit)} · {detail.profit_margin_rate.toFixed(2)}%</strong></div>
          <div className={`col-span-2 rounded border p-2 ${detail.unpaid_balance > 0 ? 'border-amber-300 bg-amber-50' : 'border-emerald-300 bg-emerald-50'}`}><p className="text-xs text-slate-500">미지급 잔액</p><strong>{formatWon(detail.unpaid_balance)}</strong></div>
        </div>
      </section>
    )
  }

  if (approval.approval_type === 'PROGRESS_CLAIM' && isProgressClaimDetail(approval.detail)) {
    const detail = approval.detail
    return (
      <section className="border-t border-slate-200 pt-4">
        <p className="text-sm font-bold text-slate-950">기성 승인 검토 내용</p>
        <div className="mt-3 rounded-md border border-cyan-200 bg-cyan-50 p-3">
          <p className="text-xs font-semibold text-cyan-800">{detail.po_id} · {detail.execution_item_id}</p>
          <p className="mt-1 text-base font-bold text-slate-950">{detail.vendor_name}</p>
          <p className="mt-1 text-sm text-slate-700">{detail.work_category || '공종 미입력'} · {detail.execution_item_name || '실행항목 미입력'}</p>
        </div>
        <div className="mt-3 grid grid-cols-2 gap-2 text-sm">
          <div className="rounded border border-slate-200 bg-white p-2"><p className="text-xs text-slate-500">기성 차수</p><strong>{detail.degree}차</strong></div>
          <div className="rounded border border-slate-200 bg-white p-2"><p className="text-xs text-slate-500">이번 기성 금액</p><strong>{formatWon(detail.claim_amount)}</strong></div>
          <div className="col-span-2 rounded border border-slate-200 bg-white p-2"><p className="text-xs text-slate-500">승인 기성 누계(이번 건 전)</p><strong>{formatWon(detail.approved_claim_total)}</strong></div>
        </div>
      </section>
    )
  }

  if (approval.approval_type === 'PURCHASE_ORDER' && isPurchaseOrderDetail(approval.detail)) {
    const detail = approval.detail
    return (
      <section className="border-t border-slate-200 pt-4">
        <p className="text-sm font-bold text-slate-950">발주품의 검토 내용</p>
        <div className="mt-3 rounded-md border border-cyan-200 bg-cyan-50 p-3">
          <p className="text-xs font-semibold text-cyan-800">현장명 · {approval.project_name || '-'}</p>
          <p className="text-xs font-semibold text-cyan-800">{detail.execution_item_id} · {detail.work_category || '공종 미입력'}</p>
          <p className="mt-1 text-base font-bold text-slate-950">{detail.execution_item_name || '실행항목명 미입력'}</p>
          <p className="mt-2 text-sm text-slate-700">선정 업체: <strong>{detail.selected_vendor_name}</strong></p>
        </div>
        <div className="mt-3 grid grid-cols-2 gap-2 text-sm">
          <div className="rounded border border-slate-200 bg-white p-2"><p className="text-xs text-slate-500">승인 실행예산</p><strong>{formatWon(detail.approved_execution_budget)}</strong></div>
          <div className="rounded border border-slate-200 bg-white p-2"><p className="text-xs text-slate-500">발주 요청금액</p><strong>{formatWon(detail.proposed_po_amount)}</strong></div>
          <div className="col-span-2 rounded border border-slate-200 bg-white p-2"><p className="text-xs text-slate-500">예산 대비</p><strong className={varianceClass(detail.budget_variance)}>{formatWon(detail.budget_variance)}</strong></div>
        </div>
        <div className="mt-3 rounded border border-slate-200 bg-white p-3 text-sm"><p className="text-xs font-semibold text-slate-500">선정 사유</p><p className="mt-1 whitespace-pre-wrap text-slate-800">{detail.selection_reason || '-'}</p></div>
        <div className="mt-4 overflow-x-auto">
          <p className="mb-2 text-sm font-semibold text-slate-800">업체별 비교견적</p>
          <table className="min-w-[620px] text-left text-xs">
            <thead className="bg-slate-100 text-slate-500"><tr><th className="px-2 py-2">업체</th><th className="px-2 py-2">견적금액</th><th className="px-2 py-2">VAT</th><th className="px-2 py-2">예산대비</th><th className="px-2 py-2">선정</th></tr></thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {detail.comparisons.map((comparison) => (
                <tr key={comparison.comparison_id ?? comparison.comparison_code ?? comparison.vendor_name} className={comparison.is_selected ? 'bg-cyan-50' : ''}>
                  <td className="px-2 py-2 font-semibold text-slate-800">{comparison.vendor_name || '-'}</td>
                  <td className="px-2 py-2">{formatWon(comparison.quoted_amount)}</td>
                  <td className="px-2 py-2">{comparison.vat_type || '-'}</td>
                  <td className={`px-2 py-2 font-semibold ${varianceClass(comparison.budget_variance ?? 0)}`}>{formatWon(comparison.budget_variance ?? 0)}</td>
                  <td className="px-2 py-2">{comparison.is_selected ? <span className="rounded bg-cyan-700 px-2 py-1 font-semibold text-white">선정</span> : '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    )
  }

  if (approval.approval_type === 'EXECUTION_BUDGET' && isExecutionBudgetDetail(approval.detail)) {
    const detail = approval.detail
    return (
      <section className="border-t border-slate-200 pt-4">
        <p className="text-sm font-bold text-slate-950">실행예산 검토 내용</p>
        <div className="mt-3 grid grid-cols-2 gap-2 text-sm">
          <div className="rounded border border-slate-200 bg-white p-2"><p className="text-xs text-slate-500">계약 공급가</p><strong>{formatWon(detail.contract_supply_amount)}</strong></div>
          <div className="rounded border border-slate-200 bg-white p-2"><p className="text-xs text-slate-500">목표 원가한도</p><strong>{formatWon(detail.target_cost_limit)}</strong></div>
          <div className={`col-span-2 rounded border p-2 ${detail.over_target_limit ? 'border-rose-300 bg-rose-50' : 'border-emerald-300 bg-emerald-50'}`}><p className="text-xs text-slate-500">실행예산 합계</p><strong>{formatWon(detail.total_detail_amount)}</strong><span className="ml-2 text-xs font-semibold">{detail.over_target_limit ? '원가한도 초과' : '원가한도 이내'}</span></div>
        </div>
        <div className="mt-4 overflow-x-auto">
          <p className="mb-2 text-sm font-semibold text-slate-800">공종별 실행예산</p>
          <table className="min-w-[520px] text-left text-xs">
            <thead className="bg-slate-100 text-slate-500"><tr><th className="px-2 py-2">항목</th><th className="px-2 py-2">공종</th><th className="px-2 py-2 text-right">금액</th></tr></thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {detail.summary_by_item.map((item) => <tr key={item.budget_item_id}><td className="px-2 py-2 font-medium">{item.budget_item_id}</td><td className="px-2 py-2">{item.category_name} · {item.item_name}</td><td className="px-2 py-2 text-right font-semibold">{formatWon(item.rolled_up_amount)}</td></tr>)}
            </tbody>
          </table>
        </div>
      </section>
    )
  }

  return <p className="border-t border-slate-200 pt-4 text-sm text-slate-500">세부 결재 문서를 불러오는 중이거나, 이 결재 유형의 상세 양식이 아직 없습니다.</p>
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
          <p className="mt-1 text-sm text-slate-500">실행예산, 변경 차수, 발주품의 결재를 한 화면에서 승인하거나 반려합니다.</p>
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
            <option value="PROJECT_REVISION">계약·실행예산 변경</option>
            <option value="PURCHASE_ORDER">발주</option>
            <option value="PROGRESS_CLAIM">기성 승인</option>
            <option value="FINAL_SETTLEMENT">준공정산 최종결재</option>
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
                  <td className="px-3 py-3"><p className="font-semibold text-slate-900">{approval.project_code}</p><p className="mt-1 text-xs font-normal text-slate-500">{approval.project_name || '-'}</p></td>
                  <td className="px-3 py-3 text-slate-600">{approvalTypeLabel(approval.approval_type)}</td>
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
                <p className="text-xs font-semibold text-slate-500">{approvalTypeLabel(selectedApproval.approval_type)}</p>
                <h4 className="mt-1 text-base font-semibold text-slate-950">{selectedApproval.title}</h4>
                <p className="mt-1 text-sm text-slate-500">{selectedApproval.project_code} · {selectedApproval.project_name || '-'} · {selectedApproval.status}</p>
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
                <ApprovalDocument approval={selectedApproval} />
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

