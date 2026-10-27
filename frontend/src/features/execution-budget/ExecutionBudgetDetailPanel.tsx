import { AlertTriangle, ArrowLeft, ArrowRight, Download, Plus, Save, Trash2, Upload } from 'lucide-react'
import { useState } from 'react'
import { MetricPanel } from '../../components/ui/Metrics'
import { MoneyInput } from '../../components/ui/MoneyInput'
import { formatWon } from '../../lib/format'
import { apiUrl } from '../../lib/api-client'
import type { ContractFile, ExecutionBudgetDetail, ExecutionBudgetDetailResponse, ExecutionBudgetSummaryItem } from '../../types/domain'

const costTypes = ['외주비', '폐기물', '인건비', '장비비', '운반비', '자재비', '현장경비']

export function ExecutionBudgetDetailPanel({
  executionBudget,
  details,
  summary,
  totalAmount,
  saveExecutionBudgetDetails,
  requestExecutionBudgetApproval,
  approvalFiles,
  setApprovalFiles,
  contractFiles,
  updateExecutionDetail,
  addExecutionDetail,
  removeExecutionDetail,
}: {
  executionBudget: ExecutionBudgetDetailResponse
  details: ExecutionBudgetDetail[]
  summary: ExecutionBudgetSummaryItem[]
  totalAmount: number
  saveExecutionBudgetDetails: () => void
  requestExecutionBudgetApproval: () => void
  approvalFiles: File[]
  setApprovalFiles: (files: File[]) => void
  contractFiles: ContractFile[]
  updateExecutionDetail: (index: number, field: keyof ExecutionBudgetDetail, value: string) => void
  addExecutionDetail: (copyIndex?: number) => void
  removeExecutionDetail: (index: number) => void
}) {
  const pageSize = 10
  const [currentPage, setCurrentPage] = useState(1)
  const pageCount = Math.max(1, Math.ceil(details.length / pageSize))
  const safeCurrentPage = Math.min(currentPage, pageCount)
  const pageStart = (safeCurrentPage - 1) * pageSize
  const pagedDetails = details.slice(pageStart, pageStart + pageSize)
  const isLocked = executionBudget.approval_status === 'REQUESTED' || (executionBudget.approval_status === 'APPROVED' && !executionBudget.execution_budget_rewrite_available)
  // 상단 지표와 표 하단 합계는 모두 현재 표시 중인 상세 행의 수량 × 단가 합계를 사용한다.
  // 승인 이력의 집계값은 변경 전 항목을 포함할 수 있어 상세 행 합계와 달라질 수 있다.
  const displayedBudgetAmount = totalAmount
  const appliedRevisionLabel = executionBudget.applied_revision_number ? `${executionBudget.applied_revision_number}차 승인본` : '최초 승인본'

  return (
    <div>
      <div className="flex flex-col gap-4 border-b border-slate-200 pb-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2"><h3 className="text-lg font-semibold text-slate-950">세부실행예산</h3><span className="rounded-full bg-cyan-100 px-2.5 py-1 text-xs font-bold text-cyan-800">{appliedRevisionLabel}</span></div>
          <p className="mt-1 text-sm text-slate-500">승인된 변경 차수가 있으면 해당 차수의 세부 항목과 금액을 표시합니다. 최초 원본은 변경이력에 보존됩니다.</p>
        </div>
        <button
          type="button"
          onClick={saveExecutionBudgetDetails}
          disabled={isLocked}
          className="inline-flex items-center justify-center gap-2 rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-300"
        >
          <Save className="h-4 w-4" aria-hidden="true" />
          세부실행예산 저장
        </button>
      </div>

      <div className="grid gap-3 border-b border-slate-200 py-5 md:grid-cols-4">
        <MetricPanel label="프로젝트" value={executionBudget.project_code} helper={executionBudget.project_name} />
        <MetricPanel label="계약 공급가액" value={formatWon(executionBudget.contract_supply_amount)} helper="고객 계약 기준" />
        <MetricPanel label="목표원가한도" value={formatWon(executionBudget.target_cost_limit)} helper="목표이익률 기준" />
        <MetricPanel
          label="세부실행예산 총합계"
          value={formatWon(displayedBudgetAmount)}
          helper={`${appliedRevisionLabel} · 결재상태 ${executionBudget.approval_status}`}
          accent={executionBudget.over_target_limit || displayedBudgetAmount > executionBudget.target_cost_limit ? 'text-rose-700' : 'text-cyan-700'}
        />
      </div>

      {executionBudget.execution_budget_rewrite_available && (
        <div className="mt-4 rounded-md border border-cyan-200 bg-cyan-50 px-4 py-3 text-sm text-cyan-900">
          승인된 계약 변경 차수 기준으로 세부실행예산을 다시 작성할 수 있습니다. 저장 후 실행예산 결재를 다시 요청하면 항목이 고정됩니다.
        </div>
      )}

      {contractFiles.length > 0 && (
        <div className="border-b border-slate-200 py-5">
          <h3 className="text-sm font-semibold text-slate-900">계약관리 첨부파일</h3>
          <p className="mt-1 text-sm text-slate-500">계약관리에서 등록한 견적서·계약서를 내려받을 수 있습니다.</p>
          <div className="mt-3 divide-y rounded-md border border-slate-200 bg-white">
            {contractFiles.map((file) => (
              <a
                key={file.file_id}
                href={apiUrl(`${file.download_url}${file.download_url.includes('?') ? '&' : '?'}source=execution-budget`)}
                className="flex items-center justify-between gap-3 px-4 py-3 text-sm hover:bg-slate-50"
              >
                <span>
                  <span className="font-semibold text-slate-900">{file.file_name}</span>
                  <span className="ml-2 rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">
                    {file.file_type}
                  </span>
                </span>
                <Download className="h-4 w-4 shrink-0 text-slate-400" aria-hidden="true" />
              </a>
            ))}
          </div>
        </div>
      )}

      {(executionBudget.over_target_limit || displayedBudgetAmount > executionBudget.target_cost_limit) && executionBudget.target_cost_limit > 0 && (
        <div className="mt-4 flex items-start gap-3 rounded-md border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
          <p>세부실행예산 총합계가 목표원가한도를 초과했습니다.</p>
        </div>
      )}

      <div className="border-b border-slate-200 py-5">
        <h3 className="text-sm font-semibold text-slate-900">실행항목ID별 자동 집계</h3>
        <div className="mt-3 overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="whitespace-nowrap bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-3 py-3">실행항목ID</th>
                <th className="px-3 py-3">공종</th>
                <th className="px-3 py-3">실행항목</th>
                <th className="px-3 py-3">집계금액</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {summary.map((item) => (
                <tr key={item.budget_item_id}>
                  <td className="px-3 py-3 font-semibold text-slate-900">{item.budget_item_id}</td>
                  <td className="px-3 py-3 text-slate-600">{item.category_name}</td>
                  <td className="px-3 py-3 text-slate-600">{item.item_name}</td>
                  <td className="px-3 py-3 font-semibold text-slate-900">{formatWon(item.rolled_up_amount)}</td>
                </tr>
              ))}
              {summary.length === 0 && (
                <tr>
                  <td className="px-3 py-6 text-center text-slate-500" colSpan={4}>
                    집계할 세부실행예산 항목이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="py-5">
        <h3 className="mb-3 text-sm font-semibold text-slate-900">{appliedRevisionLabel} 세부실행예산 항목</h3>
        <div className="mb-3 flex justify-end text-xs font-medium text-slate-500">
          {details.length > 0 ? `${pageStart + 1}-${Math.min(pageStart + pageSize, details.length)} / ${details.length}` : '0 / 0'}
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-[1170px] text-left text-sm">
            <thead className="whitespace-nowrap bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
				<th className="px-3 py-3">순번</th>
                <th className="px-3 py-3">프로젝트</th>
                <th className="px-3 py-3">실행항목ID</th>
                <th className="px-3 py-3">공종</th>
                <th className="px-3 py-3">원가구분</th>
                <th className="px-3 py-3">규격/단위</th>
                <th className="px-3 py-3">수량</th>
                <th className="px-3 py-3">단가</th>
                <th className="px-3 py-3">금액</th>
                <th className="px-3 py-3">비고</th>
                <th className="px-3 py-3"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {pagedDetails.map((detail, pageIndex) => {
                const index = pageStart + pageIndex
                return (
                <tr key={detail.detail_id ?? index}>
                  <td className="px-3 py-3 text-center font-semibold text-slate-500">{index + 1}</td>
                  <td className="px-3 py-3 text-slate-600">{executionBudget.project_code}</td>
                  <td className="px-3 py-3">
                    <input disabled={isLocked} value={detail.budget_item_id} onChange={(event) => updateExecutionDetail(index, 'budget_item_id', event.target.value)} className="input !w-40 disabled:cursor-not-allowed disabled:bg-slate-100" />
                  </td>
                  <td className="px-3 py-3">
                    <input disabled={isLocked} value={detail.category_name} onChange={(event) => updateExecutionDetail(index, 'category_name', event.target.value)} className="input !w-64 disabled:cursor-not-allowed disabled:bg-slate-100" />
                  </td>
                  <td className="px-3 py-3">
                    <select disabled={isLocked} value={detail.cost_type} onChange={(event) => updateExecutionDetail(index, 'cost_type', event.target.value)} className="input !w-36 disabled:cursor-not-allowed disabled:bg-slate-100">
                      {costTypes.map((type) => (
                        <option key={type} value={type}>{type}</option>
                      ))}
                    </select>
                  </td>
                  <td className="px-3 py-3">
                    <input disabled={isLocked} value={detail.unit ?? ''} onChange={(event) => updateExecutionDetail(index, 'unit', event.target.value)} className="input w-24 disabled:cursor-not-allowed disabled:bg-slate-100" placeholder="예: EA" />
                  </td>
                  <td className="px-3 py-3">
                    <input disabled={isLocked} type="number" min="0" step="any" value={detail.quantity} onChange={(event) => updateExecutionDetail(index, 'quantity', event.target.value)} inputMode="decimal" className="input !w-32 disabled:cursor-not-allowed disabled:bg-slate-100" />
                  </td>
                  <td className="px-3 py-3">
                    <MoneyInput disabled={isLocked} value={detail.unit_price} onChange={(value) => updateExecutionDetail(index, 'unit_price', value)} suffix="" className="w-36" />
                  </td>
                  <td className="px-3 py-3 font-semibold text-slate-900">{formatWon((detail.quantity || 0) * (detail.unit_price || 0))}</td>
                  <td className="px-3 py-3">
                    <input disabled={isLocked} value={detail.note ?? ''} onChange={(event) => updateExecutionDetail(index, 'note', event.target.value)} className="input !w-56 disabled:cursor-not-allowed disabled:bg-slate-100" />
                  </td>
                  <td className="px-3 py-3">
                    <div className="flex gap-1">
                      <button type="button" disabled={isLocked} onClick={() => addExecutionDetail(index)} className="rounded-md p-2 text-slate-500 hover:bg-cyan-50 hover:text-cyan-700 disabled:cursor-not-allowed disabled:opacity-40" aria-label="행 복사">
                        <Plus className="h-4 w-4" aria-hidden="true" />
                      </button>
                      <button type="button" disabled={isLocked} onClick={() => removeExecutionDetail(index)} className="rounded-md p-2 text-slate-500 hover:bg-rose-50 hover:text-rose-700 disabled:cursor-not-allowed disabled:opacity-40" aria-label="행 삭제">
                        <Trash2 className="h-4 w-4" aria-hidden="true" />
                      </button>
                    </div>
                  </td>
                </tr>
                )
              })}
              <tr className="bg-slate-50 font-semibold text-slate-900">
                <td className="px-3 py-3" colSpan={8}>합계</td>
                <td className="px-3 py-3">{formatWon(totalAmount)}</td>
                <td className="px-3 py-3" colSpan={2}></td>
              </tr>
            </tbody>
          </table>
        </div>
        {pageCount > 1 && (
          <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-xs font-medium text-slate-500">10개씩 페이지 처리</p>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setCurrentPage(Math.max(1, safeCurrentPage - 1))}
                disabled={safeCurrentPage === 1}
                className="inline-flex items-center gap-1 rounded-md border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
              >
                <ArrowLeft className="h-4 w-4" aria-hidden="true" />
                이전
              </button>
              <span className="min-w-16 text-center text-sm font-semibold text-slate-700">
                {safeCurrentPage} / {pageCount}
              </span>
              <button
                type="button"
                onClick={() => setCurrentPage(Math.min(pageCount, safeCurrentPage + 1))}
                disabled={safeCurrentPage === pageCount}
                className="inline-flex items-center gap-1 rounded-md border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
              >
                다음
                <ArrowRight className="h-4 w-4" aria-hidden="true" />
              </button>
            </div>
          </div>
        )}
        <div className="mt-4 flex flex-wrap items-center gap-2">
          <label className="inline-flex cursor-pointer items-center gap-2 rounded-md border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 has-[:disabled]:cursor-not-allowed has-[:disabled]:opacity-50">
            <Upload className="h-4 w-4" aria-hidden="true" />
            결재 첨부파일 선택
            <input
              type="file"
              multiple
              disabled={isLocked}
              className="hidden"
              onChange={(event) => {
                setApprovalFiles(Array.from(event.target.files ?? []))
                event.currentTarget.value = ''
              }}
            />
          </label>
          {approvalFiles.length > 0 && (
            <span className="text-sm text-slate-600">{approvalFiles.map((file) => file.name).join(', ')}</span>
          )}
        </div>
        <button
          type="button"
          onClick={requestExecutionBudgetApproval}
          disabled={isLocked || details.length === 0 || totalAmount <= 0}
          className="mt-4 mr-2 inline-flex items-center gap-2 rounded-md bg-cyan-700 px-3 py-2 text-sm font-semibold text-white hover:bg-cyan-800 disabled:cursor-not-allowed disabled:bg-slate-300"
        >
          결재요청
        </button>
        <button
          type="button"
          onClick={() => addExecutionDetail()}
          disabled={isLocked}
          className="mt-4 inline-flex items-center gap-2 rounded-md border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
        >
          <Plus className="h-4 w-4" aria-hidden="true" />
          행 추가
        </button>
      </div>
    </div>
  )
}

