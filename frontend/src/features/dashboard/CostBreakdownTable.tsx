import { formatWon } from '../../lib/format'
import type { CostBreakdown } from '../../types/domain'

export function CostBreakdownTable({ rows }: { rows: CostBreakdown[] }) {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full text-left text-sm">
        <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
          <tr>
            <th className="px-3 py-3">공종명</th>
            <th className="px-3 py-3">최초 실행예산</th>
            <th className="px-3 py-3">현재 실행예산</th>
            <th className="px-3 py-3">발주금액</th>
            <th className="px-3 py-3">누적 기성</th>
            <th className="px-3 py-3">실제 지급</th>
            <th className="px-3 py-3">예산 대비</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {rows.map((row) => (
            <tr key={row.budget_id}>
              <td className="px-3 py-3 font-semibold text-slate-900">{row.work_category}</td>
              <td className="px-3 py-3 text-slate-600">{formatWon(row.initial_execution_budget)}</td>
              <td className="px-3 py-3 text-slate-600">{formatWon(row.current_execution_budget)}</td>
              <td className="px-3 py-3 text-slate-600">{formatWon(row.purchase_amount)}</td>
              <td className="px-3 py-3 text-slate-600">{formatWon(row.accumulated_completed_amount)}</td>
              <td className="px-3 py-3 text-slate-600">{formatWon(row.actual_paid_amount)}</td>
              <td className={`px-3 py-3 font-semibold ${row.budget_variance > 0 ? 'text-rose-700' : 'text-cyan-700'}`}>
                {formatWon(row.budget_variance)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

