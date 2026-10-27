import { useEffect, useMemo, useState } from 'react'
import { CheckCircle2, PlusCircle, Save } from 'lucide-react'
import { formatDateTime, formatWorkflowStatus, formatWon, toNumber } from '../../lib/format'
import type { ProjectRevision, ProjectRevisionContractItem, ProjectRevisionHistory } from '../../types/domain'
import { revisionApi } from './revision-api'

const emptyContractItem: ProjectRevisionContractItem = { work_category: '', quoted_amount: 0, contract_amount: 0, vat_amount: 0, note: '' }

export function RevisionManagementPanel({ projectId, projectCode, projectName }: { projectId: string; projectCode?: string; projectName?: string }) {
  const [revisions, setRevisions] = useState<ProjectRevision[]>([])
  const [active, setActive] = useState<ProjectRevision | null>(null)
  const [history, setHistory] = useState<ProjectRevisionHistory | null>(null)
  const [title, setTitle] = useState('')
  const [reason, setReason] = useState('')
  const [contractItems, setContractItems] = useState<ProjectRevisionContractItem[]>([])
  const [targetProfitRate, setTargetProfitRate] = useState('0')
  const [message, setMessage] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  function selectRevision(revision: ProjectRevision) {
    setActive(revision)
    setTitle(revision.title)
    setReason(revision.reason)
    setContractItems(revision.contract_items)
    setTargetProfitRate(String(revision.target_profit_rate))
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

  useEffect(() => {
    if (!active?.revision_id) { setHistory(null); return }
    void revisionApi.history(projectId, active.revision_id)
      .then(async (response) => setHistory(response.ok ? await response.json() : null))
      .catch(() => setHistory(null))
  }, [active?.revision_id, projectId])

  const contractTotal = useMemo(() => contractItems.reduce((sum, item) => sum + item.contract_amount, 0), [contractItems])
  const nextRevision = Math.max(1, ...revisions.map((item) => item.revision_number)) + 1
  const isContractEditable = active?.status === 'DRAFT' || active?.status === 'CONTRACT_WRITTEN'

  async function createRevision() {
    if (!title.trim() || !reason.trim()) { setMessage('차수 제목과 변경 사유를 입력해 주세요.'); return }
    setIsSaving(true)
    try {
      const response = await revisionApi.create(projectId, { title: title.trim(), reason: reason.trim() })
      if (!response.ok) throw new Error(await response.text())
      const created: ProjectRevision = await response.json()
      setRevisions((items) => [created, ...items])
      selectRevision(created)
      setMessage(`${created.revision_number}차 계약 변경 차수를 생성했습니다. 계약 변경 내용을 작성해 주세요.`)
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
      selectRevision(saved)
      setMessage('계약 변경 내용을 저장했습니다. 계약 변경 결재를 요청할 수 있습니다.')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '계약 변경 저장에 실패했습니다.')
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
      selectRevision(updated)
      setMessage(`${active.revision_number}차 계약 변경을 통합결재함으로 요청했습니다.`)
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '결재 요청에 실패했습니다.')
    } finally {
      setIsSaving(false)
    }
  }

  return <div className="space-y-6">
    <section className="rounded-md border border-slate-200 bg-white p-4">
      <h3 className="text-lg font-semibold text-slate-950">계약 변경 차수 관리</h3>
      <p className="mt-1 text-sm text-slate-500">계약금액 또는 계약 공종 변경만 이곳에서 차수로 관리합니다. 세부실행예산은 세부실행예산 메뉴에서 별도로 작성·결재합니다.</p>
      <div className="mt-4 rounded-md border border-cyan-100 bg-cyan-50 px-4 py-3 text-sm"><span className="font-semibold text-cyan-900">대상 프로젝트</span><span className="ml-3 font-bold text-slate-950">{projectCode ?? projectId}</span><span className="ml-2 text-slate-700">{projectName ?? ''}</span></div>
      {!active && <div className="mt-4 grid gap-3 md:grid-cols-[1fr_1fr_auto]">
        <label className="text-sm font-semibold text-slate-700">{nextRevision}차 제목<input className="input mt-2" value={title} onChange={(event) => setTitle(event.target.value)} placeholder={`${nextRevision}차 계약 변경`} /></label>
        <label className="text-sm font-semibold text-slate-700">변경 사유<input className="input mt-2" value={reason} onChange={(event) => setReason(event.target.value)} placeholder="변경 사유" /></label>
        <button type="button" disabled={isSaving} onClick={() => void createRevision()} className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md bg-slate-900 px-4 text-sm font-semibold text-white disabled:bg-slate-300"><PlusCircle className="h-4 w-4" />새 차수 작성</button>
      </div>}
    </section>

    {revisions.length > 0 && <section className="rounded-md border border-slate-200 bg-white p-4"><div><h3 className="font-semibold text-slate-950">차수별 계약 변경 이력</h3><p className="mt-1 text-sm text-slate-500">계약금액과 계약 공종이 어떻게 바뀌었는지 차수별로 확인합니다.</p></div><div className="mt-4 grid gap-3 lg:grid-cols-2">{revisions.map((revision) => <RevisionHistoryCard key={revision.revision_id} revision={revision} selected={active?.revision_id === revision.revision_id} onClick={() => selectRevision(revision)} />)}</div></section>}

    {message && <p className="rounded-md border border-cyan-200 bg-cyan-50 px-4 py-3 text-sm text-cyan-900">{message}</p>}

    {active && <>
      <section className="rounded-md border border-slate-200 bg-white p-4">
        <div className="flex flex-wrap items-end justify-between gap-3 border-b border-slate-200 pb-3"><div><h3 className="font-semibold text-slate-950">{active.revision_number}차 고객견적·계약 관리</h3><p className="mt-1 text-sm text-slate-500">변경된 계약 전체금액을 작성합니다. 세부실행예산은 이 화면에서 작성하지 않습니다.</p></div>{isContractEditable && <button type="button" disabled={isSaving} onClick={() => void saveContract()} className="inline-flex items-center gap-2 rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white disabled:bg-slate-300"><Save className="h-4 w-4" />계약 변경 저장</button>}</div>
        <label className="mt-4 block max-w-xs text-sm font-semibold text-slate-700">목표이익률 (%)<input disabled={!isContractEditable} className="input mt-2 disabled:cursor-not-allowed disabled:bg-slate-100" value={targetProfitRate} onChange={(event) => setTargetProfitRate(event.target.value)} inputMode="decimal" /></label>
        <div className="mt-4 overflow-x-auto"><table className="min-w-[900px] text-sm"><thead className="bg-slate-50 text-xs text-slate-500"><tr><th className="px-3 py-2 text-left">공종</th><th className="px-3 py-2 text-right">견적금액</th><th className="px-3 py-2 text-right">계약금액</th><th className="px-3 py-2 text-left">비고</th></tr></thead><tbody>{contractItems.map((item, index) => <tr key={index} className="border-t border-slate-100"><td className="px-3 py-2"><input disabled={!isContractEditable} className="input disabled:cursor-not-allowed disabled:bg-slate-100" value={item.work_category} onChange={(event) => setContractItems((items) => items.map((value, i) => i === index ? { ...value, work_category: event.target.value } : value))} /></td><td className="px-3 py-2"><input disabled={!isContractEditable} className="input text-right disabled:cursor-not-allowed disabled:bg-slate-100" value={item.quoted_amount} inputMode="numeric" onChange={(event) => setContractItems((items) => items.map((value, i) => i === index ? { ...value, quoted_amount: toNumber(event.target.value) } : value))} /></td><td className="px-3 py-2"><input disabled={!isContractEditable} className="input text-right disabled:cursor-not-allowed disabled:bg-slate-100" value={item.contract_amount} inputMode="numeric" onChange={(event) => setContractItems((items) => items.map((value, i) => i === index ? { ...value, contract_amount: toNumber(event.target.value), vat_amount: toNumber(event.target.value) * 0.1 } : value))} /></td><td className="px-3 py-2"><input disabled={!isContractEditable} className="input disabled:cursor-not-allowed disabled:bg-slate-100" value={item.note ?? ''} onChange={(event) => setContractItems((items) => items.map((value, i) => i === index ? { ...value, note: event.target.value } : value))} /></td></tr>)}</tbody></table></div>
        {isContractEditable && <button type="button" onClick={() => setContractItems((items) => [...items, emptyContractItem])} className="mt-3 text-sm font-semibold text-cyan-700">+ 계약 공종 추가</button>}<p className="mt-2 text-right text-sm font-semibold">계약 공급가 합계: {formatWon(contractTotal)}</p>
      </section>

      <section className="rounded-md border border-slate-200 bg-white p-4"><div className="flex flex-wrap items-center justify-between gap-3"><div><h3 className="font-semibold text-slate-950">계약 변경 결재</h3><p className="mt-1 text-sm text-slate-500">계약 변경 저장 후 결재를 요청합니다. 세부실행예산 변경은 세부실행예산 메뉴에서 별도 처리합니다.</p></div>{active.status === 'CONTRACT_WRITTEN' && <button type="button" disabled={isSaving} onClick={() => void requestApproval()} className="inline-flex items-center gap-2 rounded-md bg-cyan-700 px-4 py-2 text-sm font-semibold text-white disabled:bg-slate-300"><CheckCircle2 className="h-4 w-4" />계약 변경 결재 요청</button>}{active.status === 'REQUESTED' && <span className="rounded-md bg-amber-50 px-3 py-2 text-sm font-semibold text-amber-800">결재 대기</span>}{active.status === 'APPROVED' && <span className="rounded-md bg-emerald-50 px-3 py-2 text-sm font-semibold text-emerald-800">승인 완료 · {active.revision_number}차 계약 반영</span>}</div><div className="mt-4"><Comparison label="계약 공급가" base={active.base_contract_total} current={active.contract_total} /></div></section>
      <RevisionDetail revision={active} history={history} />
    </>}
  </div>
}

function RevisionDetail({ revision, history }: { revision: ProjectRevision; history: ProjectRevisionHistory | null }) {
  const budgetTotal = revision.budget_details.reduce((sum, detail) => sum + detail.quantity * detail.unit_price, 0)
  return <section className="rounded-md border border-slate-200 bg-white p-4">
    <div className="flex flex-wrap items-center justify-between gap-2">
      <div><h3 className="font-semibold text-slate-950">{revision.revision_number}차 자료 상세</h3><p className="mt-1 text-sm text-slate-500">이전 차수는 읽기 전용입니다. 당시 승인된 계약·실행예산·발주·기성·지급 자료를 그대로 확인합니다.</p></div>
      <span className="rounded-md bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-700">읽기 전용</span>
    </div>
    <div className="mt-5 overflow-x-auto"><h4 className="mb-2 text-sm font-semibold text-slate-900">세부실행예산</h4><table className="min-w-[860px] text-left text-sm"><thead className="bg-slate-50 text-xs text-slate-500"><tr><th className="px-3 py-2">실행항목 ID</th><th className="px-3 py-2">공종</th><th className="px-3 py-2">원가구분</th><th className="px-3 py-2">규격단위</th><th className="px-3 py-2 text-right">수량</th><th className="px-3 py-2 text-right">단가</th><th className="px-3 py-2 text-right">금액</th><th className="px-3 py-2">비고</th></tr></thead><tbody>{revision.budget_details.map((detail, index) => <tr key={`${detail.budget_item_id}-${index}`} className="border-t border-slate-100"><td className="px-3 py-2">{detail.budget_item_id}</td><td className="px-3 py-2">{detail.category_name}</td><td className="px-3 py-2">{detail.cost_type}</td><td className="px-3 py-2">{detail.unit || '-'}</td><td className="px-3 py-2 text-right">{detail.quantity}</td><td className="px-3 py-2 text-right">{formatWon(detail.unit_price)}</td><td className="px-3 py-2 text-right font-semibold">{formatWon(detail.quantity * detail.unit_price)}</td><td className="px-3 py-2">{detail.note || '-'}</td></tr>)}{revision.budget_details.length === 0 && <tr><td colSpan={8} className="px-3 py-6 text-center text-slate-500">이 차수에서 별도 작성된 세부실행예산이 없습니다.</td></tr>}</tbody><tfoot><tr className="border-t bg-slate-50 font-semibold"><td colSpan={6} className="px-3 py-2 text-right">합계</td><td className="px-3 py-2 text-right">{formatWon(budgetTotal)}</td><td /></tr></tfoot></table></div>
    <div className="mt-6 grid gap-5 xl:grid-cols-2"><HistoryComparisons history={history} /><HistoryPurchaseOrders history={history} /></div>
  </section>
}

function HistoryComparisons({ history }: { history: ProjectRevisionHistory | null }) {
  return <div><h4 className="mb-2 text-sm font-semibold text-slate-900">업체 비교견적</h4><div className="overflow-x-auto rounded border border-slate-200"><table className="min-w-[560px] text-left text-sm"><thead className="bg-slate-50 text-xs text-slate-500"><tr><th className="px-3 py-2">실행항목</th><th className="px-3 py-2">업체</th><th className="px-3 py-2 text-right">견적금액</th><th className="px-3 py-2">선정</th></tr></thead><tbody>{history?.comparisons.map((item, index) => <tr key={`${item.execution_item_id}-${index}`} className="border-t border-slate-100"><td className="px-3 py-2">{item.execution_item_id}</td><td className="px-3 py-2">{item.vendor_name}</td><td className="px-3 py-2 text-right">{formatWon(item.quoted_amount)}</td><td className="px-3 py-2">{item.selected ? `선정${item.selection_reason ? ` · ${item.selection_reason}` : ''}` : '-'}</td></tr>)}{history && history.comparisons.length === 0 && <tr><td colSpan={4} className="px-3 py-6 text-center text-slate-500">등록된 비교견적이 없습니다.</td></tr>}{!history && <tr><td colSpan={4} className="px-3 py-6 text-center text-slate-500">이력 자료를 불러오는 중입니다.</td></tr>}</tbody></table></div></div>
}

function HistoryPurchaseOrders({ history }: { history: ProjectRevisionHistory | null }) {
  return <div><h4 className="mb-2 text-sm font-semibold text-slate-900">발주 · 기성 · 지급</h4><div className="space-y-2">{history?.purchase_orders.map((po) => <div key={po.po_id} className="rounded border border-slate-200 p-3 text-sm"><div className="flex flex-wrap justify-between gap-2"><strong>{po.po_id} · {po.vendor_name}</strong><span>{formatWon(po.po_amount)}</span></div><p className="mt-1 text-xs text-slate-500">{po.execution_item_id} · 승인 실행예산 {formatWon(po.approved_budget_amount)}</p>{po.claims.map((claim) => <div key={claim.claim_id} className="mt-2 border-t border-slate-100 pt-2 text-xs"><p>{claim.degree}차 기성 {formatWon(claim.claim_amount)} · {formatWorkflowStatus(claim.approval_status)}</p>{claim.payments.map((payment) => <p key={payment.payment_id} className="mt-1 text-slate-600">지급 {formatWon(payment.paid_amount)} · {payment.paid_date}</p>)}</div>)}</div>)}{history && history.purchase_orders.length === 0 && <p className="rounded border border-dashed border-slate-300 px-3 py-6 text-center text-sm text-slate-500">등록된 발주·기성·지급 이력이 없습니다.</p>}{!history && <p className="rounded border border-dashed border-slate-300 px-3 py-6 text-center text-sm text-slate-500">이력 자료를 불러오는 중입니다.</p>}</div></div>
}

function Comparison({ label, base, current }: { label: string; base: number; current: number }) {
  return <div className="rounded-md bg-slate-50 p-4"><p className="text-sm font-semibold text-slate-700">{label}</p><div className="mt-3 grid grid-cols-2 gap-3 text-sm"><p>직전 차수<strong className="block text-slate-950">{formatWon(base)}</strong></p><p>현재 작성 차수<strong className="block text-cyan-700">{formatWon(current)}</strong></p></div></div>
}

function RevisionHistoryCard({ revision, selected, onClick }: { revision: ProjectRevision; selected: boolean; onClick: () => void }) {
  const contractDelta = revision.contract_total - revision.base_contract_total
  const contractChanged = contractDelta !== 0 || JSON.stringify(revision.base_contract_items) !== JSON.stringify(revision.contract_items)
  const deltaClass = contractDelta > 0 ? 'text-rose-700' : contractDelta < 0 ? 'text-blue-700' : 'text-slate-600'
  return <button type="button" onClick={onClick} className={'rounded-md border p-4 text-left transition hover:border-cyan-400 ' + (selected ? 'border-cyan-700 bg-cyan-50 ring-1 ring-cyan-700' : 'border-slate-200 bg-white')}><div className="flex flex-wrap items-start justify-between gap-2"><div><p className="font-bold text-slate-950">{revision.revision_number}차 · {revision.title}</p><p className="mt-1 text-sm text-slate-600">변경 사유: {revision.reason}</p></div><span className="rounded-full bg-slate-100 px-2 py-1 text-xs font-bold text-slate-700">{formatWorkflowStatus(revision.status)}</span></div><div className="mt-4 rounded bg-slate-50 p-3"><p className="text-xs font-semibold text-slate-500">계약 공급가</p><p className="mt-1 text-xs text-slate-500">이전 {formatWon(revision.base_contract_total)}</p><p className="mt-1 font-bold text-slate-950">현재 {formatWon(revision.contract_total)}</p><p className={'mt-1 text-xs font-bold ' + deltaClass}>증감 {contractDelta > 0 ? '+' : ''}{formatWon(contractDelta)}</p></div><div className="mt-3 flex flex-wrap items-center justify-between gap-2 border-t border-slate-200 pt-3 text-xs"><span className="font-semibold text-cyan-800">{contractChanged ? '계약 변경' : '작성 중'}</span><span className="text-slate-500">작성일 {formatDateTime(revision.created_at)}</span></div></button>
}
