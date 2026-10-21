import { useEffect, useMemo, useState } from 'react'
import { CheckCircle2, PlusCircle, Save } from 'lucide-react'
import { formatDateTime, formatWorkflowStatus, formatWon, toNumber } from '../../lib/format'
import type { ProjectRevision, ProjectRevisionBudgetDetail, ProjectRevisionContractItem } from '../../types/domain'
import { revisionApi } from './revision-api'

const emptyContractItem: ProjectRevisionContractItem = { work_category: '', quoted_amount: 0, contract_amount: 0, vat_amount: 0, note: '' }
const emptyBudgetDetail: ProjectRevisionBudgetDetail = { budget_item_id: '', category_name: '', item_name: '', cost_type: '', vendor_description: '', unit: '식', quantity: 1, unit_price: 0, evidence_link: '', note: '' }

type RevisionStep = 1 | 2 | 3

function availableStep(revision: ProjectRevision): RevisionStep {
  if (revision.status === 'BUDGET_WRITTEN' || revision.status === 'REQUESTED' || revision.status === 'APPROVED' || revision.status === 'REJECTED') return 3
  if (revision.status === 'CONTRACT_WRITTEN') return 2
  return 1
}

export function RevisionManagementPanel({ projectId, projectCode, projectName }: { projectId: string; projectCode?: string; projectName?: string }) {
  const [revisions, setRevisions] = useState<ProjectRevision[]>([])
  const [active, setActive] = useState<ProjectRevision | null>(null)
  const [step, setStep] = useState<RevisionStep>(1)
  const [title, setTitle] = useState('')
  const [reason, setReason] = useState('')
  const [contractItems, setContractItems] = useState<ProjectRevisionContractItem[]>([])
  const [budgetDetails, setBudgetDetails] = useState<ProjectRevisionBudgetDetail[]>([])
  const [targetProfitRate, setTargetProfitRate] = useState('0')
  const [message, setMessage] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  function selectRevision(revision: ProjectRevision, selectedStep?: RevisionStep) {
    setActive(revision)
    setTitle(revision.title)
    setReason(revision.reason)
    setContractItems(revision.contract_items)
    setBudgetDetails(revision.budget_details)
    setTargetProfitRate(String(revision.target_profit_rate))
    setStep(selectedStep ?? availableStep(revision))
  }

  async function load() {
    const response = await revisionApi.list(projectId)
    if (!response.ok) return
    const data: ProjectRevision[] = await response.json()
    setRevisions(data)
    if (active) {
      const refreshed = data.find((item) => item.revision_id === active.revision_id)
      if (refreshed) selectRevision(refreshed)
    }
  }

  useEffect(() => { void load() }, [projectId])

  const contractTotal = useMemo(() => contractItems.reduce((sum, item) => sum + item.contract_amount, 0), [contractItems])
  const budgetTotal = useMemo(() => budgetDetails.reduce((sum, item) => sum + item.quantity * item.unit_price, 0), [budgetDetails])
  const nextRevision = Math.max(1, ...revisions.map((item) => item.revision_number)) + 1
  const completedStep = active ? availableStep(active) : 0

  async function createRevision() {
    if (!title.trim() || !reason.trim()) { setMessage('차수 제목과 변경 사유를 입력해 주세요.'); return }
    setIsSaving(true)
    try {
      const response = await revisionApi.create(projectId, { title: title.trim(), reason: reason.trim() })
      if (!response.ok) throw new Error(await response.text())
      const created: ProjectRevision = await response.json()
      setRevisions((items) => [created, ...items])
      selectRevision(created, 1)
      setMessage(`${created.revision_number}차가 생성되었습니다. 1단계 계약관리부터 작성해 주세요.`)
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '차수 생성에 실패했습니다.')
    } finally {
      setIsSaving(false)
    }
  }

  async function saveContract() {
    if (!active) return
    setIsSaving(true)
    try {
      const response = await revisionApi.saveContract(projectId, active.revision_id, { target_profit_rate: toNumber(targetProfitRate), items: contractItems })
      if (!response.ok) throw new Error(await response.text())
      const saved: ProjectRevision = await response.json()
      setRevisions((items) => items.map((item) => item.revision_id === saved.revision_id ? saved : item))
      selectRevision(saved, 2)
      setMessage('계약관리 전체 금액을 저장했습니다. 이제 2단계 세부실행예산을 작성해 주세요.')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '계약관리 저장에 실패했습니다.')
    } finally {
      setIsSaving(false)
    }
  }

  async function saveBudget() {
    if (!active) return
    setIsSaving(true)
    try {
      const response = await revisionApi.saveBudget(projectId, active.revision_id, { details: budgetDetails })
      if (!response.ok) throw new Error(await response.text())
      const saved: ProjectRevision = await response.json()
      setRevisions((items) => items.map((item) => item.revision_id === saved.revision_id ? saved : item))
      selectRevision(saved, 3)
      setMessage('세부실행예산 전체를 저장했습니다. 3단계에서 직전 차수와 비교할 수 있습니다.')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '세부실행예산 저장에 실패했습니다.')
    } finally {
      setIsSaving(false)
    }
  }

  async function requestApproval() {
    if (!active) return
    setIsSaving(true)
    try {
      const response = await revisionApi.requestApproval(projectId, active.revision_id)
      if (!response.ok) throw new Error(await response.text())
      const updated = { ...active, status: 'REQUESTED' }
      setRevisions((items) => items.map((item) => item.revision_id === updated.revision_id ? updated : item))
      selectRevision(updated, 3)
      setMessage(`${active.revision_number}차 실행예산 전체안을 통합결재함으로 올렸습니다. 대표 승인 후 해당 차수의 전체 실행예산 항목을 기준으로 업체 비교견적·발주를 진행합니다.`)
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '결재 요청에 실패했습니다.')
    } finally {
      setIsSaving(false)
    }
  }

  return <div className="space-y-6">
    <section className="rounded-md border border-slate-200 bg-white p-4">
      <h3 className="text-lg font-semibold text-slate-950">계약·실행예산 차수 관리</h3>
      <p className="mt-1 text-sm text-slate-500">직전 확정 계약을 기준으로 계약관리 전체를 작성한 뒤, 세부실행예산은 빈 예산본부터 전부 다시 작성합니다. 비교는 두 단계 작성이 끝난 뒤에만 표시됩니다.</p>
      <div className="mt-4 rounded-md border border-cyan-100 bg-cyan-50 px-4 py-3 text-sm"><span className="font-semibold text-cyan-900">대상 프로젝트</span><span className="ml-3 font-bold text-slate-950">{projectCode ?? projectId}</span><span className="ml-2 text-slate-700">{projectName ?? ''}</span></div>
      {!active && <div className="mt-4 grid gap-3 md:grid-cols-[1fr_1fr_auto]">
        <label className="text-sm font-semibold text-slate-700">{nextRevision}차 제목<input className="input mt-2" value={title} onChange={(event) => setTitle(event.target.value)} placeholder={`${nextRevision}차 계약·실행예산`} /></label>
        <label className="text-sm font-semibold text-slate-700">변경 사유<input className="input mt-2" value={reason} onChange={(event) => setReason(event.target.value)} placeholder="변경 사유" /></label>
        <button type="button" disabled={isSaving} onClick={() => void createRevision()} className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md bg-slate-900 px-4 text-sm font-semibold text-white disabled:bg-slate-300"><PlusCircle className="h-4 w-4" />새 차수 작성</button>
      </div>}
    </section>

    {revisions.length > 0 && <section className="rounded-md border border-slate-200 bg-white p-4"><div><h3 className="font-semibold text-slate-950">차수별 변경 이력</h3><p className="mt-1 text-sm text-slate-500">어떤 프로젝트의 계약과 세부실행예산이 어떻게 바뀌었는지 차수별로 확인합니다.</p></div><div className="mt-4 grid gap-3 lg:grid-cols-2">{revisions.map((revision) => <RevisionHistoryCard key={revision.revision_id} revision={revision} selected={active?.revision_id === revision.revision_id} onClick={() => selectRevision(revision)} />)}</div></section>}

    {message && <p className="rounded-md border border-cyan-200 bg-cyan-50 px-4 py-3 text-sm text-cyan-900">{message}</p>}

    {active && <>
      <ol className="grid gap-2 sm:grid-cols-3">
        <StepButton number={1} label="계약관리 작성" enabled={active.status === 'DRAFT' || active.status === 'CONTRACT_WRITTEN' || active.status === 'BUDGET_WRITTEN'} onClick={() => setStep(1)} active={step === 1} />
        <StepButton number={2} label="세부실행예산 작성" enabled={active.status === 'CONTRACT_WRITTEN' || active.status === 'BUDGET_WRITTEN'} onClick={() => setStep(2)} active={step === 2} />
        <StepButton number={3} label="변경금액 비교" enabled={completedStep >= 3} onClick={() => setStep(3)} active={step === 3} />
      </ol>

      {step === 1 && <section className="rounded-md border border-slate-200 bg-white p-4">
        <div className="flex flex-wrap items-end justify-between gap-3 border-b border-slate-200 pb-3"><div><h3 className="font-semibold text-slate-950">1단계 · {active.revision_number}차 고객견적·계약관리</h3><p className="mt-1 text-sm text-slate-500">증감액이 아닌 {active.revision_number}차 계약 전체금액을 작성합니다. 저장하면 다음 단계로 이동합니다.</p></div><button type="button" disabled={isSaving} onClick={() => void saveContract()} className="inline-flex items-center gap-2 rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white disabled:bg-slate-300"><Save className="h-4 w-4" />계약 전체 저장 후 다음</button></div>
        <label className="mt-4 block max-w-xs text-sm font-semibold text-slate-700">목표이익률(%)<input className="input mt-2" value={targetProfitRate} onChange={(event) => setTargetProfitRate(event.target.value)} inputMode="decimal" /></label>
        <div className="mt-4 overflow-x-auto"><table className="min-w-[900px] text-sm"><thead className="bg-slate-50 text-xs text-slate-500"><tr><th className="px-3 py-2 text-left">공종</th><th className="px-3 py-2 text-right">견적금액</th><th className="px-3 py-2 text-right">계약금액</th><th className="px-3 py-2 text-left">비고</th></tr></thead><tbody>{contractItems.map((item, index) => <tr key={index} className="border-t border-slate-100"><td className="px-3 py-2"><input className="input" value={item.work_category} onChange={(event) => setContractItems((items) => items.map((value, i) => i === index ? { ...value, work_category: event.target.value } : value))} /></td><td className="px-3 py-2"><input className="input text-right" value={item.quoted_amount} inputMode="numeric" onChange={(event) => setContractItems((items) => items.map((value, i) => i === index ? { ...value, quoted_amount: toNumber(event.target.value) } : value))} /></td><td className="px-3 py-2"><input className="input text-right" value={item.contract_amount} inputMode="numeric" onChange={(event) => setContractItems((items) => items.map((value, i) => i === index ? { ...value, contract_amount: toNumber(event.target.value), vat_amount: toNumber(event.target.value) * 0.1 } : value))} /></td><td className="px-3 py-2"><input className="input" value={item.note ?? ''} onChange={(event) => setContractItems((items) => items.map((value, i) => i === index ? { ...value, note: event.target.value } : value))} /></td></tr>)}</tbody></table></div>
        <button type="button" onClick={() => setContractItems((items) => [...items, emptyContractItem])} className="mt-3 text-sm font-semibold text-cyan-700">+ 계약 공종 추가</button><p className="mt-2 text-right text-sm font-semibold">계약 공급가 합계: {formatWon(contractTotal)}</p>
      </section>}

      {step === 2 && <section className="rounded-md border border-slate-200 bg-white p-4">
        <div className="flex flex-wrap items-end justify-between gap-3 border-b border-slate-200 pb-3"><div><h3 className="font-semibold text-slate-950">2단계 · {active.revision_number}차 세부실행예산 전체 작성</h3><p className="mt-1 text-sm text-slate-500">이전 세부실행예산을 복사하지 않습니다. 신규 공종을 포함해 {active.revision_number}차 실행예산 전체를 처음부터 작성하고, 전체 결재를 요청합니다.</p></div><button type="button" disabled={isSaving} onClick={() => void saveBudget()} className="inline-flex items-center gap-2 rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white disabled:bg-slate-300"><Save className="h-4 w-4" />실행예산 전체 저장 후 비교</button></div>
        <div className="mt-4 overflow-x-auto"><table className="min-w-[1180px] text-sm"><thead className="bg-slate-50 text-xs text-slate-500"><tr><th className="px-2 py-2">ID</th><th className="px-2 py-2">공종</th><th className="px-2 py-2">항목</th><th className="px-2 py-2">원가구분</th><th className="px-2 py-2 text-right">수량</th><th className="px-2 py-2 text-right">단가</th><th className="px-2 py-2 text-right">금액</th></tr></thead><tbody>{budgetDetails.map((item, index) => <tr key={index} className="border-t border-slate-100"><td className="px-2 py-2"><input className="input" value={item.budget_item_id} onChange={(event) => setBudgetDetails((items) => items.map((value, i) => i === index ? { ...value, budget_item_id: event.target.value } : value))} /></td><td className="px-2 py-2"><input className="input" value={item.category_name} onChange={(event) => setBudgetDetails((items) => items.map((value, i) => i === index ? { ...value, category_name: event.target.value } : value))} /></td><td className="px-2 py-2"><input className="input" value={item.item_name} onChange={(event) => setBudgetDetails((items) => items.map((value, i) => i === index ? { ...value, item_name: event.target.value } : value))} /></td><td className="px-2 py-2"><input className="input" value={item.cost_type} onChange={(event) => setBudgetDetails((items) => items.map((value, i) => i === index ? { ...value, cost_type: event.target.value } : value))} /></td><td className="px-2 py-2"><input className="input text-right" value={item.quantity} inputMode="decimal" onChange={(event) => setBudgetDetails((items) => items.map((value, i) => i === index ? { ...value, quantity: toNumber(event.target.value) } : value))} /></td><td className="px-2 py-2"><input className="input text-right" value={item.unit_price} inputMode="numeric" onChange={(event) => setBudgetDetails((items) => items.map((value, i) => i === index ? { ...value, unit_price: toNumber(event.target.value) } : value))} /></td><td className="px-2 py-2 text-right">{formatWon(toNumber(item.quantity) * toNumber(item.unit_price))}</td></tr>)}</tbody></table></div>
        <button type="button" onClick={() => setBudgetDetails((items) => [...items, emptyBudgetDetail])} className="mt-3 text-sm font-semibold text-cyan-700">+ 세부 실행항목 추가</button><p className="mt-2 text-right text-sm font-semibold">세부 실행예산 합계: {formatWon(budgetTotal)}</p>
      </section>}

      {step === 3 && <section className="rounded-md border border-slate-200 bg-white p-4"><div className="flex flex-wrap items-center justify-between gap-3"><div><h3 className="font-semibold text-slate-950">3단계 · {active.revision_number}차 비교 및 전체 결재</h3><p className="mt-1 text-sm text-slate-500">계약과 세부실행예산 전체 작성이 완료된 차수만 결재 요청합니다. 기존 원본은 수정하지 않습니다.</p></div>{active.status === 'BUDGET_WRITTEN' && <button type="button" disabled={isSaving} onClick={() => void requestApproval()} className="inline-flex items-center gap-2 rounded-md bg-cyan-700 px-4 py-2 text-sm font-semibold text-white disabled:bg-slate-300"><CheckCircle2 className="h-4 w-4" />실행예산 전체 결재 요청</button>}{active.status === 'REQUESTED' && <span className="rounded-md bg-amber-50 px-3 py-2 text-sm font-semibold text-amber-800">실행예산 전체 결재 대기</span>}{active.status === 'APPROVED' && <span className="rounded-md bg-emerald-50 px-3 py-2 text-sm font-semibold text-emerald-800">승인 완료 · {active.revision_number}차 전체 예산본 적용</span>}</div><div className="mt-4 grid gap-3 md:grid-cols-2"><Comparison label="계약 공급가" base={active.base_contract_total} current={active.contract_total} /><Comparison label="세부 실행예산" base={active.base_budget_total} current={active.budget_total} /></div></section>}
    </>}
  </div>
}

function StepButton({ number, label, enabled, active, onClick }: { number: number; label: string; enabled: boolean; active: boolean; onClick: () => void }) {
  return <li><button type="button" disabled={!enabled} onClick={onClick} className={'flex w-full items-center gap-2 rounded-md border px-4 py-3 text-left text-sm font-semibold disabled:cursor-not-allowed disabled:border-slate-100 disabled:bg-slate-50 disabled:text-slate-400 ' + (active ? 'border-cyan-700 bg-cyan-700 text-white' : 'border-slate-200 bg-white text-slate-700')}><CheckCircle2 className="h-4 w-4" />{number}. {label}</button></li>
}

function Comparison({ label, base, current }: { label: string; base: number; current: number }) {
  return <div className="rounded-md bg-slate-50 p-4"><p className="text-sm font-semibold text-slate-700">{label}</p><div className="mt-3 grid grid-cols-2 gap-3 text-sm"><p>직전 차수<strong className="block text-slate-950">{formatWon(base)}</strong></p><p>현재 작성 차수<strong className="block text-cyan-700">{formatWon(current)}</strong></p></div></div>
}

function RevisionHistoryCard({ revision, selected, onClick }: { revision: ProjectRevision; selected: boolean; onClick: () => void }) {
  const contractDelta = revision.contract_total - revision.base_contract_total
  const budgetDelta = revision.budget_total - revision.base_budget_total
  const contractChanged = contractDelta !== 0 || JSON.stringify(revision.base_contract_items) !== JSON.stringify(revision.contract_items)
  const budgetChanged = budgetDelta !== 0 || JSON.stringify(revision.base_budget_details) !== JSON.stringify(revision.budget_details)
  const changeLabel = [contractChanged ? '계약 변경' : '', budgetChanged ? '실행예산 변경' : ''].filter(Boolean).join(' · ') || '작성 중'
  const deltaClass = (value: number) => value > 0 ? 'text-rose-700' : value < 0 ? 'text-blue-700' : 'text-slate-600'
  return <button type="button" onClick={onClick} className={'rounded-md border p-4 text-left transition hover:border-cyan-400 ' + (selected ? 'border-cyan-700 bg-cyan-50 ring-1 ring-cyan-700' : 'border-slate-200 bg-white')}><div className="flex flex-wrap items-start justify-between gap-2"><div><p className="font-bold text-slate-950">{revision.revision_number}차 · {revision.title}</p><p className="mt-1 text-sm text-slate-600">변경 사유: {revision.reason}</p></div><span className="rounded-full bg-slate-100 px-2 py-1 text-xs font-bold text-slate-700">{formatWorkflowStatus(revision.status)}</span></div><div className="mt-4 grid gap-2 sm:grid-cols-2"><HistoryAmount label="계약 공급가" previous={revision.base_contract_total} current={revision.contract_total} delta={contractDelta} className={deltaClass(contractDelta)} /><HistoryAmount label="세부실행예산" previous={revision.base_budget_total} current={revision.budget_total} delta={budgetDelta} className={deltaClass(budgetDelta)} /></div><div className="mt-3 flex flex-wrap items-center justify-between gap-2 border-t border-slate-200 pt-3 text-xs"><span className="font-semibold text-cyan-800">{changeLabel}</span><span className="text-slate-500">작성일 {formatDateTime(revision.created_at)}</span></div></button>
}

function HistoryAmount({ label, previous, current, delta, className }: { label: string; previous: number; current: number; delta: number; className: string }) {
  return <div className="rounded bg-slate-50 p-3"><p className="text-xs font-semibold text-slate-500">{label}</p><p className="mt-1 text-xs text-slate-500">이전 {formatWon(previous)}</p><p className="mt-1 font-bold text-slate-950">현재 {formatWon(current)}</p><p className={'mt-1 text-xs font-bold ' + className}>증감 {delta > 0 ? '+' : ''}{formatWon(delta)}</p></div>
}
