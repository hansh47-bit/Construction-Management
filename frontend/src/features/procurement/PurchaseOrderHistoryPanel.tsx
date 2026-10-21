import { formatDate, formatWon, varianceClass } from '../../lib/format'
import type { PurchaseOrderHistoryItem } from '../../types/domain'

export function PurchaseOrderHistoryPanel({ purchaseOrders, revisionNumber }: { purchaseOrders: PurchaseOrderHistoryItem[]; revisionNumber?: number }) {
  const scopeName = revisionNumber ? `${revisionNumber}차 승인 실행예산` : '최초 승인 실행예산'

  return (
    <div>
      <div className="border-b border-slate-200 pb-4">
        <h3 className="text-lg font-semibold text-slate-950">{revisionNumber ? `${revisionNumber}차 계약/발주 이력` : '최초 계약/발주 이력'}</h3>
        <p className="mt-1 text-sm text-slate-500">
          {revisionNumber
            ? `${scopeName}에서 새로 진행한 비교견적·발주만 표시합니다. 이전 차수 발주는 이전 이력으로 보존됩니다.`
            : '최초 승인 실행예산에서 진행한 비교견적·발주 이력입니다.'}
        </p>
      </div>
      <div className="overflow-x-auto py-5">
        <table className="min-w-[1500px] text-left text-sm">
          <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
            <tr>
              <th className="px-3 py-3">발주번호</th><th className="px-3 py-3">실행항목</th><th className="px-3 py-3">선정 업체</th><th className="px-3 py-3">승인 실행예산</th><th className="px-3 py-3">현재 발주금액</th><th className="px-3 py-3">예산 대비</th><th className="px-3 py-3">승인 정보</th><th className="px-3 py-3">기성 / 지급 / 미지급</th><th className="px-3 py-3">선정 근거</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {purchaseOrders.map((purchaseOrder) => (
              <tr key={purchaseOrder.po_id}>
                <td className="px-3 py-3 font-semibold text-cyan-800">{purchaseOrder.po_id}</td>
                <td className="px-3 py-3 text-slate-700">{purchaseOrder.execution_item_id} · {purchaseOrder.work_category}<br />{purchaseOrder.execution_item_name}</td>
                <td className="px-3 py-3 font-semibold text-slate-900">{purchaseOrder.vendor_name}</td>
                <td className="px-3 py-3 text-slate-700">{formatWon(purchaseOrder.approved_execution_budget)}</td>
                <td className="px-3 py-3 font-semibold text-slate-900">{formatWon(purchaseOrder.current_po_amount)}</td>
                <td className={`px-3 py-3 font-semibold ${varianceClass(purchaseOrder.budget_variance)}`}>{formatWon(purchaseOrder.budget_variance)}</td>
                <td className="px-3 py-3 text-slate-600">{formatDate(purchaseOrder.approved_at?.slice(0, 10))}<br />{purchaseOrder.approver ?? '-'}</td>
                <td className="px-3 py-3 text-slate-600">{formatWon(purchaseOrder.approved_claim_total)} / {formatWon(purchaseOrder.actual_paid_total)} / {formatWon(purchaseOrder.total_unpaid_balance)}</td>
                <td className="px-3 py-3 text-slate-600"><p>{purchaseOrder.selection_reason || '-'}</p><p className="mt-1 text-xs">{purchaseOrder.construction_period || '-'} · {purchaseOrder.payment_terms || '-'}</p></td>
              </tr>
            ))}
            {purchaseOrders.length === 0 && <tr><td colSpan={9} className="px-3 py-10 text-center text-slate-500">{scopeName}에서 새로 승인된 발주가 없습니다. 업체 비교견적부터 새로 작성해 발주를 진행하세요.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  )
}
