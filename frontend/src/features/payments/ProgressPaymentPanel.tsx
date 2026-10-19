import { MetricPanel } from '../../components/ui/Metrics'
import { MoneyInput } from '../../components/ui/MoneyInput'
import { formatWorkflowStatus, formatWon, toNumber } from '../../lib/format'
import type { ProgressPaymentResponse } from '../../types/domain'

export function ProgressPaymentPanel({
  progressPayment,
  selectedPoId,
  setSelectedPoId,
  claimForm,
  setClaimForm,
  selectedPaymentPoIds,
  setSelectedPaymentPoIds,
  createProgressClaim,
  approveProgressClaim,
  requestPaymentApprovals,
}: {
  progressPayment: ProgressPaymentResponse
  selectedPoId: string
  setSelectedPoId: (value: string) => void
  claimForm: { degree: string; claim_amount: string }
  setClaimForm: (value: { degree: string; claim_amount: string }) => void
  selectedPaymentPoIds: string[]
  setSelectedPaymentPoIds: (poIds: string[]) => void
  createProgressClaim: () => void
  approveProgressClaim: (claimId: string) => void
  requestPaymentApprovals: () => void
}) {
  const selectedPo = progressPayment.purchaseOrders.find((po) => po.po_id === selectedPoId) ?? progressPayment.purchaseOrders[0]
  const paymentRows = progressPayment.purchaseOrders.flatMap((po) => po.claims.map((claim) => ({ po, claim })))
  const totalApprovedClaims = progressPayment.purchaseOrders.reduce((sum, po) => sum + po.approved_claim_total, 0)
  const totalPaid = progressPayment.purchaseOrders.reduce((sum, po) => sum + po.actual_paid_total, 0)
  const selectablePoIds = progressPayment.purchaseOrders.filter((po) => po.approved_unpaid_amount > 0 && po.payment_request_status !== 'REQUESTED').map((po) => po.po_id)

  if (!selectedPo) {
    return <div className="rounded-md border border-dashed border-slate-300 bg-slate-50 p-8 text-center text-sm text-slate-500">등록된 발주가 없습니다.</div>
  }

  return (
    <div>
      <div className="flex flex-col gap-4 border-b border-slate-200 pb-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 className="text-lg font-semibold text-slate-950">기성 및 지급 현황</h3>
          <p className="mt-1 text-sm text-slate-500">프로젝트 전체 업체의 기성·지급 현황을 확인하고, 선택 업체만 지급결재를 요청합니다.</p>
        </div>
        <button type="button" onClick={requestPaymentApprovals} disabled={selectedPaymentPoIds.length === 0} className="rounded-md bg-cyan-700 px-4 py-2 text-sm font-semibold text-white hover:bg-cyan-800 disabled:cursor-not-allowed disabled:bg-slate-300">선택 업체 지급요청 ({selectedPaymentPoIds.length})</button>
      </div>

      <div className="grid gap-3 border-b border-slate-200 py-5 md:grid-cols-3">
        <MetricPanel label="발주 업체 수" value={`${progressPayment.purchaseOrders.length} 곳`} helper={`지급결재 대기 ${progressPayment.pending_payment_approval_count}건`} />
        <MetricPanel label="승인 기성누계" value={formatWon(totalApprovedClaims)} helper="전체 업체 합계" />
        <MetricPanel label="실제 지급누계" value={formatWon(totalPaid)} helper="지급결재 승인 반영" />
        <MetricPanel label="전체 승인 후 미지급" value={formatWon(totalApprovedClaims - totalPaid)} helper="지급요청 가능 금액" accent={totalApprovedClaims > totalPaid ? 'text-rose-700' : 'text-slate-950'} />
        <MetricPanel label="준공정산 이동 조건" value={progressPayment.all_payments_approved ? '충족' : '미충족'} helper="모든 정식 발주 지급 완료 기준" accent={progressPayment.all_payments_approved ? 'text-cyan-700' : 'text-slate-700'} />
      </div>

      <div className="border-b border-slate-200 py-5">
        <h3 className="mb-3 text-sm font-semibold text-slate-900">업체별 지급 대시보드</h3>
        <div className="overflow-x-auto"><table className="min-w-[1150px] text-left text-sm"><thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500"><tr><th className="px-3 py-3"><input type="checkbox" checked={selectablePoIds.length > 0 && selectablePoIds.every((id) => selectedPaymentPoIds.includes(id))} onChange={(event) => setSelectedPaymentPoIds(event.target.checked ? selectablePoIds : [])} /></th><th className="px-3 py-3">업체</th><th className="px-3 py-3">발주번호</th><th className="px-3 py-3">현재 발주금액</th><th className="px-3 py-3">승인 기성</th><th className="px-3 py-3">지급누계</th><th className="px-3 py-3">승인 후 미지급</th><th className="px-3 py-3">지급결재 상태</th></tr></thead><tbody className="divide-y divide-slate-100">{progressPayment.purchaseOrders.map((po) => { const selectable = selectablePoIds.includes(po.po_id); return <tr key={po.po_id}><td className="px-3 py-3"><input type="checkbox" disabled={!selectable} checked={selectedPaymentPoIds.includes(po.po_id)} onChange={(event) => setSelectedPaymentPoIds(event.target.checked ? [...selectedPaymentPoIds, po.po_id] : selectedPaymentPoIds.filter((id) => id !== po.po_id))} /></td><td className="px-3 py-3 font-semibold text-slate-900">{po.vendor_name}</td><td className="px-3 py-3 text-cyan-800">{po.po_id}</td><td className="px-3 py-3">{formatWon(po.current_po_amount)}</td><td className="px-3 py-3">{formatWon(po.approved_claim_total)}</td><td className="px-3 py-3">{formatWon(po.actual_paid_total)}</td><td className="px-3 py-3 font-semibold">{formatWon(po.approved_unpaid_amount)}</td><td className="px-3 py-3">{formatWorkflowStatus(po.payment_request_status)}</td></tr> })}</tbody></table></div>
      </div>

      <div className="grid gap-3 border-b border-slate-200 py-5 md:grid-cols-[220px_120px_1fr_auto]">
        <label className="block text-sm font-semibold text-slate-700">
          기성 작성 대상
          <select value={selectedPo.po_id} onChange={(event) => setSelectedPoId(event.target.value)} className="input mt-2">
            {progressPayment.purchaseOrders.map((po) => <option key={po.po_id} value={po.po_id}>{po.vendor_name} · {po.po_id}</option>)}
          </select>
        </label>
        <label className="block text-sm font-semibold text-slate-700">
          차수
          <input value={claimForm.degree} onChange={(event) => setClaimForm({ ...claimForm, degree: event.target.value })} className="input mt-2" />
        </label>
        <label className="block text-sm font-semibold text-slate-700">
          기성금액
          <MoneyInput value={toNumber(claimForm.claim_amount)} onChange={(value) => setClaimForm({ ...claimForm, claim_amount: value })} />
        </label>
        <button type="button" onClick={createProgressClaim} className="self-end rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-800">
          기성금액 작성
        </button>
      </div>

      <div className="py-5">
        <h3 className="mb-3 text-sm font-semibold text-slate-900">전체 업체 기성 및 지급 내역</h3>
        <div className="overflow-x-auto">
          <table className="min-w-[1180px] text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-3 py-3">업체</th>
                <th className="px-3 py-3">발주번호</th>
                <th className="px-3 py-3">차수</th>
                <th className="px-3 py-3">기성금액</th>
                <th className="px-3 py-3">결재상태</th>
                <th className="px-3 py-3">지급상태</th>
                <th className="px-3 py-3">지급누계</th>
                <th className="px-3 py-3">지급가능잔액</th>
                <th className="px-3 py-3">처리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {paymentRows.map(({ po, claim }) => {
                return (
                  <tr key={claim.claim_id}>
                    <td className="px-3 py-3 font-semibold text-slate-900">{po.vendor_name}</td>
                    <td className="px-3 py-3 text-cyan-800">{po.po_id}</td>
                    <td className="px-3 py-3 font-semibold text-slate-900">{claim.degree}차</td>
                    <td className="px-3 py-3">{formatWon(claim.claim_amount)}</td>
                    <td className="px-3 py-3">{formatWorkflowStatus(claim.approval_status)}</td>
                    <td className="px-3 py-3">
                      <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${claim.payment_state === 'PAID' ? 'bg-cyan-100 text-cyan-800' : claim.payment_state === 'WAITING_FOR_PAYMENT' ? 'bg-amber-100 text-amber-800' : 'bg-slate-100 text-slate-700'}`}>
                        {formatWorkflowStatus(claim.payment_state)}
                      </span>
                    </td>
                    <td className="px-3 py-3">{formatWon(claim.paid_total)}</td>
                    <td className="px-3 py-3">{formatWon(claim.remaining_payable)}</td>
                    <td className="px-3 py-3">
                      <div className="flex gap-2">
                        {claim.approval_status !== 'APPROVED' && (
                          <button type="button" onClick={() => approveProgressClaim(claim.claim_id)} className="rounded-md border border-slate-300 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50">대표 승인</button>
                        )}
                      </div>
                    </td>
                  </tr>
                )
              })}
              {paymentRows.length === 0 && (
                <tr>
                  <td colSpan={9} className="px-3 py-8 text-center text-slate-500">등록된 기성 차수가 없습니다.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

