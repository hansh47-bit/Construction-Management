import { AlertTriangle } from 'lucide-react'
import { formatDateTime, formatWon } from '../../lib/format'
import type { ChangeOrder } from '../../types/domain'

const statusLabel: Record<string, string> = {
  APPROVED: '반영 완료',
  RECONCILIATION_REQUIRED: '조정 필요',
  PARTIALLY_APPROVED: '일부 승인',
  REQUESTED: '결재 진행 중',
  DRAFT: '작성 중',
  REJECTED: '반려',
  CANCELED: '취소',
}

export function ChangeHistoryPanel({ changeOrders }: { changeOrders: ChangeOrder[] }) {
  return (
    <div>
      <div className="border-b border-slate-200 pb-4">
        <h3 className="text-lg font-semibold text-slate-950">계약변경 연동관리</h3>
        <p className="mt-1 text-sm text-slate-500">계약 변경에 연결된 세부 실행예산·발주 변경과 기성·지급 충돌 여부를 함께 확인합니다.</p>
      </div>
      <div className="py-5">
        <div className="overflow-x-auto rounded-md border border-slate-200">
          <table className="min-w-[1280px] text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-3 py-3">차수 / 범위</th>
                <th className="px-3 py-3">변경번호</th><th className="px-3 py-3">요청일시</th><th className="px-3 py-3">상태</th><th className="px-3 py-3">사유</th><th className="px-3 py-3">계약 증감</th><th className="px-3 py-3">실행예산 증감</th><th className="px-3 py-3">발주 증감</th><th className="px-3 py-3">연결 항목 / 조정 사유</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {changeOrders.map((change) => {
                const reconciliationRequired = change.requires_reconciliation || change.status === 'RECONCILIATION_REQUIRED'
                return (
                  <tr key={change.change_order_id} className={reconciliationRequired ? 'bg-amber-50/50' : ''}>
                    <td className="px-3 py-3 text-slate-700"><p className="font-semibold">{change.revision_number ?? '-'}차</p><p className="mt-1 text-xs">{change.contract_change_enabled ? '계약 변경' : '계약 이월'} · {change.budget_change_enabled ? '예산 변경' : '예산 이월'}</p></td>
                    <td className="px-3 py-3 font-semibold text-cyan-800">{change.change_order_id}</td>
                    <td className="px-3 py-3 text-slate-600">{formatDateTime(change.created_at)}</td>
                    <td className="px-3 py-3"><span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${reconciliationRequired ? 'bg-amber-100 text-amber-800' : 'bg-cyan-100 text-cyan-800'}`}>{statusLabel[change.status] ?? change.status}</span></td>
                    <td className="px-3 py-3 text-slate-700">{change.reason}</td>
                    <td className="px-3 py-3 font-semibold">{formatWon(change.contract_delta_amount)}</td>
                    <td className="px-3 py-3">{formatWon(change.budget_delta_amount)}</td>
                    <td className="px-3 py-3">{formatWon(change.purchase_order_delta_amount)}</td>
                    <td className="px-3 py-3 text-slate-600">
                      {reconciliationRequired && <p className="mb-2 flex items-start gap-1 font-semibold text-amber-800"><AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />{change.reconciliation_message ?? '승인 기성 또는 실제 지급 이력보다 감액된 발주가 있어 조정이 필요합니다.'}</p>}
                      <p>{change.impacts.map((impact) => `${impact.label} (${formatWon(impact.delta_amount)})`).join(', ') || '-'}</p>
                    </td>
                  </tr>
                )
              })}
              {changeOrders.length === 0 && <tr><td colSpan={8} className="px-3 py-10 text-center text-slate-500">등록된 계약변경 이력이 없습니다.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
