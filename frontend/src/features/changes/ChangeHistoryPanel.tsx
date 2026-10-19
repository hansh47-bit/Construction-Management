import { formatDate, formatWon } from '../../lib/format'
import type { ChangeOrder } from '../../types/domain'

export function ChangeHistoryPanel({ changeOrders }: { changeOrders: ChangeOrder[] }) {
  return (
    <div>
      <div className="border-b border-slate-200 pb-4">
        <h3 className="text-lg font-semibold text-slate-950">변경관리 이력</h3>
        <p className="mt-1 text-sm text-slate-500">대표 금액 정정으로 즉시 반영된 현재 금액과, 최초 금액을 보존하기 위한 증감 이력을 관리합니다.</p>
      </div>
      <div className="py-5">
        <div className="overflow-x-auto rounded-md border border-slate-200">
          <table className="min-w-[1050px] text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr><th className="px-3 py-3">변경번호</th><th className="px-3 py-3">일시</th><th className="px-3 py-3">처리자</th><th className="px-3 py-3">사유</th><th className="px-3 py-3">계약 증감</th><th className="px-3 py-3">실행예산 증감</th><th className="px-3 py-3">발주 증감</th><th className="px-3 py-3">연결 항목</th></tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {changeOrders.map((change) => (
                <tr key={change.change_order_id}>
                  <td className="px-3 py-3 font-semibold text-cyan-800">{change.change_order_id}</td>
                  <td className="px-3 py-3 text-slate-600">{formatDate(change.created_at?.slice(0, 10))}</td>
                  <td className="px-3 py-3 text-slate-700">{change.requested_by}</td>
                  <td className="px-3 py-3 text-slate-700">{change.reason}</td>
                  <td className="px-3 py-3 font-semibold">{formatWon(change.contract_delta_amount)}</td>
                  <td className="px-3 py-3">{formatWon(change.budget_delta_amount)}</td>
                  <td className="px-3 py-3">{formatWon(change.purchase_order_delta_amount)}</td>
                  <td className="px-3 py-3 text-slate-600">{change.impacts.map((impact) => `${impact.label} (${formatWon(impact.delta_amount)})`).join(', ')}</td>
                </tr>
              ))}
              {changeOrders.length === 0 && <tr><td colSpan={8} className="px-3 py-10 text-center text-slate-500">등록된 변경 이력이 없습니다.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

