import { CheckCircle2 } from 'lucide-react'
import { Field } from '../../components/ui/Field'
import { MoneyInput } from '../../components/ui/MoneyInput'
import { formatWon, toNumber, varianceClass } from '../../lib/format'
import type { ProjectSettlement } from '../../types/domain'

export function SettlementPanel({
  settlement,
  form,
  setForm,
  requestSettlementApproval,
}: {
  settlement: ProjectSettlement
  form: { uncollected_receivable: string; settlement_site_expense: string; settlement_labor_expense: string; settlement_tax: string; settlement_notes: string; force_complete_if_unpaid: boolean; unpaid_handling_reason: string }
  setForm: (value: { uncollected_receivable: string; settlement_site_expense: string; settlement_labor_expense: string; settlement_tax: string; settlement_notes: string; force_complete_if_unpaid: boolean; unpaid_handling_reason: string }) => void
  requestSettlementApproval: () => void
}) {
  const hasUnpaidBalance = settlement.payments.unpaid_balance > 0
  const approvalPending = settlement.settlement_status === 'REQUESTED'
  const editingDisabled = settlement.is_settled || approvalPending
  const settlementSiteExpense = toNumber(form.settlement_site_expense)
  const settlementLaborExpense = toNumber(form.settlement_labor_expense)
  const settlementTax = toNumber(form.settlement_tax)
  const snapshotIncludesSettlementInputs = settlement.settlement_status === 'REQUESTED' || settlement.settlement_status === 'REJECTED' || settlement.is_settled
  const savedSettlementInputs = snapshotIncludesSettlementInputs ? settlement.settlement_site_expense + settlement.settlement_labor_expense + settlement.settlement_tax : 0
  const previewFinalCost = settlement.profitability.final_cost - savedSettlementInputs + settlementSiteExpense + settlementLaborExpense + settlementTax
  const previewFinalProfit = settlement.contract.final_amount - previewFinalCost
  const previewProfitMarginRate = settlement.contract.final_amount === 0 ? 0 : (previewFinalProfit / settlement.contract.final_amount) * 100
  const revisionNumber = settlement.revision_number
  const previousRevision = revisionNumber && revisionNumber > 1 ? `${revisionNumber - 1}차` : '최초'
  const sections = [
    {
      title: '계약 및 매출 현황', amount: settlement.contract,
      initialLabel: revisionNumber ? `${previousRevision} 승인 계약금액` : '최초 금액',
      changeLabel: revisionNumber ? `${revisionNumber}차 계약 증감` : '변경 금액',
      finalLabel: revisionNumber ? `${revisionNumber}차 승인 계약금액` : '최종 계약금액',
    },
    {
      title: '실행예산 현황', amount: settlement.execution_budget,
      initialLabel: revisionNumber ? `${previousRevision} 실행예산` : '최초 금액',
      changeLabel: revisionNumber ? `${revisionNumber}차 실행예산 증감` : '변경 금액',
      finalLabel: revisionNumber ? `${revisionNumber}차 승인 실행예산` : '최종 실행예산',
    },
    {
      title: '발주 및 집행 현황', amount: settlement.purchase_orders,
      initialLabel: revisionNumber ? `${revisionNumber}차 최초 발주금액` : '최초 금액',
      changeLabel: revisionNumber ? `${revisionNumber}차 발주 증감` : '변경 금액',
      finalLabel: revisionNumber ? `${revisionNumber}차 최종 발주금액` : '최종 발주금액',
    },
  ] as const

  return (
    <div>
      {settlement.revision_number && (
        <div className="mb-4 rounded-md border border-cyan-200 bg-cyan-50 px-4 py-3 text-sm text-cyan-900">
          <strong>{settlement.revision_number}차 준공정산</strong>입니다. 이전 차수의 발주·기성·지급 내역은 보존하되 현재 정산 금액에는 포함하지 않습니다.
        </div>
      )}
      <div className="flex flex-col gap-4 border-b border-slate-200 pb-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 className="text-lg font-semibold text-slate-950">준공정산</h3>
          <p className="mt-1 text-sm text-slate-500">계약, 실행예산, 발주, 기성, 지급 누계를 집계해 최종 손익과 마감 가능 여부를 확인합니다.</p>
        </div>
        <span className={`inline-flex w-fit items-center rounded-full px-3 py-1 text-xs font-semibold ${settlement.is_settled ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}`}>
          {settlement.is_settled ? '정산완료' : approvalPending ? '최종 결재대기' : settlement.settlement_status === 'REJECTED' ? '결재반려' : '작성중'}
        </span>
      </div>

      <div className="grid gap-4 py-5 lg:grid-cols-3">
        {sections.map(({ title, amount, initialLabel, changeLabel, finalLabel }) => (
          <article key={title} className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
            <h4 className="text-sm font-semibold text-slate-900">{title}</h4>
            <dl className="mt-4 space-y-3 text-sm">
              <div className="flex items-center justify-between gap-4">
                <dt className="text-slate-500">{initialLabel}</dt>
                <dd className="font-semibold text-slate-900">{formatWon(amount.initial_amount)}</dd>
              </div>
              <div className="flex items-center justify-between gap-4">
                <dt className="text-slate-500">{changeLabel}</dt>
                <dd className={varianceClass(amount.change_amount)}>{formatWon(amount.change_amount)}</dd>
              </div>
              <div className="flex items-center justify-between gap-4 border-t border-slate-100 pt-3">
                <dt className="font-semibold text-slate-700">{finalLabel}</dt>
                <dd className="text-base font-semibold text-slate-950">{formatWon(amount.final_amount)}</dd>
              </div>
            </dl>
          </article>
        ))}
      </div>

      <div className="grid gap-4 border-t border-slate-200 py-5 lg:grid-cols-2">
        <article className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
          <h4 className="text-sm font-semibold text-slate-900">기성 및 지급 현황</h4>
          <dl className="mt-4 space-y-3 text-sm">
            <div className="flex items-center justify-between gap-4">
              <dt className="text-slate-500">승인 기성누계</dt>
              <dd className="font-semibold text-slate-900">{formatWon(settlement.payments.approved_claims_total)}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-slate-500">실제 지급누계</dt>
              <dd className="font-semibold text-slate-900">{formatWon(settlement.payments.actual_paid_total)}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-slate-500">미지급금액</dt>
              <dd className={`font-semibold ${hasUnpaidBalance ? 'text-rose-700' : 'text-slate-900'}`}>{formatWon(settlement.payments.unpaid_balance)}</dd>
            </div>
            <div>
              <dt className="mb-2 text-slate-500">미수금</dt>
              <dd><MoneyInput value={toNumber(form.uncollected_receivable)} onChange={(value) => setForm({ ...form, uncollected_receivable: value })} disabled={editingDisabled} /></dd>
            </div>
            <div className="grid gap-3 border-t border-slate-100 pt-4 sm:grid-cols-3">
              <label className="block text-sm font-semibold text-slate-700">
				현장경비
				<span className="mt-1 block text-xs font-normal text-slate-500">승인된 현장경비 {formatWon(settlement.approved_site_expense_amount)}이 기본 입력되며, 실제 집행액으로 수정할 수 있습니다.</span>
                <MoneyInput value={settlementSiteExpense} onChange={(value) => setForm({ ...form, settlement_site_expense: value })} disabled={editingDisabled} className="mt-2" />
              </label>
              <label className="block text-sm font-semibold text-slate-700">
				인건비
				<span className="mt-1 block text-xs font-normal text-slate-500">승인된 인건비 {formatWon(settlement.approved_labor_expense_amount)}이 기본 입력되며, 실제 집행액으로 수정할 수 있습니다.</span>
                <MoneyInput value={settlementLaborExpense} onChange={(value) => setForm({ ...form, settlement_labor_expense: value })} disabled={editingDisabled} className="mt-2" />
              </label>
              <label className="block text-sm font-semibold text-slate-700">
                준공 세금
                <span className="mt-1 block text-xs font-normal text-slate-500">정산 시 비용 처리할 세금</span>
                <MoneyInput value={settlementTax} onChange={(value) => setForm({ ...form, settlement_tax: value })} disabled={editingDisabled} className="mt-2" />
              </label>
            </div>
          </dl>
        </article>

        <article className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
          <h4 className="text-sm font-semibold text-slate-900">최종 손익 분석</h4>
          <div className="mt-4 grid gap-3 sm:grid-cols-3">
            <div className="border-l-2 border-slate-200 pl-3">
              <p className="text-xs font-semibold text-slate-500">최종 원가</p>
              <p className="mt-2 text-lg font-semibold text-slate-950">{formatWon(previewFinalCost)}</p>
              <p className="mt-1 text-xs text-slate-500">최종 발주금액 기준</p>
            </div>
            <div className="border-l-2 border-slate-200 pl-3">
              <p className="text-xs font-semibold text-slate-500">최종 이익</p>
              <p className={`mt-2 text-lg font-semibold ${previewFinalProfit < 0 ? 'text-rose-700' : 'text-cyan-700'}`}>{formatWon(previewFinalProfit)}</p>
              <p className="mt-1 text-xs text-slate-500">최종 계약금액 - 최종 원가</p>
            </div>
            <div className="border-l-2 border-slate-200 pl-3">
              <p className="text-xs font-semibold text-slate-500">최종 이익률</p>
              <p className="mt-2 text-lg font-semibold text-slate-950">{previewProfitMarginRate.toFixed(2)}%</p>
              <p className="mt-1 text-xs text-slate-500">최종 이익 / 최종 계약금액</p>
            </div>
          </div>
          {hasUnpaidBalance && (
            <div className="mt-4 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
              미지급 잔액이 남아 있습니다. 정산완료를 진행하려면 강제 완료 확인과 처리 사유가 필요합니다.
            </div>
          )}
        </article>
      </div>

      <div className="border-t border-slate-200 py-5">
        {approvalPending && (
          <div className="mb-4 rounded-md border border-cyan-200 bg-cyan-50 p-3 text-sm text-cyan-900">
            준공정산 최종 결재 요청이 등록되었습니다. 통합결재함에서 승인되어야 정산완료 처리됩니다.
          </div>
        )}
        <div className="grid gap-4 lg:grid-cols-2">
          <Field label="정산 비고">
            <textarea value={form.settlement_notes} onChange={(event) => setForm({ ...form, settlement_notes: event.target.value })} className="input min-h-24" disabled={editingDisabled} />
          </Field>
          <Field label="미지급 처리 사유">
            <textarea value={form.unpaid_handling_reason} onChange={(event) => setForm({ ...form, unpaid_handling_reason: event.target.value })} className="input min-h-24" disabled={!hasUnpaidBalance || editingDisabled} />
          </Field>
        </div>
        <label className="mt-4 flex items-center gap-2 text-sm font-semibold text-slate-700">
          <input
            type="checkbox"
            checked={form.force_complete_if_unpaid}
            onChange={(event) => setForm({ ...form, force_complete_if_unpaid: event.target.checked })}
            disabled={!hasUnpaidBalance || editingDisabled}
          />
          미지급 잔액이 있어도 사유를 남기고 정산완료 처리
        </label>
        <div className="mt-5 flex justify-end">
          <button
            type="button"
            onClick={requestSettlementApproval}
            disabled={editingDisabled || (hasUnpaidBalance && (!form.force_complete_if_unpaid || !form.unpaid_handling_reason.trim()))}
            className="inline-flex items-center gap-2 rounded-md bg-cyan-700 px-4 py-2 text-sm font-semibold text-white hover:bg-cyan-800 disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
            준공정산 최종 결재요청
          </button>
        </div>
        {settlement.is_settled && (
          <p className="mt-3 text-right text-xs text-slate-500">
            {settlement.settlement_id} · {settlement.settled_by ?? 'system'} · {settlement.settled_at ?? '-'}
          </p>
        )}
      </div>
    </div>
  )
}

