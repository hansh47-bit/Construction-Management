import { UserMenu } from './features/auth/AuthProvider'
import { useAuth } from './features/auth/auth'
import { DraftPermissions, WriteScope } from './features/auth/Permissions'
import { apiJson } from './lib/api-client'
import { confirmAction, hasUnsavedDraft, notify, userAction } from './lib/feedback'
import { useDraftGuard, useLiveRefresh } from './lib/use-live-refresh'
import { useWorkspaceState, useWorkspaceUpdate } from './lib/use-workspace-state'
import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import type { ApprovalItem, CustomerContract, CustomerContractItem, ExecutionBudgetDetail, ExecutionBudgetDetailResponse, ExecutionBudgetSummaryItem, IntegratedSummary, ProgressPaymentResponse, ProjectSettlement, PurchaseOrderDraft, PurchaseOrderHistoryItem, PurchaseOrderHistoryResponse, VendorComparisonItem, VendorComparisonResponse } from './types/domain'
import { approvalApi } from './features/approvals/api'
import { canDecideApproval } from './features/approvals/workflow'
import { approvalProjectOptions, matchesApproval } from './features/approvals/project-filter'
import type { ApprovalProject } from './features/approvals/project-filter'
import { contractApi } from './features/contracts/api'
import { executionBudgetApi } from './features/execution-budget/api'
import { paymentApi } from './features/payments/api'
import { procurementApi } from './features/procurement/api'
import { projectApi } from './features/projects/api'
import { settlementApi } from './features/settlement/api'
import { ProjectCreationModal } from './features/projects/ProjectCreationModal'
import { ProjectList } from './features/projects/ProjectList'
import { useProjects } from './features/projects/useProjects'
import { ApprovalInboxPanel } from './features/approvals/ApprovalInboxPanel'
import { RevisionManagementPanel } from './features/changes/RevisionManagementPanel'
import { revisionApi } from './features/changes/revision-api'
import { ProjectOverview } from './features/dashboard/ProjectOverview'
import { ExecutiveDashboard } from './features/dashboard/ExecutiveDashboard'
import { ContractManagementPanel } from './features/contracts/ContractManagementPanel'
import { ExecutionBudgetDetailPanel } from './features/execution-budget/ExecutionBudgetDetailPanel'
import { ProgressPaymentPanel } from './features/payments/ProgressPaymentPanel'
import { claimEntryDefaults } from './features/payments/progress-claim'
import { PurchaseOrderHistoryPanel } from './features/procurement/PurchaseOrderHistoryPanel'
import { VendorComparisonPanel } from './features/procurement/VendorComparisonPanel'
import { SettlementPanel } from './features/settlement/SettlementPanel'
import { DailyLogPanel } from './features/site-records/DailyLogPanel'
import { MeetingMinutesPanel } from './features/site-records/MeetingMinutesPanel'
import { toNumber } from './lib/format'
import {
  AlertTriangle,
  FolderPlus,
} from 'lucide-react'

const tabs = ['고객 견적·계약 관리', '세부실행예산', '업체 비교견적', '통합결재함', '계약/발주 이력', '기성 및 지급 현황', '현장 작업일지', '현장 회의록', '준공정산']
const projectTabs = tabs.filter((tab) => tab !== '통합결재함')

function newContractItem(): CustomerContractItem {
  return {
    work_category: '',
    quoted_amount: 0,
    contract_amount: 0,
    vat_amount: 0,
    total_amount: 0,
    note: '',
  }
}

function newExecutionBudgetDetail(projectCode = ''): ExecutionBudgetDetail {
  return {
    budget_item_id: '',
    category_name: '',
    item_name: '',
    cost_type: '외주비',
    vendor_description: '',
    unit: '식',
    quantity: 0,
    unit_price: 0,
    amount: 0,
    evidence_link: '',
    note: projectCode ? `${projectCode} 입력` : '',
  }
}

function newVendorComparison(itemId = 'EX-001', suffix = 'A'): VendorComparisonItem {
  return {
    comparison_code: `CMP-${itemId.replace('EX-', '')}-${suffix}`,
    vendor_name: '',
    quoted_amount: 0,
    vat_type: '별도',
    construction_period: '',
    scope_and_notes: '',
    payment_terms: '',
    estimate_file_url: '',
    is_selected: false,
    selection_reason: '',
  }
}

function App() {
  const { user, can, notice } = useAuth()
  const updateView = useWorkspaceUpdate()
  const availablePages = (['projects', 'executive', 'approvals', 'changes'] as const).filter(page => can(page === 'executive' ? 'DASHBOARD_READ' : page === 'approvals' ? 'APPROVAL_READ' : page === 'changes' ? 'CHANGE_READ' : 'PROJECT_READ'))
  const availableTabs = projectTabs.filter(tab => can(tab === '고객 견적·계약 관리' ? 'CONTRACT_READ' : tab === '세부실행예산' ? 'BUDGET_READ' : ['업체 비교견적', '계약/발주 이력'].includes(tab) ? 'PROCUREMENT_READ' : tab === '기성 및 지급 현황' ? 'PROGRESS_READ' : tab === '준공정산' ? 'SETTLEMENT_READ' : 'SITE_READ'))
  const [draftAccessResult, setDraftAccessResult] = useState<{ projectId: string; itemId: string; values: Record<string, boolean> } | null>(null)
  const [activePage, setActivePage] = useWorkspaceState<'projects' | 'executive' | 'approvals' | 'changes'>('page', availablePages[0] ?? 'projects', availablePages)
  const [selectedProjectId, setSelectedProjectId] = useWorkspaceState<string | null>('project', null)
  const [summary, setSummary] = useState<IntegratedSummary | null>(null)
  const [contract, setContract] = useState<CustomerContract | null>(null)
  const [contractItems, setContractItems] = useState<CustomerContractItem[]>([])
  const [executionBudget, setExecutionBudget] = useState<ExecutionBudgetDetailResponse | null>(null)
  const [executionDetails, setExecutionDetails] = useState<ExecutionBudgetDetail[]>([])
  const [executionBudgetApprovalFiles, setExecutionBudgetApprovalFiles] = useState<File[]>([])
  const [selectedExecutionItemId, setSelectedExecutionItemId] = useWorkspaceState<string>('item', 'EX-001')
  const draftAccess = draftAccessResult?.projectId === selectedProjectId && draftAccessResult.itemId === selectedExecutionItemId ? draftAccessResult.values : { CONTRACT_WRITE: false, BUDGET_WRITE: false, PROCUREMENT_WRITE: false }
  const [revisionProcurementItemIds, setRevisionProcurementItemIds] = useState<string[] | null>(null)
  const [vendorComparison, setVendorComparison] = useState<VendorComparisonResponse | null>(null)
  const [vendorComparisons, setVendorComparisons] = useState<VendorComparisonItem[]>([])
  const [purchaseOrderDraft, setPurchaseOrderDraft] = useState<PurchaseOrderDraft | null>(null)
  const [purchaseOrderHistory, setPurchaseOrderHistory] = useState<PurchaseOrderHistoryItem[]>([])
  const [purchaseOrderHistoryRevision, setPurchaseOrderHistoryRevision] = useState<number | undefined>()
  const [approvals, setApprovals] = useState<ApprovalItem[]>([])
  const [approvalProjects, setApprovalProjects] = useState<ApprovalProject[]>([])
  const [pendingApprovalCount, setPendingApprovalCount] = useState(0)
  const [selectedApproval, setSelectedApproval] = useState<ApprovalItem | null>(null)
  const [selectedApprovalId, setSelectedApprovalId] = useWorkspaceState<string>('approval', '', undefined, true)
  const [approvalProjectFilter] = useWorkspaceState<string>('approvalProject', '')
  const [approvalStatusFilter, setApprovalStatusFilter] = useWorkspaceState<string>('approvalStatus', 'REQUESTED', ['', 'REQUESTED', 'APPROVED', 'REJECTED', 'CANCELLED'])
  const [approvalTypeFilter, setApprovalTypeFilter] = useWorkspaceState<string>('approvalType', '')
  const [rejectComment, setRejectComment] = useState('')
  const [progressPayment, setProgressPayment] = useState<ProgressPaymentResponse | null>(null)
  const [settlement, setSettlement] = useState<ProjectSettlement | null>(null)
  const [selectedPoId, setSelectedPoId] = useWorkspaceState<string>('po', '')
  const [claimDraft, setClaimDraft] = useState<{ projectId: string; poId: string; amount: string } | null>(null)
  const selectedClaimPo = progressPayment?.project_id === selectedProjectId
    ? progressPayment.purchaseOrders.find(po => po.po_id === selectedPoId) ?? progressPayment.purchaseOrders[0]
    : undefined
  const claimDefaults = claimEntryDefaults(selectedClaimPo)
  const currentClaimDraft = claimDraft?.projectId === selectedProjectId && claimDraft?.poId === selectedClaimPo?.po_id ? claimDraft : null
  const claimForm = { ...claimDefaults, claim_amount: currentClaimDraft?.amount ?? claimDefaults.claim_amount }
  const claimEdited = !!currentClaimDraft && toNumber(currentClaimDraft.amount) !== toNumber(claimDefaults.claim_amount)
  const [selectedPaymentPoIds, setSelectedPaymentPoIds] = useState<string[]>([])
  const [settlementForm, setSettlementForm] = useState({ uncollected_receivable: '0', settlement_site_expense: '0', settlement_labor_expense: '0', settlement_tax: '0', settlement_notes: '', force_complete_if_unpaid: false, unpaid_handling_reason: '' })
  const [targetProfitRate, setTargetProfitRate] = useState('0')
  const [selectedFileType, setSelectedFileType] = useState('CONTRACT')
  const [activeTab, setActiveTab] = useWorkspaceState<string>('tab', availableTabs[0] ?? tabs[0], availableTabs)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const tabRef = useRef<HTMLDivElement | null>(null)
  const scope = useRef({ projectId: selectedProjectId, itemId: selectedExecutionItemId, poId: selectedPoId, approvalId: selectedApprovalId, approvalProjectId: approvalProjectFilter, page: activePage, status: approvalStatusFilter, type: approvalTypeFilter })
  useLayoutEffect(() => { scope.current = { projectId: selectedProjectId, itemId: selectedExecutionItemId, poId: selectedPoId, approvalId: selectedApprovalId, approvalProjectId: approvalProjectFilter, page: activePage, status: approvalStatusFilter, type: approvalTypeFilter } })
  const reads = useRef<Record<string, number>>({})
  const loadApprovalProjects = useCallback(async () => {
    if (!can('PROJECT_READ')) return
    const sequence = reads.current.approvalProjects = (reads.current.approvalProjects ?? 0) + 1
    try {
      const response = await projectApi.list('')
      if (!response.ok) return
      const data: ApprovalProject[] = await response.json()
      if (reads.current.approvalProjects === sequence) setApprovalProjects(data)
    } catch { /* Existing options and approval labels remain usable during a read failure. */ }
  }, [can])
  const statusApprovals = useMemo(() => approvals.filter(approval => matchesApproval(approval, { status: approvalStatusFilter, type: approvalTypeFilter })), [approvals, approvalStatusFilter, approvalTypeFilter])
  const visibleApprovals = useMemo(() => statusApprovals.filter(approval => matchesApproval(approval, { projectId: approvalProjectFilter })), [statusApprovals, approvalProjectFilter])
  const approvalProjectChoices = useMemo(() => approvalProjectOptions(approvalProjects, statusApprovals, approvalProjectFilter), [approvalProjects, statusApprovals, approvalProjectFilter])
  const [baselines, setBaselines] = useState({ contract: '', budget: '', vendor: '', settlement: '' })
  const fingerprints = {
    contract: JSON.stringify({ items: contractItems, rate: targetProfitRate }),
    budget: JSON.stringify(executionDetails),
    vendor: JSON.stringify(vendorComparisons),
    settlement: JSON.stringify(settlementForm),
  }
  const dirty = {
    contract: !!baselines.contract && fingerprints.contract !== baselines.contract,
    budget: !!baselines.budget && fingerprints.budget !== baselines.budget,
    vendor: !!baselines.vendor && fingerprints.vendor !== baselines.vendor,
    settlement: !!baselines.settlement && fingerprints.settlement !== baselines.settlement,
  }
  const editor = useRef({ dirty, fingerprints, settlementForm })
  useLayoutEffect(() => { editor.current = { dirty, fingerprints, settlementForm } })
  useDraftGuard(Object.values(dirty).some(Boolean) || executionBudgetApprovalFiles.length > 0 || claimEdited || !!rejectComment.trim())
  const { projects, keyword, setKeyword, isModalOpen, setIsModalOpen, form, setForm, refreshProjects, submitProject, deleteProject } = useProjects({
    onError: setError,
    onDeleted: (projectId) => { if (selectedProjectId === projectId) setSelectedProjectId(null) },
  })

  useEffect(() => {
    setBaselines({ contract: '', budget: '', vendor: '', settlement: '' })
    setClaimDraft(null)
    setIsLoading(false)
    setSummary(null)
    setContract(null)
    setExecutionBudget(null)
    setVendorComparison(null)
    setProgressPayment(null)
    setSettlement(null)
    if (!selectedProjectId) {
      setSummary(null)
      setContract(null)
      setContractItems([])
      setExecutionBudget(null)
      setExecutionDetails([])
      setExecutionBudgetApprovalFiles([])
      setRevisionProcurementItemIds(null)
      setVendorComparison(null)
      setVendorComparisons([])
      setPurchaseOrderDraft(null)
      setPurchaseOrderHistory([])
      setPurchaseOrderHistoryRevision(undefined)
      setApprovals([])
      setSelectedApproval(null)
      setProgressPayment(null)
      setSelectedPaymentPoIds([])
      setSettlement(null)
      setSelectedPoId('')
      return
    }

    const controller = new AbortController()
    async function loadSummary() {
      const sequence = reads.current.summary = (reads.current.summary ?? 0) + 1
      try {
        setIsLoading(true)
        const response = await projectApi.integratedSummary(selectedProjectId!, controller.signal)
        if (controller.signal.aborted || scope.current.projectId !== selectedProjectId || reads.current.summary !== sequence) return
        if (!response.ok) {
          if (response.status === 403 || response.status === 404) {
            updateView({ project: null, item: null, po: null }, true)
            notify('해당 현장이 삭제되었거나 조회 권한이 없어 프로젝트 목록으로 이동했습니다.', 'info')
            return
          }
          throw new Error(`통합현황 조회 실패: ${response.status}`)
        }
        const loaded: IntegratedSummary = await response.json()
        if (controller.signal.aborted || scope.current.projectId !== selectedProjectId || reads.current.summary !== sequence) return
        setSummary(loaded)
        setError(null)
      } catch (err) {
        if (err instanceof DOMException && err.name === 'AbortError') {
          return
        }
        setError(err instanceof Error ? err.message : '통합현황을 불러오지 못했습니다.')
      } finally {
        if (!controller.signal.aborted && scope.current.projectId === selectedProjectId) setIsLoading(false)
      }
    }

    loadSummary()
    return () => controller.abort()
  }, [selectedProjectId])

  useEffect(() => {
    if (!selectedProjectId || !selectedExecutionItemId) {
      return
    }
    loadVendorComparisons(selectedProjectId, selectedExecutionItemId)
  }, [selectedProjectId, selectedExecutionItemId])

  useEffect(() => {
    if (!selectedProjectId) {
      return
    }
    loadCustomerContract(selectedProjectId)
    loadExecutionBudgetDetails(selectedProjectId)
    loadApprovals()
    loadProgressPayments(selectedProjectId)
    loadPurchaseOrders(selectedProjectId)
    loadSettlement(selectedProjectId)
  }, [selectedProjectId])

  useEffect(() => {
    if (!selectedProjectId && activePage !== 'approvals') {
      return
    }
    loadApprovals()
  }, [approvalStatusFilter, approvalTypeFilter, activePage, selectedProjectId])

  useEffect(() => {
    void loadPendingApprovalCount()
  }, [])

  useEffect(() => {
    // This starts an asynchronous catalog read; state is updated only after the response.
    // eslint-disable-next-line react/set-state-in-effect
    if (activePage === 'approvals') void loadApprovalProjects()
  }, [activePage, loadApprovalProjects])

  useEffect(() => {
    if (!selectedProjectId) return
    let live = true
    void apiJson<Record<string, boolean>>(`/api/v1/projects/${selectedProjectId}/access?itemId=${encodeURIComponent(selectedExecutionItemId)}`).then(value => { if (live) setDraftAccessResult({ projectId: selectedProjectId, itemId: selectedExecutionItemId, values: value }) }).catch(() => {})
    return () => { live = false }
  }, [selectedProjectId, selectedExecutionItemId])

  const selectedProject = useMemo(
    () => projects.find((project) => project.project_id === selectedProjectId) ?? (summary?.project_info.project_id === selectedProjectId ? summary.project_info : undefined),
    [projects, selectedProjectId, summary],
  )

  useLiveRefresh(async () => {
    const projectId = scope.current.projectId
    const tasks: Promise<unknown>[] = [refreshProjects(keyword), loadApprovals()]
    if (scope.current.page === 'approvals') tasks.push(loadApprovalProjects())
    if (projectId) {
      tasks.push(loadCustomerContract(projectId, true), loadExecutionBudgetDetails(projectId, true), loadProgressPayments(projectId), loadPurchaseOrders(projectId), loadSettlement(projectId, true))
      if (scope.current.itemId) tasks.push(loadVendorComparisons(projectId, scope.current.itemId, true))
      tasks.push(refreshSummary(projectId))
      const itemId = scope.current.itemId
      tasks.push(apiJson<Record<string, boolean>>(`/api/v1/projects/${projectId}/access?itemId=${encodeURIComponent(itemId)}`).then(values => {
        if (scope.current.projectId === projectId && scope.current.itemId === itemId) setDraftAccessResult({ projectId, itemId, values })
      }))
    }
    await Promise.allSettled(tasks)
  })

  useEffect(() => {
    if (selectedApprovalId && can('APPROVAL_READ')) void loadApprovalDetail(selectedApprovalId)
  }, [selectedApprovalId])

  async function refreshSummary(projectId: string) {
    const sequence = reads.current.summary = (reads.current.summary ?? 0) + 1
    const response = await projectApi.integratedSummary(projectId)
    if (!response.ok) return
    const latest: IntegratedSummary = await response.json()
    if (scope.current.projectId === projectId && reads.current.summary === sequence) setSummary(latest)
  }

  async function startConstruction() {
    if (!can('PROJECT_STATUS')) return
    if (!selectedProjectId || !selectedProject) return
    if (!await confirmAction({ title: '공사 시작 확인', message: `“${selectedProject.project_name}” 프로젝트의 공사를 시작하시겠습니까? 상태가 공사진행으로 변경됩니다.`, confirmLabel: '공사 시작' })) {
      return
    }

    const response = await projectApi.startConstruction(selectedProjectId)
    if (!response.ok) {
      const body = await response.json().catch(() => null) as { detail?: string; message?: string } | null
      setError(body?.detail ?? body?.message ?? `공사 시작 처리 실패: ${response.status}`)
      return
    }

    await response.json()
    await refreshProjects(keyword)
    await refreshSummary(selectedProjectId)
    setError(null)
  }

  async function synchronizePreparationStatus() {
    if (!can('PROJECT_STATUS')) return
    if (!selectedProjectId || !selectedProject) return
    if (!await confirmAction({ title: '프로젝트 상태 변경 확인', message: `“${selectedProject.project_name}” 프로젝트의 승인된 실행예산을 반영해 상태를 공사준비로 변경하시겠습니까?`, confirmLabel: '상태 변경' })) {
      return
    }

    const response = await projectApi.synchronizePreparationStatus(selectedProjectId)
    if (!response.ok) {
      const body = await response.json().catch(() => null) as { detail?: string; message?: string } | null
      setError(body?.detail ?? body?.message ?? `프로젝트 상태 보정 실패: ${response.status}`)
      return
    }

    await response.json()
    await refreshProjects(keyword)
    await refreshSummary(selectedProjectId)
    setError(null)
  }

  async function loadCustomerContract(projectId: string, preserveDraft = false) {
    if (!can('CONTRACT_READ')) return
    const sequence = reads.current.contract = (reads.current.contract ?? 0) + 1
    try {
      const response = await contractApi.get(projectId)
      if (!response.ok) {
        throw new Error(`고객 계약 조회 실패: ${response.status}`)
      }
      const data: CustomerContract = await response.json()
      if (scope.current.projectId !== projectId || reads.current.contract !== sequence) return
      setContract(data)
      if (!preserveDraft || !editor.current.dirty.contract) {
        const items = data.items.length > 0 ? data.items : [newContractItem()]
        const rate = String(data.target_profit_rate ?? 0)
        setBaselines(current => ({ ...current, contract: JSON.stringify({ items, rate }) }))
        setTargetProfitRate(rate)
        setContractItems(items)
      }
      if (!preserveDraft) setError(null)
    } catch (err) {
      if (!preserveDraft && scope.current.projectId === projectId) setError(err instanceof Error ? err.message : '고객 계약 정보를 불러오지 못했습니다.')
    }
  }

  async function saveCustomerContract(): Promise<boolean> {
    if (!can('CONTRACT_WRITE') || draftAccess.CONTRACT_WRITE === false) return false
    if (!selectedProjectId) {
      return false
    }
    setError(null)
    const payload = {
      target_profit_rate: Number(targetProfitRate || 0),
      items: contractItems
        .filter((item) => item.work_category.trim())
        .map((item) => ({
          work_category: item.work_category,
          quoted_amount: item.quoted_amount,
          contract_amount: item.contract_amount,
          vat_amount: item.vat_amount,
          note: item.note ?? '',
        })),
    }

    let response: Response
    try {
      response = await contractApi.save(selectedProjectId, payload)
    } catch (err) {
      setError(err instanceof Error ? err.message : '고객 계약을 저장하지 못했습니다.')
      return false
    }

    if (!response.ok) {
      setError(`고객 계약 저장 실패: ${response.status}`)
      return false
    }

    const data: CustomerContract = await response.json()
    setBaselines(current => ({ ...current, contract: JSON.stringify({ items: data.items.length > 0 ? data.items : [newContractItem()], rate: String(data.target_profit_rate ?? 0) }) }))
    setContract(data)
    setContractItems(data.items.length > 0 ? data.items : [newContractItem()])
    setTargetProfitRate(String(data.target_profit_rate ?? 0))
    await refreshProjects(keyword)
    if (selectedProjectId) {
      await refreshSummary(selectedProjectId)
      await loadSettlement(selectedProjectId, true)
    }
    return true
  }

  async function uploadContractFile(file: File | null) {
    if (!can('CONTRACT_WRITE') || draftAccess.CONTRACT_WRITE === false) return
    if (!selectedProjectId || !file) {
      return
    }

    const formData = new FormData()
    formData.append('fileType', selectedFileType)
    formData.append('file', file)

    const response = await contractApi.upload(selectedProjectId, formData)

    if (!response.ok) {
      setError(`계약 파일 업로드 실패: ${response.status}`)
      return
    }

    const data: CustomerContract = await response.json()
    setContract(data)
  }

  async function deleteContractFile(fileId: string) {
    if (!can('CONTRACT_WRITE') || draftAccess.CONTRACT_WRITE === false) return
    if (!selectedProjectId) {
      return
    }

    if (!await confirmAction({ title: '계약 첨부파일 삭제', message: '선택한 계약 첨부파일을 삭제하시겠습니까?', confirmLabel: '삭제', destructive: true })) return

    const response = await contractApi.deleteFile(selectedProjectId, fileId)

    if (!response.ok) {
      setError(`계약 파일 삭제 실패: ${response.status}`)
      return
    }

    const data: CustomerContract = await response.json()
    setContract(data)
    setError(null)
  }

  async function loadExecutionBudgetDetails(projectId: string, preserveDraft = false) {
    if (!can('BUDGET_READ')) return
    const sequence = reads.current.budget = (reads.current.budget ?? 0) + 1
    try {
      const [response, procurementResponse] = await Promise.all([executionBudgetApi.get(projectId), revisionApi.procurementItems(projectId)])
      if (!response.ok) {
        throw new Error(`세부실행예산 조회 실패: ${response.status}`)
      }
      const data: ExecutionBudgetDetailResponse = await response.json()
      if (scope.current.projectId !== projectId || reads.current.budget !== sequence) return
      let eligibleItemIds: string[] | null = null
      if (procurementResponse.ok) {
        const procurement: { has_approved_revision: boolean; item_ids: string[] } = await procurementResponse.json()
        eligibleItemIds = procurement.has_approved_revision ? procurement.item_ids : null
        setRevisionProcurementItemIds(eligibleItemIds)
      }
      setExecutionBudget(data)
      if (!preserveDraft || !editor.current.dirty.budget) {
        const details = data.details.length > 0 ? data.details : [newExecutionBudgetDetail(data.project_code)]
        setBaselines(current => ({ ...current, budget: JSON.stringify(details) }))
        setExecutionDetails(details)
      }
      const eligibleItems = data.summary_by_item.filter((item) => item.approval_status === 'APPROVED' && item.procurement_required && (eligibleItemIds === null || eligibleItemIds.includes(item.budget_item_id)))
      if (preserveDraft && editor.current.dirty.vendor) return
      if (eligibleItems.length > 0) {
        if (!eligibleItems.some(item => item.budget_item_id === scope.current.itemId)) updateView({ item: eligibleItems[0].budget_item_id }, true)
      } else {
        updateView({ item: '' }, true)
      }
      if (!preserveDraft) setError(null)
    } catch (err) {
      if (!preserveDraft && scope.current.projectId === projectId) setError(err instanceof Error ? err.message : '세부실행예산을 불러오지 못했습니다.')
    }
  }

  async function saveExecutionBudgetDetails() {
    if (!can('BUDGET_WRITE') || draftAccess.BUDGET_WRITE === false) return
    if (!selectedProjectId) {
      return
    }
    const payload = {
      details: executionDetails
        .filter((detail) => detail.budget_item_id.trim() && detail.category_name.trim())
        .map((detail) => ({
          budget_item_id: detail.budget_item_id,
          category_name: detail.category_name,
          item_name: detail.item_name,
          cost_type: detail.cost_type,
          vendor_description: detail.vendor_description ?? '',
          unit: detail.unit ?? '',
          quantity: detail.quantity,
          unit_price: detail.unit_price,
          evidence_link: detail.evidence_link ?? '',
          note: detail.note ?? '',
        })),
    }

    const response = executionBudget?.applied_revision_id && executionBudget.applied_revision_status !== 'APPROVED'
      ? await revisionApi.saveBudget(selectedProjectId, executionBudget.applied_revision_id, payload)
      : await executionBudgetApi.save(selectedProjectId, payload)
    if (!response.ok) {
      const message = await response.text()
      setError(message || `세부실행예산 저장 실패: ${response.status}`)
      return
    }

    await loadExecutionBudgetDetails(selectedProjectId)
    await loadSettlement(selectedProjectId, true)
  }

  async function requestExecutionBudgetApproval() {
    if (!can('BUDGET_REQUEST')) return
    if (!selectedProjectId) return
    if (editor.current.dirty.budget) { notify('작성 중인 실행예산을 먼저 저장한 뒤 결재를 요청해주세요.', 'info'); return }
    if (!await confirmAction({ title: '실행예산 결재 요청', message: `“${selectedProject?.project_name ?? ''}” 현장의 저장된 실행예산을 결재 요청하시겠습니까?`, confirmLabel: '결재 요청' })) return
    if (executionBudget?.applied_revision_id) {
      const response = await revisionApi.requestApproval(selectedProjectId, executionBudget.applied_revision_id)
      if (!response.ok) {
        const message = await response.text()
        setError(message || `변경 차수 결재요청 실패: ${response.status}`)
        return
      }
      setExecutionBudgetApprovalFiles([])
      await loadExecutionBudgetDetails(selectedProjectId)
      await loadApprovals()
      return
    }
    const formData = new FormData()
    formData.append('request', new Blob([
      JSON.stringify({ request_reason: '최초 실행예산 승인 요청' }),
    ], { type: 'application/json' }))
    executionBudgetApprovalFiles.forEach((file) => formData.append('files', file))
    const response = await executionBudgetApi.requestApproval(selectedProjectId, formData)
    if (!response.ok) {
      setError(`실행예산 결재요청 실패: ${response.status}`)
      return
    }
    await response.json()
    setExecutionBudgetApprovalFiles([])
    await loadExecutionBudgetDetails(selectedProjectId)
    await loadApprovals()
  }

  async function loadApprovals() {
    if (!can('APPROVAL_READ')) return
    void loadPendingApprovalCount()
    if (!selectedProjectId && activePage !== 'approvals') return
    const params = new URLSearchParams()
    if (selectedProjectId && activePage !== 'approvals') params.set('projectId', selectedProjectId)
    if (approvalStatusFilter) params.set('status', approvalStatusFilter)
    if (approvalTypeFilter) params.set('type', approvalTypeFilter)
    const scopeKey = JSON.stringify([selectedProjectId, activePage, approvalStatusFilter, approvalTypeFilter])
    const sequence = reads.current.approvals = (reads.current.approvals ?? 0) + 1
    const response = await approvalApi.list(params)
    if (!response.ok) {
      setError(`통합결재함 조회 실패: ${response.status}`)
      return
    }
    const data: ApprovalItem[] = await response.json()
    if (reads.current.approvals !== sequence || scopeKey !== JSON.stringify([scope.current.projectId, scope.current.page, scope.current.status, scope.current.type])) return
    setApprovals(data)
    const detailId = scope.current.approvalId
    if (detailId && data.some(approval => approval.approval_id === detailId && matchesApproval(approval, { projectId: scope.current.approvalProjectId }))) await loadApprovalDetail(detailId)
    else if (detailId) { setSelectedApproval(null); setSelectedApprovalId('') }
  }

  async function loadApprovalDetail(approvalId: string) {
    if (!can('APPROVAL_READ')) return
    if (scope.current.approvalId !== approvalId) setSelectedApprovalId(approvalId)
    const sequence = reads.current.detail = (reads.current.detail ?? 0) + 1
    const response = await approvalApi.detail(approvalId)
    if (!response.ok) {
      if (scope.current.approvalId === approvalId) { setSelectedApproval(null); if (response.status === 403 || response.status === 404) setSelectedApprovalId('') }
      return
    }
    const detail: ApprovalItem = await response.json()
    if (scope.current.approvalId === approvalId && reads.current.detail === sequence) {
      if (!matchesApproval(detail, { projectId: scope.current.approvalProjectId, status: scope.current.status, type: scope.current.type })) {
        setSelectedApproval(null)
        updateView({ approval: null }, true)
        return
      }
      setSelectedApproval(detail)
    }
  }

  async function changeApprovalProject(projectId: string) {
    if (projectId === approvalProjectFilter) return
    if (rejectComment.trim() && !await confirmAction({ title: '작성 중인 결재 의견 확인', message: '입력 중인 반려·승인취소 사유를 지우고 다른 현장의 결재를 보시겠습니까?', confirmLabel: '현장 변경' })) return
    setSelectedApproval(null)
    setRejectComment('')
    updateView({ approvalProject: projectId || null, approval: null })
  }

  async function decideApproval(approvalId: string, decision: 'approve' | 'reject' | 'cancel') {
    if (decision === 'cancel') {
      if (!can('APPROVAL_DECIDE')) return
    } else if (!canDecideApproval(selectedApproval?.approval_id === approvalId ? selectedApproval : approvals.find((item) => item.approval_id === approvalId), user, can)) return
    const decisionLabel = decision === 'approve' ? '승인' : decision === 'reject' ? '반려' : '승인취소'
    if (!await confirmAction({ title: `결재 ${decisionLabel} 확인`, message: decision === 'cancel' ? '승인 완료된 실행예산을 승인취소하고 재작업 상태로 전환할까요? 기존 승인 이력은 보존됩니다.' : `이 결재를 ${decisionLabel}하시겠습니까?${decision === 'reject' ? `\n반려 사유: ${rejectComment}` : '\n본인 결재 단계가 처리되고 다음 단계가 화면에 반영됩니다.'}`, confirmLabel: decisionLabel, destructive: decision !== 'approve' })) return
    const response = await approvalApi.decide(approvalId, decision, { comment: decision === 'approve' ? '승인' : rejectComment })
    if (!response.ok) {
      const body = await response.json().catch(() => null) as { message?: string } | null
      setError(body?.message ?? `결재 처리 실패: ${response.status}`)
      return
    }
    setRejectComment('')
    const decidedApproval: ApprovalItem = await response.json()
    setSelectedApproval(decidedApproval)
    await loadApprovals()
    await refreshProjects(keyword)
    if (selectedProjectId) {
      await loadExecutionBudgetDetails(selectedProjectId, true)
      if (selectedExecutionItemId) await loadVendorComparisons(selectedProjectId, selectedExecutionItemId, true)
      await refreshSummary(selectedProjectId)
      await loadProgressPayments(selectedProjectId)
      await loadPurchaseOrders(selectedProjectId)
      await loadSettlement(selectedProjectId, true)
    }
  }

  function updateExecutionDetail(index: number, field: keyof ExecutionBudgetDetail, value: string) {
    setExecutionDetails((details) =>
      details.map((detail, detailIndex) => {
        if (detailIndex !== index) {
          return detail
        }
        if (field === 'quantity' || field === 'unit_price') {
          const next = { ...detail, [field]: toNumber(value) }
          next.amount = (next.quantity || 0) * (next.unit_price || 0)
          return next
        }
        if (field === 'category_name') {
          return { ...detail, category_name: value, item_name: value }
        }
        return { ...detail, [field]: value }
      }),
    )
  }

  function addExecutionDetail(copyIndex?: number) {
    setExecutionDetails((details) => {
      const base = copyIndex === undefined ? newExecutionBudgetDetail(selectedProject?.project_code) : { ...details[copyIndex], detail_id: undefined, vendor_description: '', quantity: 0, unit_price: 0, amount: 0, note: '' }
      return [...details, base]
    })
  }

  function removeExecutionDetail(index: number) {
    setExecutionDetails((details) => details.filter((_, detailIndex) => detailIndex !== index))
  }

  async function loadVendorComparisons(projectId: string, itemId: string, preserveDraft = false) {
    if (!can('PROCUREMENT_READ')) return
    const sequence = reads.current.vendor = (reads.current.vendor ?? 0) + 1
    try {
      const response = await procurementApi.comparisons(projectId, itemId)
      if (!response.ok) {
        throw new Error(`업체 비교견적 조회 실패: ${response.status}`)
      }
      const data: VendorComparisonResponse = await response.json()
      if (scope.current.projectId !== projectId || scope.current.itemId !== itemId || reads.current.vendor !== sequence) return
      setVendorComparison(data)
      if (!preserveDraft || !editor.current.dirty.vendor) {
        const comparisons = data.comparisons.length > 0 ? data.comparisons : [newVendorComparison(itemId, 'A'), newVendorComparison(itemId, 'B')]
        setBaselines(current => ({ ...current, vendor: JSON.stringify(comparisons) }))
        setVendorComparisons(comparisons)
      }
      await loadPurchaseOrderDraft(projectId, itemId, false)
      if (!preserveDraft) setError(null)
    } catch (err) {
      if (!preserveDraft && scope.current.projectId === projectId && scope.current.itemId === itemId) setError(err instanceof Error ? err.message : '업체 비교견적을 불러오지 못했습니다.')
    }
  }

  async function loadPurchaseOrderDraft(projectId: string, itemId: string, showError = true) {
    if (!can('PROCUREMENT_READ')) return
    const sequence = reads.current.draft = (reads.current.draft ?? 0) + 1
    const response = await procurementApi.draft(projectId, itemId)
    if (scope.current.projectId !== projectId || scope.current.itemId !== itemId || reads.current.draft !== sequence) return
    if (!response.ok) {
      setPurchaseOrderDraft(null)
      if (showError) setError(`발주품의 초안 조회 실패: ${response.status}`)
      return
    }
    const data: PurchaseOrderDraft = await response.json()
    if (scope.current.projectId !== projectId || scope.current.itemId !== itemId || reads.current.draft !== sequence) return
    setPurchaseOrderDraft(data)
    await loadSettlement(projectId, true)
  }

  async function loadPurchaseOrders(projectId: string) {
    if (!can('PROCUREMENT_READ')) return
    const sequence = reads.current.orders = (reads.current.orders ?? 0) + 1
    const response = await procurementApi.orders(projectId)
    if (!response.ok) {
      setError(`발주 이력 조회 실패: ${response.status}`)
      return
    }
    const data: PurchaseOrderHistoryResponse = await response.json()
    if (scope.current.projectId !== projectId || reads.current.orders !== sequence) return
    setPurchaseOrderHistory(data.items)
    setPurchaseOrderHistoryRevision(data.revision_number)
  }

  async function persistVendorComparisons(announceSave = true): Promise<VendorComparisonResponse | null> {
    if (!can('PROCUREMENT_WRITE') || draftAccess.PROCUREMENT_WRITE === false) return null
    if (!selectedProjectId || !selectedExecutionItemId) return null
    const response = await procurementApi.saveComparisons(selectedProjectId, selectedExecutionItemId, {
      comparisons: vendorComparisons
        .filter((comparison) => comparison.vendor_name.trim())
        .map((comparison) => ({
          comparison_code: comparison.comparison_code,
          vendor_name: comparison.vendor_name,
          quoted_amount: comparison.quoted_amount,
          vat_type: comparison.vat_type || '별도',
          construction_period: comparison.construction_period ?? '',
          scope_and_notes: comparison.scope_and_notes ?? '',
          payment_terms: comparison.payment_terms ?? '',
          estimate_file_url: comparison.estimate_file_url ?? '',
          is_selected: comparison.is_selected,
          selection_reason: comparison.selection_reason ?? '',
        })),
    }, announceSave)
    if (!response.ok) {
      setError(`업체 비교견적 저장 실패: ${response.status}`)
      return null
    }
    const data: VendorComparisonResponse = await response.json()
    setBaselines(current => ({ ...current, vendor: JSON.stringify(data.comparisons.length > 0 ? data.comparisons : [newVendorComparison(selectedExecutionItemId, 'A')]) }))
    setVendorComparison(data)
    setVendorComparisons(data.comparisons.length > 0 ? data.comparisons : [newVendorComparison(selectedExecutionItemId, 'A')])
    await loadPurchaseOrderDraft(selectedProjectId, selectedExecutionItemId, false)
    return data
  }

  async function loadPendingApprovalCount() {
    if (!can('APPROVAL_READ')) return
    const sequence = reads.current.pending = (reads.current.pending ?? 0) + 1
    try {
      const response = await approvalApi.list(new URLSearchParams({ status: 'REQUESTED' }))
      if (!response.ok) return
      const data: ApprovalItem[] = await response.json()
      if (reads.current.pending !== sequence) return
      setPendingApprovalCount(user?.position === 'CEO' || user?.position === 'TEAM_LEADER' ? data.filter((approval) => canDecideApproval(approval, user, can)).length : data.length)
    } catch {
      // Navigation stays usable even when the notification count cannot be refreshed.
    }
  }

  async function saveVendorComparisons() {
    await persistVendorComparisons()
  }

  async function requestPurchaseOrder() {
    if (!can('PROCUREMENT_REQUEST')) return
    if (!selectedProjectId || !selectedExecutionItemId) return
    const selected = vendorComparisons.find((comparison) => comparison.is_selected)
    if (!selected || !selected.vendor_name.trim() || selected.quoted_amount <= 0 || !selected.selection_reason?.trim()) {
      setError('발주품의를 요청하려면 업체명, 견적금액, 선정 업체, 선정 사유를 모두 입력해야 합니다.')
      return
    }
    if (!await confirmAction({ title: '발주품의 결재 요청', message: `선정 업체: ${selected.vendor_name}\n요청 금액: ${selected.quoted_amount.toLocaleString('ko-KR')}원\n\n비교견적을 저장하고 발주품의를 결재 요청하시겠습니까?`, confirmLabel: '결재 요청' })) return
    const saved = await persistVendorComparisons(false)
    if (!saved) return
    const response = await procurementApi.requestDraft(selectedProjectId, selectedExecutionItemId)
    if (!response.ok) {
      setError(`발주품의 요청 실패: ${response.status}`)
      return
    }
    setPurchaseOrderDraft(await response.json())
    await loadExecutionBudgetDetails(selectedProjectId, true)
    await loadApprovals()
  }

  function updateVendorComparison(index: number, field: keyof VendorComparisonItem, value: string | boolean) {
    setVendorComparisons((items) =>
      items.map((item, itemIndex) => {
        if (itemIndex !== index) {
          return field === 'is_selected' && value === true ? { ...item, is_selected: false } : item
        }
        if (field === 'quoted_amount') return { ...item, quoted_amount: toNumber(String(value)) }
        if (field === 'is_selected') return { ...item, is_selected: Boolean(value) }
        return { ...item, [field]: value }
      }),
    )
  }

  async function uploadVendorComparisonFile(index: number, file: File) {
    if (!can('PROCUREMENT_WRITE') || draftAccess.PROCUREMENT_WRITE === false) return
    if (!selectedProjectId || !selectedExecutionItemId) return
    const saved = await persistVendorComparisons(false)
    const comparison = saved?.comparisons[index]
    if (!comparison?.comparison_id) {
      setError('업체명 입력 후 비교견적을 저장해야 파일을 첨부할 수 있습니다.')
      return
    }
    const response = await procurementApi.uploadComparisonAttachment(
      selectedProjectId,
      selectedExecutionItemId,
      comparison.comparison_id,
      file,
    )
    if (!response.ok) {
      setError(`비교견적 파일 첨부 실패: ${response.status}`)
      return
    }
    const attachment = await response.json()
    const attach = (items: VendorComparisonItem[]) => items.map((item, itemIndex) =>
      itemIndex === index ? { ...item, attachments: [...(item.attachments ?? []), attachment] } : item,
    )
    setVendorComparisons(attach)
    if (saved) setBaselines(current => ({ ...current, vendor: JSON.stringify(attach(saved.comparisons)) }))
    setVendorComparison((current) => current ? { ...current, comparisons: attach(current.comparisons) } : current)
    setError(null)
  }

  function addVendorComparison() {
    const suffix = String.fromCharCode(65 + vendorComparisons.length)
    setVendorComparisons((items) => [...items, newVendorComparison(selectedExecutionItemId, suffix)])
  }

  function removeVendorComparison(index: number) {
    setVendorComparisons((items) => items.filter((_, itemIndex) => itemIndex !== index))
  }

  async function loadProgressPayments(projectId: string): Promise<ProgressPaymentResponse | null> {
    if (!can('PROGRESS_READ')) return null
    const sequence = reads.current.progress = (reads.current.progress ?? 0) + 1
    try {
      const response = await paymentApi.status(projectId)
      if (!response.ok) throw new Error(`기성/지급 현황 조회 실패: ${response.status}`)
      const data: ProgressPaymentResponse = await response.json()
      if (scope.current.projectId !== projectId || reads.current.progress !== sequence) return data
      setProgressPayment(data)
      if (!data.purchaseOrders.some(po => po.po_id === scope.current.poId)) updateView({ po: data.purchaseOrders[0]?.po_id ?? null }, true)
      setError(null)
      return data
    } catch (err) {
      setError(err instanceof Error ? err.message : '기성/지급 현황을 불러오지 못했습니다.')
      return null
    }
  }

  async function loadSettlement(projectId: string, preserveDraft = false) {
    if (!can('SETTLEMENT_READ')) return
    const sequence = reads.current.settlement = (reads.current.settlement ?? 0) + 1
    try {
      const response = await settlementApi.get(projectId)
      if (!response.ok) throw new Error(`준공정산 조회 실패: ${response.status}`)
      const data: ProjectSettlement = await response.json()
      if (scope.current.projectId !== projectId || reads.current.settlement !== sequence) return
      setSettlement(data)
      const form = editor.current.settlementForm
      const latestForm = {
        ...form,
        uncollected_receivable: String(data.payments.uncollected_receivable ?? 0),
        settlement_site_expense: String(data.settlement_site_expense ?? 0),
        settlement_labor_expense: String(data.settlement_labor_expense ?? 0),
        settlement_tax: String(data.settlement_tax ?? 0),
        settlement_notes: data.settlement_notes ?? form.settlement_notes,
        unpaid_handling_reason: data.unpaid_handling_reason ?? form.unpaid_handling_reason,
      }
      if (!preserveDraft || !editor.current.dirty.settlement) {
        setBaselines(current => ({ ...current, settlement: JSON.stringify(latestForm) }))
        setSettlementForm(latestForm)
      }
      if (!preserveDraft) setError(null)
    } catch (err) {
      if (!preserveDraft && scope.current.projectId === projectId) setError(err instanceof Error ? err.message : '준공정산 정보를 불러오지 못했습니다.')
    }
  }

  async function requestSettlementApproval() {
    if (!can('SETTLEMENT_REQUEST')) return
    if (!selectedProjectId) return
    if (!await confirmAction({ title: '준공정산 결재 요청', message: '입력한 현장경비·인건비·세금과 정산 내용을 포함해 준공정산 결재를 요청하시겠습니까?', confirmLabel: '결재 요청' })) return
    const response = await settlementApi.requestApproval(selectedProjectId, {
      uncollected_receivable: toNumber(settlementForm.uncollected_receivable),
      settlement_site_expense: toNumber(settlementForm.settlement_site_expense),
      settlement_labor_expense: toNumber(settlementForm.settlement_labor_expense),
      settlement_tax: toNumber(settlementForm.settlement_tax),
      settlement_notes: settlementForm.settlement_notes,
      force_complete_if_unpaid: settlementForm.force_complete_if_unpaid,
      unpaid_handling_reason: settlementForm.unpaid_handling_reason,
      settled_by: selectedProject?.manager_id ?? 'system',
    })
    if (!response.ok) {
      const message = await response.text()
      setError(message || `준공정산 결재요청 실패: ${response.status}`)
      return
    }
    setSettlement(await response.json())
    setBaselines(current => ({ ...current, settlement: editor.current.fingerprints.settlement }))
    await refreshProjects(keyword)
    await loadApprovals()
    await refreshSummary(selectedProjectId)
  }

  async function createProgressClaim() {
    if (!can('PROGRESS_WRITE')) return
    if (!selectedProjectId || !selectedClaimPo) return
    const latest = await loadProgressPayments(selectedProjectId)
    if (!latest || scope.current.projectId !== selectedProjectId) return
    const purchaseOrder = latest.purchaseOrders.find(po => po.po_id === selectedClaimPo.po_id)
    if (!purchaseOrder || scope.current.poId !== purchaseOrder.po_id) { notify('선택한 발주를 다시 확인해주세요.', 'info'); return }
    const defaults = claimEntryDefaults(purchaseOrder)
    const amount = toNumber(currentClaimDraft?.amount ?? defaults.claim_amount)
    if (!Number.isFinite(amount) || amount <= 0 || amount > toNumber(defaults.claim_amount)) {
      notify(`기성금액은 0원보다 크고 기성 작성 가능 잔액 ${Number(defaults.claim_amount).toLocaleString('ko-KR')}원 이하여야 합니다.`, 'error')
      return
    }
    if (!await confirmAction({ title: '기성 등록 확인', message: `${purchaseOrder.vendor_name} (${purchaseOrder.po_id})\n${defaults.degree}차 기성 ${amount.toLocaleString('ko-KR')}원을 등록하시겠습니까?`, confirmLabel: '기성 등록' })) return
    if (scope.current.projectId !== selectedProjectId || scope.current.poId !== purchaseOrder.po_id) return
    const response = await paymentApi.createClaim(selectedProjectId, { po_id: purchaseOrder.po_id, degree: Number(defaults.degree), claim_amount: amount })
    if (!response.ok) {
      setError(`기성 작성 실패: ${response.status}`)
      return
    }
    const data: ProgressPaymentResponse = await response.json()
    setProgressPayment(data)
    setClaimDraft(null)
    await loadSettlement(selectedProjectId, true)
  }

  async function selectProgressPurchaseOrder(poId: string) {
    if (poId === selectedClaimPo?.po_id) return
    if (claimEdited && !await confirmAction({ title: '작성 중인 기성금액 확인', message: '입력 중인 기성금액을 저장하지 않고 다른 발주를 선택하시겠습니까?', confirmLabel: '발주 변경' })) return
    setClaimDraft(null)
    setSelectedPoId(poId)
  }

  async function deleteProgressClaim(claimId: string) {
    if (!can('PROGRESS_WRITE')) return
    if (!selectedProjectId || !await confirmAction({ title: '기성 삭제 확인', message: '결재 대기 또는 반려된 기성 건을 삭제하시겠습니까? 연결된 결재 요청도 함께 삭제됩니다.', confirmLabel: '삭제', destructive: true })) return
    const response = await paymentApi.deleteClaim(selectedProjectId, claimId)
    if (!response.ok) {
      setError(`기성 삭제 실패: ${response.status}`)
      return
    }
    const data: ProgressPaymentResponse = await response.json()
    setProgressPayment(data)
    await Promise.all([loadSettlement(selectedProjectId, true), loadApprovals()])
  }

  async function requestPaymentApprovals() {
    if (user?.position !== 'FINANCE' || !can('PAYMENT_REQUEST')) return
    if (!selectedProjectId || selectedPaymentPoIds.length === 0) return
    if (!await confirmAction({ title: '지급결재 요청', message: `선택한 ${selectedPaymentPoIds.length}개 업체의 지급결재를 대표에게 요청하시겠습니까? 실제 송금 등록과는 별도입니다.`, confirmLabel: '지급결재 요청' })) return
    const response = await paymentApi.requestPayments(selectedProjectId, { po_ids: selectedPaymentPoIds })
    if (!response.ok) {
      setError(`지급요청 실패: ${response.status}`)
      return
    }
    setSelectedPaymentPoIds([])
    await loadProgressPayments(selectedProjectId)
    await loadApprovals()
  }

  function updateContractItem(index: number, field: keyof CustomerContractItem, value: string) {
    setContractItems((items) =>
      items.map((item, itemIndex) => {
        if (itemIndex !== index) {
          return item
        }
        if (field === 'work_category' || field === 'note') {
          return { ...item, [field]: value }
        }
        const nextValue = toNumber(value)
        const next = { ...item, [field]: nextValue }
        if (field === 'contract_amount') {
          next.vat_amount = Math.round(nextValue * 0.1)
        }
        next.total_amount = (next.contract_amount ?? 0) + (next.vat_amount ?? 0)
        return next
      }),
    )
  }

  function addContractItem() {
    setContractItems((items) => [...items, newContractItem()])
  }

  function removeContractItem(index: number) {
    setContractItems((items) => items.filter((_, itemIndex) => itemIndex !== index))
  }

  function moveToTab(tab: string) {
    setActiveTab(tab)
    window.setTimeout(() => tabRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 0)
  }

  async function selectProject(projectId: string | null) {
    if (projectId === selectedProjectId) return
    if (hasUnsavedDraft() && !await confirmAction({ title: '작성 중인 내용 확인', message: '저장하지 않은 내용이 있습니다. 저장하지 않고 다른 프로젝트로 이동하시겠습니까?', confirmLabel: '이동', destructive: true })) return
    updateView({ project: projectId, tab: tabs[0], item: null, po: null, approval: null })
  }

  const contractTotal = useMemo(() => {
    const supply = contractItems.reduce((sum, item) => sum + (item.contract_amount || 0), 0)
    const vat = contractItems.reduce((sum, item) => sum + (item.vat_amount || 0), 0)
    const targetCostLimit = Math.round(supply * (1 - Number(targetProfitRate || 0) / 100))
    return {
      supply,
      vat,
      total: supply + vat,
      targetCostLimit,
    }
  }, [contractItems, targetProfitRate])

  const executionTotal = useMemo(
    () => executionDetails.reduce((sum, detail) => sum + (detail.quantity || 0) * (detail.unit_price || 0), 0),
    [executionDetails],
  )

  const executionSummary = useMemo(() => {
    const directSettlementCostTypes = new Set(['현장경비', '인건비'])
    const summary = new Map<string, ExecutionBudgetSummaryItem>()
    executionDetails.forEach((detail) => {
      const requiresProcurement = !directSettlementCostTypes.has(detail.cost_type)
      const current = summary.get(detail.budget_item_id) ?? {
        budget_item_id: detail.budget_item_id,
        category_name: detail.category_name,
        item_name: detail.item_name,
        approval_status: executionBudget?.approval_status ?? 'DRAFT',
        purchase_order_status: 'NOT_REQUESTED',
        procurement_required: requiresProcurement,
        rolled_up_amount: 0,
      }
      current.category_name = detail.category_name
      current.item_name = detail.item_name
      current.procurement_required ||= requiresProcurement
      current.rolled_up_amount += (detail.quantity || 0) * (detail.unit_price || 0)
      summary.set(detail.budget_item_id, current)
    })
    return [...summary.values()].sort((a, b) => a.budget_item_id.localeCompare(b.budget_item_id))
  }, [executionBudget?.approval_status, executionDetails])

  return (
    <DraftPermissions access={draftAccess}><main className="min-h-screen bg-[#f5f7fb] text-slate-900">
      <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 shadow-sm backdrop-blur">
        <div className="mx-auto w-full max-w-[1440px] px-4 sm:px-6 lg:px-8">
          <div className="flex min-h-16 items-center justify-between gap-4">
            <button type="button" onClick={() => setActivePage('projects')} className="text-left" aria-label="프로젝트 현황으로">
              <p className="text-lg font-black tracking-tight text-slate-950">아이엔씨 <span className="text-blue-700">ERP</span></p>
              <p className="text-[11px] font-medium text-slate-400">공사 통합관리</p>
            </button>
            <div className="flex items-center gap-3">
              {activePage === 'projects' && can('PROJECT_CREATE') && <button type="button" onClick={() => setIsModalOpen(true)} className="inline-flex items-center justify-center gap-2 rounded-lg bg-blue-700 px-3.5 py-2 text-sm font-bold text-white shadow-sm hover:bg-blue-800"><FolderPlus className="h-4 w-4" aria-hidden="true" />프로젝트 등록</button>}
              <UserMenu />
            </div>
          </div>
          <nav className="-mb-px flex items-end gap-1 overflow-x-auto" aria-label="최상위 메뉴">
            {([
              ['executive', '경영진 대시보드'],
              ['projects', '프로젝트 현황'],
              ['approvals', '통합결재함'],
              ['changes', '변경관리 이력'],
            ] as const).filter(([page]) => can(page === 'executive' ? 'DASHBOARD_READ' : page === 'approvals' ? 'APPROVAL_READ' : page === 'changes' ? 'CHANGE_READ' : 'PROJECT_READ')).map(([page, label]) => (
              <button key={page} type="button" onClick={() => setActivePage(page)} className={`relative shrink-0 border-b-2 px-4 py-3 text-sm font-bold transition ${activePage === page ? 'border-blue-600 text-blue-700' : 'border-transparent text-slate-500 hover:border-slate-300 hover:text-slate-800'}`}>
                {label}{page === 'approvals' && pendingApprovalCount > 0 && <span className="ml-1.5 inline-flex min-w-5 items-center justify-center rounded-full bg-rose-500 px-1.5 py-0.5 text-[10px] font-black text-white">{pendingApprovalCount}</span>}
              </button>
            ))}
          </nav>
        </div>
      </header>

      <div className="mx-auto flex w-full max-w-[1440px] flex-col gap-6 px-4 py-6 sm:px-6 lg:px-8">

        {notice && <p role="status" className="rounded bg-amber-50 p-3 text-sm">{notice}</p>}
        <p className="text-xs text-slate-500">변경 내용은 자동으로 확인합니다. 새로고침해도 보던 화면이 유지됩니다.{Object.values(dirty).some(Boolean) && <span className="ml-2 font-semibold text-amber-700">작성 중인 입력 내용은 자동 갱신으로 덮어쓰지 않습니다.</span>}</p>
        {error && (
          <section className="flex items-start gap-3 rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
            <p>{error}</p>
          </section>
        )}

        {activePage === 'projects' && (
          <>
        {!selectedProjectId ? (
          <ProjectList
            projects={projects}
            keyword={keyword}
            setKeyword={setKeyword}
            onSelect={projectId => { void selectProject(projectId) }}
            onDelete={userAction('프로젝트 삭제', deleteProject, undefined)}
          />
        ) : null}

        {isLoading && !summary ? (
          <section className="rounded-md border border-slate-200 bg-white p-8 text-center text-sm text-slate-500">
            통합현황을 불러오는 중입니다.
          </section>
        ) : summary ? (
          <>
            <ProjectOverview
              project={selectedProject}
              summary={summary}
              executionBudget={executionBudget}
              onBack={() => { void selectProject(null) }}
              onStartConstruction={userAction('공사 시작', startConstruction, undefined)}
              onSynchronizePreparation={userAction('프로젝트 상태 변경', synchronizePreparationStatus, undefined)}
              onMoveToTab={moveToTab}
            />

            <section ref={tabRef} className="scroll-mt-36">
              <div className="flex gap-1 overflow-x-auto border-b border-slate-200">
                {projectTabs.filter(tab => can(tab === '고객 견적·계약 관리' ? 'CONTRACT_READ' : tab === '세부실행예산' ? 'BUDGET_READ' : ['업체 비교견적', '계약/발주 이력'].includes(tab) ? 'PROCUREMENT_READ' : tab === '기성 및 지급 현황' ? 'PROGRESS_READ' : tab === '준공정산' ? 'SETTLEMENT_READ' : 'SITE_READ')).map((tab) => (
                  <button
                    key={tab}
                    type="button"
                    onClick={() => setActiveTab(tab)}
                    className={`-mb-px shrink-0 border-b-2 px-4 py-3 text-sm font-bold transition ${
                      activeTab === tab ? 'border-blue-600 text-blue-700' : 'border-transparent text-slate-500 hover:border-slate-300 hover:text-slate-800'
                    }`}
                  >
                    {tab}
                  </button>
                ))}
              </div>
              <div className="pt-6">
                {activeTab === '고객 견적·계약 관리' && can('CONTRACT_READ') && contract && (
                  <WriteScope permission="CONTRACT_WRITE"><ContractManagementPanel
                    contract={contract}
                    contractItems={contractItems}
                    contractTotal={contractTotal}
                    targetProfitRate={targetProfitRate}
                    selectedFileType={selectedFileType}
                    setTargetProfitRate={setTargetProfitRate}
                    setSelectedFileType={setSelectedFileType}
                    saveCustomerContract={userAction('계약 저장', saveCustomerContract, false)}
                    openChangeManagement={() => setActivePage('changes')}
                    uploadContractFile={userAction('계약 파일 등록', uploadContractFile, undefined)}
                    deleteContractFile={userAction('계약 파일 삭제', deleteContractFile, undefined)}
                    updateContractItem={updateContractItem}
                    addContractItem={addContractItem}
                    removeContractItem={removeContractItem}
                  /></WriteScope>
                )}
                {activeTab === '세부실행예산' && can('BUDGET_READ') && executionBudget && (
                  <WriteScope permission="BUDGET_WRITE"><ExecutionBudgetDetailPanel
                    executionBudget={executionBudget}
                    details={executionDetails}
                    summary={executionSummary}
                    totalAmount={executionTotal}
                    saveExecutionBudgetDetails={userAction('실행예산 저장', saveExecutionBudgetDetails, undefined)}
                    requestExecutionBudgetApproval={userAction('실행예산 결재 요청', requestExecutionBudgetApproval, undefined)}
                    approvalFiles={executionBudgetApprovalFiles}
                    setApprovalFiles={setExecutionBudgetApprovalFiles}
                    contractFiles={contract?.files ?? []}
                    updateExecutionDetail={updateExecutionDetail}
                    addExecutionDetail={addExecutionDetail}
                    removeExecutionDetail={removeExecutionDetail}
                  /></WriteScope>
                )}
                {activeTab === '업체 비교견적' && can('PROCUREMENT_READ') && executionBudget && vendorComparison && (
                  <WriteScope permission="PROCUREMENT_WRITE"><VendorComparisonPanel
                    executionBudget={executionBudget}
                    revisionProcurementItemIds={revisionProcurementItemIds}
                    selectedExecutionItemId={selectedExecutionItemId}
                    setSelectedExecutionItemId={setSelectedExecutionItemId}
                    vendorComparison={vendorComparison}
                    comparisons={vendorComparisons}
                    purchaseOrderDraft={purchaseOrderDraft}
                    saveVendorComparisons={userAction('비교견적 저장', saveVendorComparisons, undefined)}
                    requestPurchaseOrder={userAction('발주품의 결재 요청', requestPurchaseOrder, undefined)}
                    updateVendorComparison={updateVendorComparison}
                    addVendorComparison={addVendorComparison}
                    removeVendorComparison={removeVendorComparison}
                    uploadVendorComparisonFile={userAction('견적 첨부파일 등록', uploadVendorComparisonFile, undefined)}
                    openPurchaseOrderHistory={() => moveToTab('계약/발주 이력')}
                  /></WriteScope>
                )}
                {activeTab === '계약/발주 이력' && can('PROCUREMENT_READ') && (
                  <PurchaseOrderHistoryPanel purchaseOrders={purchaseOrderHistory} revisionNumber={purchaseOrderHistoryRevision} />
                )}
                {activeTab === '기성 및 지급 현황' && can('PROGRESS_READ') && progressPayment && (
                  <ProgressPaymentPanel
                    projectId={selectedProjectId!}
                    onPaymentRecorded={() => { void loadProgressPayments(selectedProjectId!); void refreshSummary(selectedProjectId!) }}
                    progressPayment={progressPayment}
                    selectedPoId={selectedPoId}
                    setSelectedPoId={poId => { void selectProgressPurchaseOrder(poId) }}
                    claimForm={claimForm}
                    setClaimAmount={value => { if (selectedProjectId && selectedClaimPo) setClaimDraft({ projectId: selectedProjectId, poId: selectedClaimPo.po_id, amount: value }) }}
                    selectedPaymentPoIds={selectedPaymentPoIds}
                    setSelectedPaymentPoIds={setSelectedPaymentPoIds}
                    createProgressClaim={userAction('기성 등록', createProgressClaim, undefined)}
                    deleteProgressClaim={userAction('기성 삭제', deleteProgressClaim, undefined)}
                    requestPaymentApprovals={userAction('지급결재 요청', requestPaymentApprovals, undefined)}
                  />
                )}
                {activeTab === '현장 작업일지' && can('SITE_READ') && selectedProjectId && (
                  <DailyLogPanel key={selectedProjectId} project={selectedProject} projectId={selectedProjectId} />
                )}
                {activeTab === '현장 회의록' && can('SITE_READ') && selectedProjectId && (
                  <MeetingMinutesPanel key={selectedProjectId} project={selectedProject} projectId={selectedProjectId} />
                )}
                {activeTab === '준공정산' && can('SETTLEMENT_READ') && settlement && (
                  <WriteScope permission="SETTLEMENT_REQUEST"><SettlementPanel
                    settlement={settlement}
                    form={settlementForm}
                    setForm={setSettlementForm}
                    requestSettlementApproval={userAction('준공정산 결재 요청', requestSettlementApproval, undefined)}
                  /></WriteScope>
                )}
                {activeTab !== '고객 견적·계약 관리' && activeTab !== '세부실행예산' && activeTab !== '업체 비교견적' && activeTab !== '계약/발주 이력' && activeTab !== '기성 및 지급 현황' && activeTab !== '현장 작업일지' && activeTab !== '현장 회의록' && activeTab !== '준공정산' && (
                  <div className="rounded-md border border-dashed border-slate-300 bg-slate-50 p-8 text-center text-sm text-slate-500">
                    {activeTab} 상세 목록은 다음 단계 API에서 확장 예정입니다. 현재 계산값은 상단 통합현황에 반영되어 있습니다.
                  </div>
                )}
              </div>
            </section>
          </>
        ) : null}
          </>
        )}

        {activePage === 'approvals' && can('APPROVAL_READ') && (
          <section className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
            <ApprovalInboxPanel
              approvals={visibleApprovals}
              selectedApproval={selectedApproval?.approval_id === selectedApprovalId && matchesApproval(selectedApproval, { projectId: approvalProjectFilter, status: approvalStatusFilter, type: approvalTypeFilter }) ? selectedApproval : null}
              projects={approvalProjectChoices}
              projectFilter={approvalProjectFilter}
              setProjectFilter={projectId => { void changeApprovalProject(projectId) }}
              totalCount={statusApprovals.length}
              statusFilter={approvalStatusFilter}
              typeFilter={approvalTypeFilter}
              rejectComment={rejectComment}
              setStatusFilter={setApprovalStatusFilter}
              setTypeFilter={setApprovalTypeFilter}
              setRejectComment={setRejectComment}
              loadApprovalDetail={loadApprovalDetail}
              approveApproval={userAction('결재 승인', (approvalId: string) => decideApproval(approvalId, 'approve'), undefined)}
              rejectApproval={userAction('결재 반려', (approvalId: string) => decideApproval(approvalId, 'reject'), undefined)}
              cancelApproval={userAction('결재 승인취소', (approvalId: string) => decideApproval(approvalId, 'cancel'), undefined)}
            />
          </section>
        )}

        {activePage === 'executive' && can('DASHBOARD_READ') && <ExecutiveDashboard />}

        {activePage === 'changes' && can('CHANGE_READ') && (
          <section className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
            <div className="mb-6 flex flex-col gap-3 border-b border-slate-200 pb-4 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <h2 className="text-lg font-semibold text-slate-950">프로젝트별 변경관리 이력</h2>
                <p className="mt-1 text-sm text-slate-500">조회할 프로젝트를 선택하면 해당 프로젝트의 계약 변경 차수와 관련 이력만 표시합니다.</p>
              </div>
              <label className="w-full text-sm font-semibold text-slate-700 sm:w-80">
                조회 프로젝트
                <select
                  aria-label="변경관리 이력 조회 프로젝트"
                  className="input mt-2 w-full"
                  value={selectedProjectId ?? ''}
                  onChange={event => { void selectProject(event.target.value || null) }}
                >
                  <option value="">프로젝트를 선택하세요</option>
                  {projects.map((project) => (
                    <option key={project.project_id} value={project.project_id}>
                      {project.project_code} · {project.project_name}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            {selectedProjectId ? (
              <div className="space-y-8">
                <RevisionManagementPanel key={selectedProjectId} projectId={selectedProjectId} projectCode={selectedProject?.project_code} projectName={selectedProject?.project_name} />
              </div>
            ) : (
              <div className="rounded-md border border-dashed border-slate-300 bg-slate-50 p-8 text-center text-sm text-slate-500">
                조회할 프로젝트를 선택하면 해당 프로젝트의 변경관리 이력을 확인할 수 있습니다.
              </div>
            )}
          </section>
        )}
      </div>

      {isModalOpen && (
        <ProjectCreationModal
          form={form}
          setForm={setForm}
          onClose={() => setIsModalOpen(false)}
          onSubmit={submitProject}
        />
      )}
    </main></DraftPermissions>
  )
}

export default App
