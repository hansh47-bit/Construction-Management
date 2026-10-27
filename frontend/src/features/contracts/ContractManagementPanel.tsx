import { CheckCircle2, Download, Plus, Save, Trash2, Upload } from 'lucide-react'
import { useState } from 'react'
import { MetricPanel } from '../../components/ui/Metrics'
import { MoneyInput } from '../../components/ui/MoneyInput'
import { apiUrl } from '../../lib/api-client'
import { formatWon } from '../../lib/format'
import type { CustomerContract, CustomerContractItem } from '../../types/domain'

export function ContractManagementPanel({
  contract,
  contractItems,
  contractTotal,
  targetProfitRate,
  selectedFileType,
  setTargetProfitRate,
  setSelectedFileType,
  saveCustomerContract,
  openChangeManagement,
  uploadContractFile,
  deleteContractFile,
  updateContractItem,
  addContractItem,
  removeContractItem,
}: {
  contract: CustomerContract
  contractItems: CustomerContractItem[]
  contractTotal: { supply: number; vat: number; total: number; targetCostLimit: number }
  targetProfitRate: string
  selectedFileType: string
  setTargetProfitRate: (value: string) => void
  setSelectedFileType: (value: string) => void
  saveCustomerContract: () => Promise<boolean>
  openChangeManagement: () => void
  uploadContractFile: (file: File | null) => void
  deleteContractFile: (fileId: string) => void
  updateContractItem: (index: number, field: keyof CustomerContractItem, value: string) => void
  addContractItem: () => void
  removeContractItem: (index: number) => void
}) {
  const [isSaving, setIsSaving] = useState(false)
  const [saveNotice, setSaveNotice] = useState<string | null>(null)

  async function handleSaveCustomerContract() {
    if (isSaving) return

    setIsSaving(true)
    setSaveNotice(null)
    try {
      if (await saveCustomerContract()) {
        setSaveNotice('계약 정보가 저장되었습니다.')
      }
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div>
      <div className="flex flex-col gap-4 border-b border-slate-200 pb-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 className="text-lg font-semibold text-slate-950">고객 견적·계약 관리</h3>
          <p className="mt-1 text-sm text-slate-500">공급가액 기준 계약금액과 목표이익률로 목표원가한도를 계산합니다.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={openChangeManagement}
            className="inline-flex items-center justify-center gap-2 rounded-md border border-amber-500 bg-amber-50 px-4 py-2 text-sm font-semibold text-amber-800 hover:bg-amber-100"
          >
            계약 변경안 작성
          </button>
          <button
            type="button"
            onClick={() => void handleSaveCustomerContract()}
            disabled={isSaving}
            className="inline-flex items-center justify-center gap-2 rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-800 disabled:cursor-wait disabled:opacity-70"
          >
            <Save className="h-4 w-4" aria-hidden="true" />
            계약 저장
          </button>
        </div>
      </div>

      {saveNotice && (
        <div className="mt-4 flex items-center gap-2 rounded-md border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-800" role="status">
          <CheckCircle2 className="h-4 w-4 shrink-0" aria-hidden="true" />
          <p>{saveNotice}</p>
        </div>
      )}

      <div className="grid gap-4 border-b border-slate-200 py-5 lg:grid-cols-[260px_1fr_1fr]">
        <label className="block text-sm font-semibold text-slate-700">
          목표이익률 (%)
          <input
            type="number"
            step="0.1"
            value={targetProfitRate}
            onChange={(event) => setTargetProfitRate(event.target.value)}
            className="input mt-2"
          />
        </label>
        <MetricPanel
          label="계약 총 공급가액"
          value={formatWon(contract.current_contract_supply_amount ?? contractTotal.supply)}
          helper={`최초 ${formatWon(contract.total_contract_supply_amount)} · VAT ${formatWon(contractTotal.vat)}`}
        />
        <MetricPanel
          label="목표원가한도"
          value={formatWon(contract.target_cost_limit)}
          helper="현재 계약 공급가액 × (1 - 목표이익률)"
          accent="text-cyan-700"
        />
      </div>

      <div className="py-5">
        <div className="overflow-x-auto">
          <table className="min-w-[1100px] text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-3 py-3">공종</th>
                <th className="px-3 py-3">견적 제출 공급가액</th>
                <th className="px-3 py-3">최종 계약 공급가액</th>
                <th className="px-3 py-3">VAT (10%)</th>
                <th className="px-3 py-3">VAT 포함 총액</th>
                <th className="px-3 py-3">비고</th>
                <th className="px-3 py-3"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {contractItems.map((item, index) => (
                <tr key={item.item_id ?? index}>
                  <td className="px-3 py-3">
                    <input
                      value={item.work_category}
                      onChange={(event) => updateContractItem(index, 'work_category', event.target.value)}
                      className="input"
                      placeholder="공종"
                    />
                  </td>
                  <td className="px-3 py-3">
                    <MoneyInput value={item.quoted_amount} onChange={(value) => updateContractItem(index, 'quoted_amount', value)} />
                  </td>
                  <td className="px-3 py-3">
                    <MoneyInput value={item.contract_amount} onChange={(value) => updateContractItem(index, 'contract_amount', value)} />
                  </td>
                  <td className="px-3 py-3">
                    <MoneyInput value={item.vat_amount} onChange={(value) => updateContractItem(index, 'vat_amount', value)} />
                  </td>
                  <td className="px-3 py-3 font-semibold text-slate-900">
                    {formatWon((item.contract_amount || 0) + (item.vat_amount || 0))}
                  </td>
                  <td className="px-3 py-3">
                    <input
                      value={item.note ?? ''}
                      onChange={(event) => updateContractItem(index, 'note', event.target.value)}
                      className="input"
                      placeholder="비고"
                    />
                  </td>
                  <td className="px-3 py-3">
                    <button
                      type="button"
                      onClick={() => removeContractItem(index)}
                      className="rounded-md p-2 text-slate-500 hover:bg-rose-50 hover:text-rose-700"
                      aria-label="행 삭제"
                    >
                      <Trash2 className="h-4 w-4" aria-hidden="true" />
                    </button>
                  </td>
                </tr>
              ))}
              <tr className="bg-slate-50 font-semibold text-slate-900">
                <td className="px-3 py-3">합계</td>
                <td className="px-3 py-3">{formatWon(contractItems.reduce((sum, item) => sum + (item.quoted_amount || 0), 0))}</td>
                <td className="px-3 py-3">{formatWon(contractTotal.supply)}</td>
                <td className="px-3 py-3">{formatWon(contractTotal.vat)}</td>
                <td className="px-3 py-3">{formatWon(contractTotal.total)}</td>
                <td className="px-3 py-3" colSpan={2}></td>
              </tr>
            </tbody>
          </table>
          <datalist id="work-categories">
            {['인테리어', '전기', '사인', '냉난방', '철거', '목공', '설비', '잡자재/예비비'].map((category) => (
              <option key={category} value={category} />
            ))}
          </datalist>
        </div>

        <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
          <button
            type="button"
            onClick={addContractItem}
            className="inline-flex items-center gap-2 rounded-md border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
          >
            <Plus className="h-4 w-4" aria-hidden="true" />
            행 추가
          </button>
          <p className="text-sm text-slate-500">VAT는 계약 공급가액 입력 시 10%로 자동 계산되며 직접 수정할 수 있습니다.</p>
        </div>
      </div>

      <div className="border-t border-slate-200 pt-5">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
          <div className="min-w-0">
            <h3 className="text-sm font-semibold text-slate-900">견적/계약 증빙 파일</h3>
            <p className="mt-1 text-sm text-slate-500">제출 견적서와 최종 계약서 파일을 프로젝트에 첨부합니다.</p>
          </div>
          <div className="flex shrink-0 items-center gap-2 whitespace-nowrap">
            <select value={selectedFileType} onChange={(event) => setSelectedFileType(event.target.value)} className="input w-36">
              <option value="ESTIMATE">견적서</option>
              <option value="CONTRACT">계약서</option>
            </select>
            <label className="inline-flex cursor-pointer items-center gap-2 rounded-md border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">
              <Upload className="h-4 w-4" aria-hidden="true" />
              파일 선택
              <input
                type="file"
                className="hidden"
                onChange={(event) => {
                  uploadContractFile(event.target.files?.[0] ?? null)
                  event.currentTarget.value = ''
                }}
              />
            </label>
          </div>
        </div>
        <div className="mt-3 divide-y divide-slate-100 rounded-md border border-slate-200">
          {(contract.files ?? []).length === 0 ? (
            <p className="px-4 py-4 text-sm text-slate-500">첨부된 파일이 없습니다.</p>
          ) : (
            contract.files.map((file) => (
              <div key={file.file_id} className="flex items-center justify-between gap-3 px-4 py-3 text-sm hover:bg-slate-50">
                <span className="min-w-0">
                  <span className="font-semibold text-slate-900">{file.file_name}</span>
                  <span className="ml-2 rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">
                    {file.file_type}
                  </span>
                </span>
                <span className="flex shrink-0 items-center gap-1">
                  <a
                    href={apiUrl(file.download_url)}
                    className="rounded-md p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900"
                    aria-label={`${file.file_name} 다운로드`}
                    title="다운로드"
                  >
                    <Download className="h-4 w-4" aria-hidden="true" />
                  </a>
                  <button
                    type="button"
                    onClick={() => {
                      if (window.confirm(`'${file.file_name}' 파일을 삭제하시겠습니까?`)) {
                        deleteContractFile(file.file_id)
                      }
                    }}
                    className="rounded-md p-2 text-slate-500 hover:bg-rose-50 hover:text-rose-700"
                    aria-label={`${file.file_name} 삭제`}
                    title="삭제"
                  >
                    <Trash2 className="h-4 w-4" aria-hidden="true" />
                  </button>
                </span>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}

