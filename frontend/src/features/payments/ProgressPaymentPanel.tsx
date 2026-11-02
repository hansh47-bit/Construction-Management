import { useState } from 'react'
import { useAuth } from '../auth/auth'
import { paymentApi } from './api'
import { claimEntryDefaults } from './progress-claim'
import { confirmAction } from '../../lib/feedback'
import { useDraftGuard } from '../../lib/use-live-refresh'
import type { ClaimStatus } from '../../types/domain'
import { PermissionGate } from '../auth/Permissions'
import { MetricPanel } from '../../components/ui/Metrics'
import { MoneyInput } from '../../components/ui/MoneyInput'
import { formatWorkflowStatus, formatWon, toNumber } from '../../lib/format'
import type { ProgressPaymentResponse } from '../../types/domain'

export function ProgressPaymentPanel({
  projectId,
  onPaymentRecorded,
  progressPayment,
  selectedPoId,
  setSelectedPoId,
  claimForm,
  setClaimAmount,
  selectedPaymentPoIds,
  setSelectedPaymentPoIds,
  createProgressClaim,
  deleteProgressClaim,
  requestPaymentApprovals,
}: {
  projectId: string
  onPaymentRecorded: () => void
  progressPayment: ProgressPaymentResponse
  selectedPoId: string
  setSelectedPoId: (value: string) => void
  claimForm: { degree: string; claim_amount: string }
  setClaimAmount: (value: string) => void
  selectedPaymentPoIds: string[]
  setSelectedPaymentPoIds: (poIds: string[]) => void
  createProgressClaim: () => void
  deleteProgressClaim: (claimId: string) => void
  requestPaymentApprovals: () => void
}) {
  const { user, can } = useAuth()
  const canRequestPayment = user?.position === 'FINANCE' && can('PAYMENT_REQUEST')
  const [paying, setPaying] = useState<ClaimStatus | null>(null)
  const selectedPo = progressPayment.purchaseOrders.find((po) => po.po_id === selectedPoId) ?? progressPayment.purchaseOrders[0]
  const selectedPoSequence = Math.max(1, progressPayment.purchaseOrders.findIndex((po) => po.po_id === selectedPo?.po_id) + 1)
  const availableClaimAmount = toNumber(claimEntryDefaults(selectedPo).claim_amount)
  const enteredClaimAmount = toNumber(claimForm.claim_amount)
  const paymentRows = progressPayment.purchaseOrders.flatMap((po) => po.claims.map((claim) => ({ po, claim })))
  const totalApprovedClaims = progressPayment.purchaseOrders.reduce((sum, po) => sum + po.approved_claim_total, 0)
  const totalPaid = progressPayment.purchaseOrders.reduce((sum, po) => sum + po.actual_paid_total, 0)
  const requestedProgressClaimTotal = paymentRows
    .filter(({ claim }) => claim.approval_status === 'REQUESTED')
    .reduce((sum, { claim }) => sum + claim.claim_amount, 0)
  const beforeProgressRequestTotal = progressPayment.purchaseOrders.reduce((sum, po) => {
    const committedClaimTotal = po.claims
      .filter((claim) => claim.approval_status === 'REQUESTED' || claim.approval_status === 'APPROVED')
      .reduce((claimSum, claim) => claimSum + claim.claim_amount, 0)
    return sum + Math.max(0, po.current_po_amount - committedClaimTotal)
  }, 0)
  const selectablePoIds = progressPayment.purchaseOrders
    .filter((po) => po.approved_unpaid_amount > 0 && po.payment_request_status !== 'REQUESTED')
    .map((po) => po.po_id)

  if (!selectedPo && progressPayment.revision_number) {
    return (
      <div className="rounded-md border border-dashed border-cyan-300 bg-cyan-50 p-8 text-center text-sm text-cyan-900">
        <p className="font-semibold">{progressPayment.revision_number}차 기성·지급 현황에는 아직 신규 발주가 없습니다.</p>
        <p className="mt-1">{progressPayment.revision_number}차 실행예산 기준으로 업체 비교견적과 발주 승인을 새로 진행한 뒤 기성·지급을 작성하세요.</p>
      </div>
    )
  }

  if (!selectedPo) {
    return <div className="rounded-md border border-dashed border-slate-300 bg-slate-50 p-8 text-center text-sm text-slate-500">등록된 발주가 없습니다.</div>
  }

  return (
    <div>
      {progressPayment.revision_number && (
        <div className="mb-4 rounded-md border border-cyan-200 bg-cyan-50 px-4 py-3 text-sm text-cyan-900">
          <strong>{progressPayment.revision_number}차 기성·지급 현황</strong>입니다. 이전 차수의 발주·기성·지급 기록은 현재 차수 집계에 포함하지 않습니다.
        </div>
      )}
      <div className="flex flex-col gap-4 border-b border-slate-200 pb-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 className="text-lg font-semibold text-slate-950">기성 및 지급 현황</h3>
          <p className="mt-1 text-sm text-slate-500">재무가 지급할 업체를 선택해 대표에게 지급결재를 요청합니다. 다른 직책은 현황을 조회할 수 있습니다.</p>
          <p className="mt-1 text-sm font-medium text-cyan-800">기성 승인·반려는 통합결재함에서 처리합니다.</p>
        </div>
        {canRequestPayment && <button type="button" onClick={requestPaymentApprovals} disabled={selectedPaymentPoIds.length === 0} className="rounded-md bg-cyan-700 px-4 py-2 text-sm font-semibold text-white hover:bg-cyan-800 disabled:cursor-not-allowed disabled:bg-slate-300">대표에게 지급결재 요청 ({selectedPaymentPoIds.length})</button>}
      </div>

      <div className="grid gap-3 border-b border-slate-200 py-5 md:grid-cols-3">
        <MetricPanel label="발주 업체 수" value={`${progressPayment.purchaseOrders.length}곳`} helper={`지급결재 대기 ${progressPayment.pending_payment_approval_count}건`} />
        <MetricPanel label="기성 승인 요청 합계" value={formatWon(requestedProgressClaimTotal)} helper="통합결재함 승인 대기 기성" accent={requestedProgressClaimTotal > 0 ? 'text-amber-700' : 'text-slate-950'} />
        <MetricPanel label="기성 승인 요청 전 합계" value={formatWon(beforeProgressRequestTotal)} helper="아직 기성 승인 요청하지 않은 발주 잔액" />
        <MetricPanel label="승인 기성누계" value={formatWon(totalApprovedClaims)} helper="전체 업체 합계" />
        <MetricPanel label="실제 지급누계" value={formatWon(totalPaid)} helper="재무의 실제 지급 등록 합계" />
        <MetricPanel label="전체 승인 후 미지급" value={formatWon(totalApprovedClaims - totalPaid)} helper="지급요청 가능한 금액" accent={totalApprovedClaims > totalPaid ? 'text-rose-700' : 'text-slate-950'} />
        <MetricPanel label="준공정산 이동 조건" value={progressPayment.all_payments_approved ? '충족' : '미충족'} helper="모든 확정 발주 지급 완료 기준" accent={progressPayment.all_payments_approved ? 'text-cyan-700' : 'text-slate-700'} />
      </div>

      <div className="border-b border-slate-200 py-5">
        <h3 className="mb-3 text-sm font-semibold text-slate-900">업체별 지급 대시보드</h3>
        <div className="overflow-x-auto">
          <table className="min-w-[1380px] text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-3 py-3">순번</th>
                <th className="px-3 py-3"><input type="checkbox" aria-label="지급요청 대상 전체 선택" disabled={!canRequestPayment || selectablePoIds.length === 0} checked={canRequestPayment && selectablePoIds.length > 0 && selectablePoIds.every((id) => selectedPaymentPoIds.includes(id))} onChange={(event) => { if (canRequestPayment) setSelectedPaymentPoIds(event.target.checked ? selectablePoIds : []) }} /></th>
                <th className="px-3 py-3">업체</th>
                <th className="px-3 py-3">공종 / 실행항목</th>
                <th className="px-3 py-3">발주번호</th>
                <th className="px-3 py-3">현재 발주금액</th>
                <th className="px-3 py-3">승인 기성</th>
                <th className="px-3 py-3">지급누계</th>
                <th className="px-3 py-3">승인 후 미지급</th>
                <th className="px-3 py-3">총 지급잔액</th>
                <th className="px-3 py-3">지급결재 상태</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {progressPayment.purchaseOrders.map((po, index) => {
                const selectable = selectablePoIds.includes(po.po_id)
                return (
                  <tr key={po.po_id}>
                    <td className="px-3 py-3 text-slate-500">{index + 1}</td>
                    <td className="px-3 py-3"><input type="checkbox" aria-label={`${po.vendor_name} 지급요청 선택`} disabled={!canRequestPayment || !selectable} checked={canRequestPayment && selectedPaymentPoIds.includes(po.po_id)} onChange={(event) => { if (canRequestPayment) setSelectedPaymentPoIds(event.target.checked ? [...selectedPaymentPoIds, po.po_id] : selectedPaymentPoIds.filter((id) => id !== po.po_id)) }} /></td>
                    <td className="px-3 py-3 font-semibold text-slate-900">{po.vendor_name}</td>
                    <td className="px-3 py-3"><p className="font-medium text-slate-800">{po.work_category || '미분류'}</p><p className="mt-0.5 text-xs text-slate-500">{po.execution_item_id || '-'} · {po.execution_item_name || '실행항목 미입력'}</p></td>
                    <td className="px-3 py-3 text-cyan-800">{po.po_id}</td>
                    <td className="px-3 py-3">{formatWon(po.current_po_amount)}</td>
                    <td className="px-3 py-3">{formatWon(po.approved_claim_total)}</td>
                    <td className="px-3 py-3">{formatWon(po.actual_paid_total)}</td>
                    <td className="px-3 py-3 font-semibold">{formatWon(po.approved_unpaid_amount)}</td>
                    <td className="px-3 py-3 font-semibold text-rose-700">{formatWon(po.total_unpaid_balance)}</td>
                    <td className="px-3 py-3">{formatWorkflowStatus(po.payment_request_status)}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>

      <section aria-label="기성금액 작성" className="border-b border-slate-200 py-5">
      <h3 className="mb-3 text-sm font-semibold text-slate-900">선택 업체 기성 작성 및 잔액</h3>
      <div className="mb-5 grid gap-3 md:grid-cols-3">
        <MetricPanel label="기성 작성 가능 잔액" value={formatWon(availableClaimAmount)} helper="현재 발주금액 − 승인·결재 대기 기성 합계" />
        <MetricPanel label="실제 지급누계" value={formatWon(selectedPo.actual_paid_total)} helper={`${selectedPo.vendor_name} · 선택한 발주 기준`} />
        <MetricPanel label="총 지급잔액" value={formatWon(selectedPo.total_unpaid_balance)} helper="현재 발주금액 − 실제 지급누계" accent={selectedPo.total_unpaid_balance > 0 ? 'text-rose-700' : 'text-slate-950'} />
      </div>
      <div className="grid gap-3 lg:grid-cols-[minmax(420px,2.4fr)_120px_minmax(240px,1fr)_auto]">
        <label className="block text-sm font-semibold text-slate-700">
          기성 작성 대상
          <div className="mt-2 rounded-md border border-cyan-200 bg-cyan-50 px-4 py-3">
            <p className="text-xs font-bold tracking-wide text-cyan-800">발주 순서 {String(selectedPoSequence).padStart(2, '0')} · {selectedPo.po_id}</p>
            <p className="mt-1 text-base font-extrabold text-slate-950">{selectedPo.vendor_name} · {selectedPo.work_category || '미분류'}</p>
            <p className="mt-1 text-sm text-slate-700">{selectedPo.execution_item_id || '-'} · {selectedPo.execution_item_name || '실행항목 미입력'}</p>
          </div>
          <select value={selectedPo.po_id} onChange={(event) => setSelectedPoId(event.target.value)} className="input mt-2 h-12 text-base font-semibold">
            {progressPayment.purchaseOrders.map((po, index) => <option key={po.po_id} value={po.po_id}>[{String(index + 1).padStart(2, '0')}] {po.vendor_name} · {po.work_category || '미분류'} / {po.execution_item_name || po.execution_item_id || '실행항목 미입력'} · {po.po_id}</option>)}
          </select>
        </label>
        <label className="block text-sm font-semibold text-slate-700">
          차수
          <input aria-label="기성 차수" type="number" min="1" value={claimForm.degree} readOnly className="input mt-2 bg-slate-50" />
          <p className="mt-1 text-xs font-medium text-slate-500">등록된 마지막 차수 다음 번호로 자동 설정됩니다.</p>
        </label>
        <label className="block text-sm font-semibold text-slate-700">
          기성금액
          <MoneyInput value={enteredClaimAmount} onChange={setClaimAmount} disabled={availableClaimAmount <= 0} />
          <p className="mt-1 text-xs font-medium text-slate-500">이미 승인·결재 요청한 기성을 뺀 잔액이 자동 입력되며, 그 범위에서 수정할 수 있습니다.</p>
        </label>
        <PermissionGate permission="PROGRESS_WRITE"><button type="button" onClick={createProgressClaim} disabled={!Number.isFinite(enteredClaimAmount) || enteredClaimAmount <= 0 || enteredClaimAmount > availableClaimAmount} className="self-end rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-300">기성금액 작성</button></PermissionGate>
      </div>
      {availableClaimAmount <= 0 && <p className="mt-3 text-sm font-semibold text-slate-600">기성 작성 가능 잔액이 없습니다. 기존 기성과 결재 상태를 확인해주세요.</p>}
      {enteredClaimAmount > availableClaimAmount && <p role="alert" className="mt-3 text-sm font-semibold text-rose-700">기성금액은 기성 작성 가능 잔액 {formatWon(availableClaimAmount)}을 초과할 수 없습니다.</p>}
      <p className="mt-3 text-xs text-slate-500">기성 등록·승인만으로 지급잔액이 줄어들지는 않습니다. 실제 지급 등록 후 반영됩니다.</p>
      </section>

      <div className="py-5">
        <h3 className="mb-3 text-sm font-semibold text-slate-900">전체 업체 기성 및 지급 내역</h3>
        <div className="overflow-x-auto">
          <table className="min-w-[1480px] text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-3 py-3">순번</th>
                <th className="px-3 py-3">업체</th>
                <th className="px-3 py-3">공종 / 실행항목</th>
                <th className="px-3 py-3">발주번호</th>
                <th className="px-3 py-3">차수</th>
                <th className="px-3 py-3">기성금액</th>
                <th className="px-3 py-3">결재상태</th>
                <th className="px-3 py-3">지급상태</th>
                <th className="px-3 py-3">지급누계</th>
                <th className="px-3 py-3">지급잔액</th>
                <th className="px-3 py-3">처리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {paymentRows.map(({ po, claim }, index) => (
                <tr key={claim.claim_id}>
                  <td className="px-3 py-3 text-slate-500">{index + 1}</td>
                  <td className="px-3 py-3 font-semibold text-slate-900">{po.vendor_name}</td>
                  <td className="px-3 py-3"><p className="font-medium text-slate-800">{po.work_category || '미분류'}</p><p className="mt-0.5 text-xs text-slate-500">{po.execution_item_id || '-'} · {po.execution_item_name || '실행항목 미입력'}</p></td>
                  <td className="px-3 py-3 text-cyan-800">{po.po_id}</td>
                  <td className="px-3 py-3 font-semibold text-slate-900">{claim.degree}차</td>
                  <td className="px-3 py-3">{formatWon(claim.claim_amount)}</td>
                  <td className="px-3 py-3">{formatWorkflowStatus(claim.approval_status)}</td>
                  <td className="px-3 py-3"><span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${claim.payment_state === 'PAID' ? 'bg-cyan-100 text-cyan-800' : claim.payment_state === 'WAITING_FOR_PAYMENT' ? 'bg-amber-100 text-amber-800' : 'bg-slate-100 text-slate-700'}`}>{formatWorkflowStatus(claim.payment_state)}</span></td>
                  <td className="px-3 py-3">{formatWon(claim.paid_total)}</td>
                  <td className="px-3 py-3">
                    <div className="flex items-center gap-2">
                      <span>{formatWon(claim.remaining_payable)}</span>
                      {(user?.position !== 'STAFF' || claim.created_user_id === user.id) && (claim.approval_status === 'REQUESTED' || claim.approval_status === 'REJECTED') && (
                        <PermissionGate permission="PROGRESS_WRITE"><button type="button" onClick={() => deleteProgressClaim(claim.claim_id)} className="rounded border border-rose-300 px-2 py-1 text-xs font-semibold text-rose-700 hover:bg-rose-50">삭제</button></PermissionGate>
                      )}
                    </div>
                  </td>
                  <td className="px-3 py-3 text-xs font-semibold text-slate-600">{claim.approval_status === 'APPROVED' && claim.remaining_payable > 0 && <PermissionGate permission="PAYMENT_EXECUTE"><button type="button" onClick={() => setPaying(claim)} className="mb-2 block rounded bg-blue-700 px-3 py-2 text-white">실제 지급 등록</button></PermissionGate>}{claim.approval_status === 'REQUESTED' ? '통합결재함 승인 대기' : claim.approval_status === 'REJECTED' ? '통합결재함 반려' : '-'}</td>
                </tr>
              ))}
              {paymentRows.length === 0 && <tr><td colSpan={11} className="px-3 py-8 text-center text-slate-500">등록된 기성 차수가 없습니다.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
      {paying && <PaymentForm projectId={projectId} claim={paying} close={() => setPaying(null)} saved={() => { setPaying(null); onPaymentRecorded() }} />}
    </div>
  )
}

function PaymentForm({ projectId, claim, close, saved }: { projectId: string; claim: ClaimStatus; close: () => void; saved: () => void }) {
 const [amount, setAmount] = useState(String(claim.remaining_payable)); const [date, setDate] = useState(new Date().toISOString().slice(0, 10)); const [account, setAccount] = useState(''); const [proof, setProof] = useState(''); const [busy, setBusy] = useState(false); const [error, setError] = useState('')
 useDraftGuard(!!account || !!proof || amount !== String(claim.remaining_payable))
 async function save() { if (busy) return; if (!await confirmAction({ title: '실제 지급 등록 확인', message: `실제 송금한 ${formatWon(Number(amount))}을 ${date} 지급 내역으로 등록하시겠습니까? 결재 승인과는 별도인 실제 지급 기록입니다.`, confirmLabel: '지급 등록' })) return; setBusy(true); try { const response = await paymentApi.createPayment(projectId, claim.claim_id, { paid_amount: Number(amount), paid_date: date, account_info: account, receipt_link: proof }); if (!response.ok) { const body = await response.json() as { message?: string }; throw new Error(body.message ?? '지급 등록 실패') }; saved() } catch (e) { setError((e as Error).message) } finally { setBusy(false) } }
 return <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/40 p-4"><form className="w-full max-w-lg space-y-4 rounded-xl bg-white p-6 shadow-xl" onSubmit={e => { e.preventDefault(); void save() }}><h2 className="text-xl font-bold">실제 지급 등록</h2><p className="text-sm text-slate-600">실제 송금한 금액과 날짜를 등록하세요. 승인 기성 잔액: {formatWon(claim.remaining_payable)}</p>{error && <p role="alert" className="text-sm text-rose-700">{error}</p>}<label className="block text-sm font-semibold">지급금액<input required type="number" min="1" max={claim.remaining_payable} value={amount} onChange={e => setAmount(e.target.value)} className="input mt-1" /></label><label className="block text-sm font-semibold">실제 지급일<input required type="date" value={date} onChange={e => setDate(e.target.value)} className="input mt-1" /></label><label className="block text-sm font-semibold">지급 계좌·내용<input value={account} onChange={e => setAccount(e.target.value)} className="input mt-1" /></label><label className="block text-sm font-semibold">지급 증빙 링크<input value={proof} onChange={e => setProof(e.target.value)} className="input mt-1" /></label><div className="flex justify-end gap-2"><button type="button" onClick={close} className="rounded border px-4 py-2">취소</button><button disabled={busy} className="rounded bg-blue-700 px-4 py-2 text-white disabled:opacity-40">{busy ? '등록 중…' : '지급 등록'}</button></div></form></div>
}
