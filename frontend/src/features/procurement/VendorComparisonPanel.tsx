import { Plus, Save, Trash2 } from 'lucide-react'
import { MetricPanel } from '../../components/ui/Metrics'
import { MoneyInput } from '../../components/ui/MoneyInput'
import { formatWon, varianceClass } from '../../lib/format'
import type { ExecutionBudgetDetailResponse, PurchaseOrderDraft, VendorComparisonItem, VendorComparisonResponse } from '../../types/domain'

const vatTypes = ['별도', '포함', '면세']

export function VendorComparisonPanel({
  executionBudget,
  selectedExecutionItemId,
  setSelectedExecutionItemId,
  vendorComparison,
  comparisons,
  purchaseOrderDraft,
  saveVendorComparisons,
  requestPurchaseOrder,
  updateVendorComparison,
  addVendorComparison,
  removeVendorComparison,
  openPurchaseOrderHistory,
}: {
  executionBudget: ExecutionBudgetDetailResponse
  selectedExecutionItemId: string
  setSelectedExecutionItemId: (value: string) => void
  vendorComparison: VendorComparisonResponse
  comparisons: VendorComparisonItem[]
  purchaseOrderDraft: PurchaseOrderDraft | null
  saveVendorComparisons: () => void
  requestPurchaseOrder: () => void
  updateVendorComparison: (index: number, field: keyof VendorComparisonItem, value: string | boolean) => void
  addVendorComparison: () => void
  removeVendorComparison: (index: number) => void
  openPurchaseOrderHistory: () => void
}) {
	const approvedItems = executionBudget.summary_by_item.filter((item) => item.approval_status === 'APPROVED')
	const hasApprovedItems = approvedItems.length > 0
  const selected = comparisons.find((comparison) => comparison.is_selected)
  const approvedBudget = vendorComparison.approved_execution_budget || 0
  const isPurchaseOrderLocked = purchaseOrderDraft?.approval_status === 'REQUESTED' || purchaseOrderDraft?.approval_status === 'APPROVED'
  const canRequestPurchaseOrder = hasApprovedItems && !!selected && !isPurchaseOrderLocked

  return (
    <div>
      <div className="flex flex-col gap-4 border-b border-slate-200 pb-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 className="text-lg font-semibold text-slate-950">업체 비교견적 및 발주품의</h3>
          <p className="mt-1 text-sm text-slate-500">승인 실행예산을 기준으로 업체 견적을 비교하고 선정 업체를 발주품의로 연결합니다.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" onClick={saveVendorComparisons} disabled={!hasApprovedItems || isPurchaseOrderLocked} className="inline-flex items-center gap-2 rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-300">
            <Save className="h-4 w-4" aria-hidden="true" />
            비교견적 저장
          </button>
          <button type="button" onClick={requestPurchaseOrder} disabled={!canRequestPurchaseOrder} className="inline-flex items-center gap-2 rounded-md bg-cyan-700 px-4 py-2 text-sm font-semibold text-white hover:bg-cyan-800 disabled:cursor-not-allowed disabled:bg-slate-300">
            발주품의 요청
          </button>
        </div>
      </div>

		{!hasApprovedItems && (
			<p className="border-b border-slate-200 py-5 text-sm text-slate-500">승인된 세부실행예산 항목이 있어야 업체 비교견적을 작성할 수 있습니다.</p>
		)}
      <div className="grid gap-4 border-b border-slate-200 py-5 lg:grid-cols-[260px_1fr_1fr]">
        <label className="block text-sm font-semibold text-slate-700">
          실행항목ID
          <select value={selectedExecutionItemId} onChange={(event) => setSelectedExecutionItemId(event.target.value)} className="input mt-2">
            {approvedItems.map((item) => (
              <option key={item.budget_item_id} value={item.budget_item_id}>
                {item.budget_item_id} · {item.item_name}
              </option>
            ))}
          </select>
        </label>
        <MetricPanel label="공종 / 실행항목" value={vendorComparison.work_category} helper={vendorComparison.execution_item_name} />
        <MetricPanel label="승인 실행예산" value={formatWon(approvedBudget)} helper="세부실행예산 Roll-up 기준" />
      </div>

      {selected && (
        <div className="border-b border-slate-200 py-5">
          <h3 className="text-sm font-semibold text-slate-900">발주품의서</h3>
          <div className="mt-3 rounded-md border border-slate-200 bg-slate-50 p-4 text-sm">
            <p className="font-semibold text-slate-950">[발주품의서] {vendorComparison.project_code} / {vendorComparison.execution_item_id} ({vendorComparison.execution_item_name})</p>
            <div className="mt-3 grid gap-2 sm:grid-cols-2">
              <p>승인 실행예산: <strong>{formatWon(approvedBudget)}</strong></p>
              <p>선정 업체: <strong>{selected.vendor_name}</strong></p>
              <p>발주예정금액: <strong>{formatWon(selected.quoted_amount)}</strong></p>
              <p>
                예산대비증감:{' '}
                <strong className={varianceClass((selected.quoted_amount || 0) - approvedBudget)}>
                  {formatWon((selected.quoted_amount || 0) - approvedBudget)}
                </strong>
              </p>
            </div>
            <p className="mt-3">선정 사유: {selected.selection_reason || '-'}</p>
            {purchaseOrderDraft && (
              <div className="mt-3 rounded-md bg-white px-3 py-2 text-xs font-semibold text-slate-600">
                <p>결재상태: {purchaseOrderDraft.approval_status}</p>
                {purchaseOrderDraft.po_id && (
                  <div className="mt-2 flex flex-wrap items-center gap-2 text-cyan-800">
                    <span>발주 확정 · {purchaseOrderDraft.po_id} · {purchaseOrderDraft.approved_at?.slice(0, 10) ?? '-'} · {purchaseOrderDraft.approver ?? '-'}</span>
                    <button type="button" onClick={openPurchaseOrderHistory} className="rounded border border-cyan-300 px-2 py-1 text-xs font-semibold hover:bg-cyan-50">계약/발주 이력 보기</button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      <div className="py-5">
        <div className="overflow-x-auto">
          <table className="min-w-[1600px] text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-3 py-3">비교ID</th>
                <th className="px-3 py-3">프로젝트</th>
                <th className="px-3 py-3">실행항목ID</th>
                <th className="px-3 py-3">공종</th>
                <th className="px-3 py-3">실행항목</th>
                <th className="px-3 py-3">승인 실행예산</th>
                <th className="px-3 py-3">업체</th>
                <th className="px-3 py-3">견적금액</th>
                <th className="px-3 py-3">VAT</th>
                <th className="px-3 py-3">공사기간</th>
                <th className="px-3 py-3">포함범위/특이사항</th>
                <th className="px-3 py-3">결제조건</th>
                <th className="px-3 py-3">견적서링크</th>
                <th className="px-3 py-3">선정</th>
                <th className="px-3 py-3">선정사유</th>
                <th className="px-3 py-3">예산대비</th>
                <th className="px-3 py-3"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {comparisons.map((comparison, index) => {
                const variance = (comparison.quoted_amount || 0) - approvedBudget
                return (
                  <tr key={comparison.comparison_id ?? index}>
                    <td className="px-3 py-3"><input disabled={isPurchaseOrderLocked} value={comparison.comparison_code ?? ''} onChange={(event) => updateVendorComparison(index, 'comparison_code', event.target.value)} className="input" /></td>
                    <td className="px-3 py-3 text-slate-600">{vendorComparison.project_code}</td>
                    <td className="px-3 py-3 text-slate-600">{vendorComparison.execution_item_id}</td>
                    <td className="px-3 py-3 text-slate-600">{vendorComparison.work_category}</td>
                    <td className="px-3 py-3 text-slate-600">{vendorComparison.execution_item_name}</td>
                    <td className="px-3 py-3 font-semibold text-slate-900">{formatWon(approvedBudget)}</td>
                    <td className="px-3 py-3"><input disabled={isPurchaseOrderLocked} value={comparison.vendor_name} onChange={(event) => updateVendorComparison(index, 'vendor_name', event.target.value)} className="input" /></td>
                    <td className="px-3 py-3"><MoneyInput value={comparison.quoted_amount} disabled={isPurchaseOrderLocked} onChange={(value) => updateVendorComparison(index, 'quoted_amount', value)} /></td>
                    <td className="px-3 py-3">
                      <select disabled={isPurchaseOrderLocked} value={comparison.vat_type} onChange={(event) => updateVendorComparison(index, 'vat_type', event.target.value)} className="input">
                        {vatTypes.map((type) => <option key={type} value={type}>{type}</option>)}
                      </select>
                    </td>
                    <td className="px-3 py-3"><input disabled={isPurchaseOrderLocked} value={comparison.construction_period ?? ''} onChange={(event) => updateVendorComparison(index, 'construction_period', event.target.value)} className="input" /></td>
                    <td className="px-3 py-3"><input disabled={isPurchaseOrderLocked} value={comparison.scope_and_notes ?? ''} onChange={(event) => updateVendorComparison(index, 'scope_and_notes', event.target.value)} className="input" /></td>
                    <td className="px-3 py-3"><input disabled={isPurchaseOrderLocked} value={comparison.payment_terms ?? ''} onChange={(event) => updateVendorComparison(index, 'payment_terms', event.target.value)} className="input" /></td>
                    <td className="px-3 py-3"><input disabled={isPurchaseOrderLocked} value={comparison.estimate_file_url ?? ''} onChange={(event) => updateVendorComparison(index, 'estimate_file_url', event.target.value)} className="input" /></td>
                    <td className="px-3 py-3 text-center"><input disabled={isPurchaseOrderLocked} type="radio" checked={comparison.is_selected} onChange={() => updateVendorComparison(index, 'is_selected', true)} /></td>
                    <td className="px-3 py-3"><input disabled={isPurchaseOrderLocked} value={comparison.selection_reason ?? ''} onChange={(event) => updateVendorComparison(index, 'selection_reason', event.target.value)} className="input" /></td>
                    <td className={`px-3 py-3 font-semibold ${varianceClass(variance)}`}>{formatWon(variance)}</td>
                    <td className="px-3 py-3">
                      <button type="button" disabled={isPurchaseOrderLocked} onClick={() => removeVendorComparison(index)} className="rounded-md p-2 text-slate-500 hover:bg-rose-50 hover:text-rose-700 disabled:cursor-not-allowed disabled:opacity-40" aria-label="행 삭제">
                        <Trash2 className="h-4 w-4" aria-hidden="true" />
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
        <button type="button" disabled={isPurchaseOrderLocked} onClick={addVendorComparison} className="mt-4 inline-flex items-center gap-2 rounded-md border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40">
          <Plus className="h-4 w-4" aria-hidden="true" />
          업체 행 추가
        </button>
      </div>
    </div>
  )
}

