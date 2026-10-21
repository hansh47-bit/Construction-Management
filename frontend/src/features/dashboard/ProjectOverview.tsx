import { AlertTriangle, ArrowLeft, ArrowRight, Banknote, BriefcaseBusiness, CheckCircle2, ClipboardList, FileClock, MapPin } from 'lucide-react'
import { SubMetric } from '../../components/ui/Metrics'
import { formatWon } from '../../lib/format'
import type { ExecutionBudgetDetailResponse, IntegratedSummary, Project } from '../../types/domain'

export function ProjectOverview({ project, summary, executionBudget, onBack, onStartConstruction, onSynchronizePreparation, onMoveToTab }: {
  project?: Project
  summary: IntegratedSummary
  executionBudget: ExecutionBudgetDetailResponse | null
  onBack: () => void
  onStartConstruction: () => void
  onSynchronizePreparation: () => void
  onMoveToTab: (tab: string) => void
}) {
  const flowCards: [string, number, string][] = [
    ['현재 계약금액', summary.flow_summary.current_contract_amount, '계약/발주 이력'],
    ['현재 실행예산', summary.flow_summary.current_execution_budget, '공종별 흐름 추적'],
    ['현재 발주금액', summary.flow_summary.current_purchase_amount, '계약/발주 이력'],
    ['누적 기성금액', summary.flow_summary.accumulated_completed_amount, '기성 및 지급 현황'],
    ['실제 지급금액', summary.flow_summary.actual_paid_amount, '기성 및 지급 현황'],
  ]
  return <>
    <section className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between"><div><button type="button" onClick={onBack} className="mb-3 inline-flex items-center gap-2 rounded-md border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"><ArrowLeft className="h-4 w-4" />목록</button><div className="flex flex-wrap items-center gap-2"><h2 className="text-xl font-semibold text-slate-950">{project?.project_name ?? '프로젝트'}</h2>{project?.status && <span className="rounded-full bg-cyan-100 px-2.5 py-1 text-xs font-semibold text-cyan-800">{project.status}</span>}{project?.status === '공사준비' && <button type="button" onClick={onStartConstruction} className="rounded-md bg-cyan-700 px-3 py-1.5 text-xs font-semibold text-white hover:bg-cyan-800">공사 시작</button>}{project?.status === '계약' && executionBudget?.approval_status === 'APPROVED' && <button type="button" onClick={onSynchronizePreparation} className="rounded-md border border-cyan-700 px-3 py-1.5 text-xs font-semibold text-cyan-800 hover:bg-cyan-50">실행예산 승인 반영</button>}</div><p className="mt-2 text-sm text-slate-600">{project?.project_code} · {project?.client_name ?? '발주처 미입력'}</p><p className="mt-4 flex items-center gap-2 text-sm text-slate-600"><MapPin className="h-4 w-4 text-slate-400" />{project?.site_address ?? '현장주소 미입력'}</p></div><div className="rounded-md border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600 lg:text-right"><p className="font-medium text-slate-800">목표 이익률 {project?.target_profit_rate ?? 0}%</p><p className="mt-1">담당자 {project?.manager_id ?? '미지정'}</p><p className="mt-1">팀장 {project?.team_leader_id ?? '미지정'}</p></div></div>
    </section>
    <section className="rounded-md border border-slate-200 bg-white p-4 shadow-sm"><div className="grid gap-3 xl:grid-cols-5">{flowCards.map(([label, value, targetTab], index) => <button key={label} type="button" onClick={() => onMoveToTab(targetTab)} className="group rounded-md border border-slate-200 bg-white p-4 text-left shadow-sm transition hover:border-cyan-300 hover:bg-cyan-50"><div className="flex items-center justify-between gap-2"><p className="text-sm font-medium text-slate-500">{label}</p>{index < flowCards.length - 1 && <ArrowRight className="h-4 w-4 text-slate-300 group-hover:text-cyan-700" />}</div><p className="mt-3 text-xl font-semibold text-slate-950">{formatWon(value)}</p></button>)}</div></section>
    <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4"><SubMetric label="목표 원가한도" value={formatWon(summary.profit_and_balance.target_cost_limit)} icon={<CheckCircle2 />} /><SubMetric label="예상 최종원가" value={formatWon(summary.profit_and_balance.expected_final_cost)} icon={<ClipboardList />} /><SubMetric label="예상 이익 / 이익률" value={`${formatWon(summary.profit_and_balance.expected_profit)} / ${summary.profit_and_balance.expected_profit_rate}%`} icon={<BriefcaseBusiness />} accent={summary.profit_and_balance.expected_profit >= 0 ? 'text-cyan-700' : 'text-rose-700'} /><SubMetric label="미발주 예정원가" value={formatWon(summary.profit_and_balance.unpurchased_budget_amount)} icon={<FileClock />} /><SubMetric label="미기성금액" value={formatWon(summary.profit_and_balance.unbilled_amount)} icon={<Banknote />} /><SubMetric label="승인 후 미지급" value={formatWon(summary.profit_and_balance.approved_unpaid_amount)} icon={<AlertTriangle />} /><SubMetric label="총 미지급 잔액" value={formatWon(summary.profit_and_balance.total_unpaid_balance)} icon={<Banknote />} /></section>
  </>
}
