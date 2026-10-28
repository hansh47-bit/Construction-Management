import { useEffect, useMemo, useRef, useState } from 'react'
import type { ApprovalItem, CustomerContract, CustomerContractItem, ExecutionBudgetDetail, ExecutionBudgetDetailResponse, ExecutionBudgetSummaryItem, IntegratedSummary, ProgressPaymentResponse, ProjectSettlement, PurchaseOrderDraft, PurchaseOrderHistoryItem, PurchaseOrderHistoryResponse, VendorComparisonItem, VendorComparisonResponse } from './types/domain'
import { approvalApi } from './features/approvals/api'
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
  const [activePage, setActivePage] = useState<'projects' | 'executive' | 'approvals' | 'changes'>('projects')
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null)
  const [summary, setSummary] = useState<IntegratedSummary | null>(null)
  const [contract, setContract] = useState<CustomerContract | null>(null)
  const [contractItems, setContractItems] = useState<CustomerContractItem[]>([])
  const [executionBudget, setExecutionBudget] = useState<ExecutionBudgetDetailResponse | null>(null)
  const [executionDetails, setExecutionDetails] = useState<ExecutionBudgetDetail[]>([])
  const [executionBudgetApprovalFiles, setExecutionBudgetApprovalFiles] = useState<File[]>([])
  const [selectedExecutionItemId, setSelectedExecutionItemId] = useState('EX-001')
  const [revisionProcurementItemIds, setRevisionProcurementItemIds] = useState<string[] | null>(null)
  const [vendorComparison, setVendorComparison] = useState<VendorComparisonResponse | null>(null)
  const [vendorComparisons, setVendorComparisons] = useState<VendorComparisonItem[]>([])
  const [purchaseOrderDraft, setPurchaseOrderDraft] = useState<PurchaseOrderDraft | null>(null)
  const [purchaseOrderHistory, setPurchaseOrderHistory] = useState<PurchaseOrderHistoryItem[]>([])
  const [purchaseOrderHistoryRevision, setPurchaseOrderHistoryRevision] = useState<number | undefined>()
  const [approvals, setApprovals] = useState<ApprovalItem[]>([])
  const [pendingApprovalCount, setPendingApprovalCount] = useState(0)
  const [selectedApproval, setSelectedApproval] = useState<ApprovalItem | null>(null)
  const [approvalStatusFilter, setApprovalStatusFilter] = useState('REQUESTED')
  const [approvalTypeFilter, setApprovalTypeFilter] = useState('')
  const [rejectComment, setRejectComment] = useState('')
  const [progressPayment, setProgressPayment] = useState<ProgressPaymentResponse | null>(null)
  const [settlement, setSettlement] = useState<ProjectSettlement | null>(null)
  const [selectedPoId, setSelectedPoId] = useState('')
  const [claimForm, setClaimForm] = useState({ degree: '1', claim_amount: '0' })
  const [selectedPaymentPoIds, setSelectedPaymentPoIds] = useState<string[]>([])
  const [settlementForm, setSettlementForm] = useState({ uncollected_receivable: '0', settlement_site_expense: '0', settlement_labor_expense: '0', settlement_tax: '0', settlement_notes: '', force_complete_if_unpaid: false, unpaid_handling_reason: '' })
  const [targetProfitRate, setTargetProfitRate] = useState('0')
  const [selectedFileType, setSelectedFileType] = useState('CONTRACT')
  const [activeTab, setActiveTab] = useState(tabs[0])
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const tabRef = useRef<HTMLDivElement | null>(null)
  const { projects, keyword, setKeyword, isModalOpen, setIsModalOpen, form, setForm, refreshProjects, submitProject, deleteProject } = useProjects({
    onError: setError,
    onDeleted: (projectId) => { if (selectedProjectId === projectId) setSelectedProjectId(null) },
  })

  useEffect(() => {
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
      try {
        setIsLoading(true)
        const response = await projectApi.integratedSummary(selectedProjectId!, controller.signal)
        if (!response.ok) {
          throw new Error(`통합현황 조회 실패: ${response.status}`)
        }
        setSummary(await response.json())
        setError(null)
      } catch (err) {
        if (err instanceof DOMException && err.name === 'AbortError') {
          return
        }
        setError(err instanceof Error ? err.message : '통합현황을 불러오지 못했습니다.')
      } finally {
        setIsLoading(false)
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

  const selectedProject = useMemo(
    () => projects.find((project) => project.project_id === selectedProjectId) ?? summary?.project_info,
    [projects, selectedProjectId, summary],
  )

  async function startConstruction() {
    if (!selectedProjectId || !selectedProject) return
    if (!window.confirm(`“${selectedProject.project_name}” 프로젝트의 공사를 시작하시겠습니까? 상태가 공사진행으로 변경됩니다.`)) {
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
    const summaryResponse = await projectApi.integratedSummary(selectedProjectId)
    if (summaryResponse.ok) setSummary(await summaryResponse.json())
    setError(null)
  }

  async function synchronizePreparationStatus() {
    if (!selectedProjectId || !selectedProject) return
    if (!window.confirm(`“${selectedProject.project_name}” 프로젝트의 승인된 실행예산을 반영해 상태를 공사준비로 변경하시겠습니까?`)) {
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
    const summaryResponse = await projectApi.integratedSummary(selectedProjectId)
    if (summaryResponse.ok) setSummary(await summaryResponse.json())
    setError(null)
  }

  async function loadCustomerContract(projectId: string) {
    try {
      const response = await contractApi.get(projectId)
      if (!response.ok) {
        throw new Error(`고객 계약 조회 실패: ${response.status}`)
      }
      const data: CustomerContract = await response.json()
      setContract(data)
      setTargetProfitRate(String(data.target_profit_rate ?? 0))
      setContractItems(data.items.length > 0 ? data.items : [newContractItem()])
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : '고객 계약 정보를 불러오지 못했습니다.')
    }
  }

  async function saveCustomerContract(): Promise<boolean> {
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
    setContract(data)
    setContractItems(data.items.length > 0 ? data.items : [newContractItem()])
    setTargetProfitRate(String(data.target_profit_rate ?? 0))
    await refreshProjects(keyword)
    if (selectedProjectId) {
      const summaryResponse = await projectApi.integratedSummary(selectedProjectId)
      if (summaryResponse.ok) {
        setSummary(await summaryResponse.json())
      }
      await loadSettlement(selectedProjectId)
    }
    return true
  }

  async function uploadContractFile(file: File | null) {
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
    if (!selectedProjectId) {
      return
    }

    const response = await contractApi.deleteFile(selectedProjectId, fileId)

    if (!response.ok) {
      setError(`계약 파일 삭제 실패: ${response.status}`)
      return
    }

    const data: CustomerContract = await response.json()
    setContract(data)
    setError(null)
  }

  async function loadExecutionBudgetDetails(projectId: string) {
    try {
      const [response, procurementResponse] = await Promise.all([executionBudgetApi.get(projectId), revisionApi.procurementItems(projectId)])
      if (!response.ok) {
        throw new Error(`세부실행예산 조회 실패: ${response.status}`)
      }
      const data: ExecutionBudgetDetailResponse = await response.json()
      let eligibleItemIds: string[] | null = null
      if (procurementResponse.ok) {
        const procurement: { has_approved_revision: boolean; item_ids: string[] } = await procurementResponse.json()
        eligibleItemIds = procurement.has_approved_revision ? procurement.item_ids : null
        setRevisionProcurementItemIds(eligibleItemIds)
      }
      setExecutionBudget(data)
      setExecutionDetails(data.details.length > 0 ? data.details : [newExecutionBudgetDetail(data.project_code)])
      const eligibleItems = data.summary_by_item.filter((item) => item.approval_status === 'APPROVED' && item.procurement_required && (eligibleItemIds === null || eligibleItemIds.includes(item.budget_item_id)))
      if (eligibleItems.length > 0) {
        setSelectedExecutionItemId((current) => eligibleItems.some((item) => item.budget_item_id === current) ? current : eligibleItems[0].budget_item_id)
      } else {
        setSelectedExecutionItemId('')
      }
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : '세부실행예산을 불러오지 못했습니다.')
    }
  }

  async function saveExecutionBudgetDetails() {
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
    await loadSettlement(selectedProjectId)
  }

  async function requestExecutionBudgetApproval() {
    if (!selectedProjectId) return
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
      JSON.stringify({ request_user: 'system', request_reason: '최초 실행예산 승인 요청' }),
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
    void loadPendingApprovalCount()
    if (!selectedProjectId && activePage !== 'approvals') return
    const params = new URLSearchParams()
    if (selectedProjectId && activePage !== 'approvals') params.set('projectId', selectedProjectId)
    if (approvalStatusFilter) params.set('status', approvalStatusFilter)
    if (approvalTypeFilter) params.set('type', approvalTypeFilter)
    const response = await approvalApi.list(params)
    if (!response.ok) {
      setError(`통합결재함 조회 실패: ${response.status}`)
      return
    }
    const data: ApprovalItem[] = await response.json()
    setApprovals(data)
    setSelectedApproval((current) => current ? data.find((approval) => approval.approval_id === current.approval_id) ?? null : null)
  }

  async function loadApprovalDetail(approvalId: string) {
    const response = await approvalApi.detail(approvalId)
    if (!response.ok) {
      setError(`결재 상세 조회 실패: ${response.status}`)
      return
    }
    setSelectedApproval(await response.json())
  }

  async function decideApproval(approvalId: string, decision: 'approve' | 'reject' | 'cancel') {
    if (decision === 'cancel' && !window.confirm('승인 완료된 실행예산을 승인취소하고 재작업 상태로 전환할까요? 기존 승인 이력은 보존됩니다.')) {
      return
    }
    const response = await approvalApi.decide(approvalId, decision, { approver: '대표', comment: decision === 'approve' ? '승인' : rejectComment })
    if (!response.ok) {
      setError(`결재 처리 실패: ${response.status}`)
      return
    }
    setRejectComment('')
    const decidedApproval: ApprovalItem = await response.json()
    setSelectedApproval(decidedApproval)
    await loadApprovals()
    await refreshProjects(keyword)
    if (selectedProjectId) {
      await loadExecutionBudgetDetails(selectedProjectId)
      if (selectedExecutionItemId) await loadVendorComparisons(selectedProjectId, selectedExecutionItemId)
      const summaryResponse = await projectApi.integratedSummary(selectedProjectId)
      if (summaryResponse.ok) setSummary(await summaryResponse.json())
      const progress = await loadProgressPayments(selectedProjectId)
      await loadPurchaseOrders(selectedProjectId)
      await loadSettlement(selectedProjectId)
      if (decision === 'approve' && decidedApproval.approval_type === 'PAYMENT' && decidedApproval.project_id === selectedProjectId && progress?.all_payments_approved) {
        setActivePage('projects')
        moveToTab('준공정산')
      }
    }
    if (decision === 'approve' && decidedApproval.approval_type === 'PAYMENT' && decidedApproval.project_id !== selectedProjectId) {
      const paymentProjectProgress = await loadProgressPayments(decidedApproval.project_id)
      if (paymentProjectProgress?.all_payments_approved) {
        setSelectedProjectId(decidedApproval.project_id)
        setActivePage('projects')
        moveToTab('준공정산')
      }
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

  async function loadVendorComparisons(projectId: string, itemId: string) {
    try {
      const response = await procurementApi.comparisons(projectId, itemId)
      if (!response.ok) {
        throw new Error(`업체 비교견적 조회 실패: ${response.status}`)
      }
      const data: VendorComparisonResponse = await response.json()
      setVendorComparison(data)
      setVendorComparisons(data.comparisons.length > 0 ? data.comparisons : [newVendorComparison(itemId, 'A'), newVendorComparison(itemId, 'B')])
      await loadPurchaseOrderDraft(projectId, itemId, false)
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : '업체 비교견적을 불러오지 못했습니다.')
    }
  }

  async function loadPurchaseOrderDraft(projectId: string, itemId: string, showError = true) {
    const response = await procurementApi.draft(projectId, itemId)
    if (!response.ok) {
      setPurchaseOrderDraft(null)
      if (showError) setError(`발주품의 초안 조회 실패: ${response.status}`)
      return
    }
    setPurchaseOrderDraft(await response.json())
    await loadSettlement(projectId)
  }

  async function loadPurchaseOrders(projectId: string) {
    const response = await procurementApi.orders(projectId)
    if (!response.ok) {
      setError(`발주 이력 조회 실패: ${response.status}`)
      return
    }
    const data: PurchaseOrderHistoryResponse = await response.json()
    setPurchaseOrderHistory(data.items)
    setPurchaseOrderHistoryRevision(data.revision_number)
  }

  async function persistVendorComparisons(): Promise<VendorComparisonResponse | null> {
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
    })
    if (!response.ok) {
      setError(`업체 비교견적 저장 실패: ${response.status}`)
      return null
    }
    const data: VendorComparisonResponse = await response.json()
    setVendorComparison(data)
    setVendorComparisons(data.comparisons.length > 0 ? data.comparisons : [newVendorComparison(selectedExecutionItemId, 'A')])
    await loadPurchaseOrderDraft(selectedProjectId, selectedExecutionItemId, false)
    return data
  }

  async function loadPendingApprovalCount() {
    try {
      const response = await approvalApi.list(new URLSearchParams({ status: 'REQUESTED' }))
      if (!response.ok) return
      const data: ApprovalItem[] = await response.json()
      setPendingApprovalCount(data.length)
    } catch {
      // Navigation stays usable even when the notification count cannot be refreshed.
    }
  }

  async function saveVendorComparisons() {
    await persistVendorComparisons()
  }

  async function requestPurchaseOrder() {
    if (!selectedProjectId || !selectedExecutionItemId) return
    const selected = vendorComparisons.find((comparison) => comparison.is_selected)
    if (!selected || !selected.vendor_name.trim() || selected.quoted_amount <= 0 || !selected.selection_reason?.trim()) {
      setError('발주품의를 요청하려면 업체명, 견적금액, 선정 업체, 선정 사유를 모두 입력해야 합니다.')
      return
    }
    const saved = await persistVendorComparisons()
    if (!saved) return
    const response = await procurementApi.requestDraft(selectedProjectId, selectedExecutionItemId)
    if (!response.ok) {
      setError(`발주품의 요청 실패: ${response.status}`)
      return
    }
    setPurchaseOrderDraft(await response.json())
    await loadExecutionBudgetDetails(selectedProjectId)
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
    if (!selectedProjectId || !selectedExecutionItemId) return
    const saved = await persistVendorComparisons()
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
    try {
      const response = await paymentApi.status(projectId)
      if (!response.ok) throw new Error(`기성/지급 현황 조회 실패: ${response.status}`)
      const data: ProgressPaymentResponse = await response.json()
      setProgressPayment(data)
      if (!selectedPoId && data.purchaseOrders.length > 0) {
        const firstPurchaseOrder = data.purchaseOrders[0]
        setSelectedPoId(firstPurchaseOrder.po_id)
        setClaimForm({ degree: '1', claim_amount: String(firstPurchaseOrder.current_po_amount) })
      }
      setError(null)
      return data
    } catch (err) {
      setError(err instanceof Error ? err.message : '기성/지급 현황을 불러오지 못했습니다.')
      return null
    }
  }

  async function loadSettlement(projectId: string) {
    try {
      const response = await settlementApi.get(projectId)
      if (!response.ok) throw new Error(`준공정산 조회 실패: ${response.status}`)
      const data: ProjectSettlement = await response.json()
      setSettlement(data)
      setSettlementForm((form) => ({
        ...form,
        uncollected_receivable: String(data.payments.uncollected_receivable ?? 0),
        settlement_site_expense: String(data.settlement_site_expense ?? 0),
        settlement_labor_expense: String(data.settlement_labor_expense ?? 0),
        settlement_tax: String(data.settlement_tax ?? 0),
        settlement_notes: data.settlement_notes ?? form.settlement_notes,
        unpaid_handling_reason: data.unpaid_handling_reason ?? form.unpaid_handling_reason,
      }))
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : '준공정산 정보를 불러오지 못했습니다.')
    }
  }

  async function requestSettlementApproval() {
    if (!selectedProjectId) return
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
    await refreshProjects(keyword)
    await loadApprovals()
    const summaryResponse = await projectApi.integratedSummary(selectedProjectId)
    if (summaryResponse.ok) {
      setSummary(await summaryResponse.json())
    }
  }

  async function createProgressClaim() {
    if (!selectedProjectId || !selectedPoId) return
    const response = await paymentApi.createClaim(selectedProjectId, { po_id: selectedPoId, degree: Number(claimForm.degree || 0), claim_amount: toNumber(claimForm.claim_amount) })
    if (!response.ok) {
      setError(`기성 작성 실패: ${response.status}`)
      return
    }
    const data: ProgressPaymentResponse = await response.json()
    setProgressPayment(data)
    setClaimForm({ degree: '1', claim_amount: '0' })
    await loadSettlement(selectedProjectId)
  }

  async function deleteProgressClaim(claimId: string) {
    if (!selectedProjectId || !window.confirm('결재 대기 또는 반려된 기성 건을 삭제하시겠습니까? 연결된 결재 요청도 함께 삭제됩니다.')) return
    const response = await paymentApi.deleteClaim(selectedProjectId, claimId)
    if (!response.ok) {
      setError(`기성 삭제 실패: ${response.status}`)
      return
    }
    const data: ProgressPaymentResponse = await response.json()
    setProgressPayment(data)
    await Promise.all([loadSettlement(selectedProjectId), loadApprovals()])
  }

  async function requestPaymentApprovals() {
    if (!selectedProjectId || selectedPaymentPoIds.length === 0) return
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
    <main className="min-h-screen bg-[#f5f7fb] text-slate-900">
      <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 shadow-sm backdrop-blur">
        <div className="mx-auto w-full max-w-[1440px] px-4 sm:px-6 lg:px-8">
          <div className="flex min-h-16 items-center justify-between gap-4">
            <button type="button" onClick={() => setActivePage('projects')} className="text-left" aria-label="프로젝트 현황으로">
              <p className="text-lg font-black tracking-tight text-slate-950">아이엔씨 <span className="text-blue-700">ERP</span></p>
              <p className="text-[11px] font-medium text-slate-400">공사 통합관리</p>
            </button>
            <div className="flex items-center gap-3">
              {activePage === 'projects' && <button type="button" onClick={() => setIsModalOpen(true)} className="inline-flex items-center justify-center gap-2 rounded-lg bg-blue-700 px-3.5 py-2 text-sm font-bold text-white shadow-sm hover:bg-blue-800"><FolderPlus className="h-4 w-4" aria-hidden="true" />프로젝트 등록</button>}
              <span className="hidden rounded-full bg-blue-50 px-3 py-1.5 text-xs font-bold text-blue-700 sm:inline">대표이사</span>
            </div>
          </div>
          <nav className="-mb-px flex items-end gap-1 overflow-x-auto" aria-label="최상위 메뉴">
            {([
              ['executive', '경영진 대시보드'],
              ['projects', '프로젝트 현황'],
              ['approvals', '통합결재함'],
              ['changes', '변경관리 이력'],
            ] as const).map(([page, label]) => (
              <button key={page} type="button" onClick={() => setActivePage(page)} className={`relative shrink-0 border-b-2 px-4 py-3 text-sm font-bold transition ${activePage === page ? 'border-blue-600 text-blue-700' : 'border-transparent text-slate-500 hover:border-slate-300 hover:text-slate-800'}`}>
                {label}{page === 'approvals' && pendingApprovalCount > 0 && <span className="ml-1.5 inline-flex min-w-5 items-center justify-center rounded-full bg-rose-500 px-1.5 py-0.5 text-[10px] font-black text-white">{pendingApprovalCount}</span>}
              </button>
            ))}
          </nav>
        </div>
      </header>

      <div className="mx-auto flex w-full max-w-[1440px] flex-col gap-6 px-4 py-6 sm:px-6 lg:px-8">

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
            onSelect={(projectId) => {
              setActiveTab('고객 견적·계약 관리')
              setSelectedProjectId(projectId)
            }}
            onDelete={deleteProject}
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
              onBack={() => setSelectedProjectId(null)}
              onStartConstruction={startConstruction}
              onSynchronizePreparation={synchronizePreparationStatus}
              onMoveToTab={moveToTab}
            />

            <section ref={tabRef} className="scroll-mt-36">
              <div className="flex gap-1 overflow-x-auto border-b border-slate-200">
                {projectTabs.map((tab) => (
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
                {activeTab === '고객 견적·계약 관리' && contract && (
                  <ContractManagementPanel
                    contract={contract}
                    contractItems={contractItems}
                    contractTotal={contractTotal}
                    targetProfitRate={targetProfitRate}
                    selectedFileType={selectedFileType}
                    setTargetProfitRate={setTargetProfitRate}
                    setSelectedFileType={setSelectedFileType}
                    saveCustomerContract={saveCustomerContract}
                    openChangeManagement={() => setActivePage('changes')}
                    uploadContractFile={uploadContractFile}
                    deleteContractFile={deleteContractFile}
                    updateContractItem={updateContractItem}
                    addContractItem={addContractItem}
                    removeContractItem={removeContractItem}
                  />
                )}
                {activeTab === '세부실행예산' && executionBudget && (
                  <ExecutionBudgetDetailPanel
                    executionBudget={executionBudget}
                    details={executionDetails}
                    summary={executionSummary}
                    totalAmount={executionTotal}
                    saveExecutionBudgetDetails={saveExecutionBudgetDetails}
                    requestExecutionBudgetApproval={requestExecutionBudgetApproval}
                    approvalFiles={executionBudgetApprovalFiles}
                    setApprovalFiles={setExecutionBudgetApprovalFiles}
                    contractFiles={contract?.files ?? []}
                    updateExecutionDetail={updateExecutionDetail}
                    addExecutionDetail={addExecutionDetail}
                    removeExecutionDetail={removeExecutionDetail}
                  />
                )}
                {activeTab === '업체 비교견적' && executionBudget && vendorComparison && (
                  <VendorComparisonPanel
                    executionBudget={executionBudget}
                    revisionProcurementItemIds={revisionProcurementItemIds}
                    selectedExecutionItemId={selectedExecutionItemId}
                    setSelectedExecutionItemId={setSelectedExecutionItemId}
                    vendorComparison={vendorComparison}
                    comparisons={vendorComparisons}
                    purchaseOrderDraft={purchaseOrderDraft}
                    saveVendorComparisons={saveVendorComparisons}
                    requestPurchaseOrder={requestPurchaseOrder}
                    updateVendorComparison={updateVendorComparison}
                    addVendorComparison={addVendorComparison}
                    removeVendorComparison={removeVendorComparison}
                    uploadVendorComparisonFile={uploadVendorComparisonFile}
                    openPurchaseOrderHistory={() => moveToTab('계약/발주 이력')}
                  />
                )}
                {activeTab === '계약/발주 이력' && (
                  <PurchaseOrderHistoryPanel purchaseOrders={purchaseOrderHistory} revisionNumber={purchaseOrderHistoryRevision} />
                )}
                {activeTab === '기성 및 지급 현황' && progressPayment && (
                  <ProgressPaymentPanel
                    progressPayment={progressPayment}
                    selectedPoId={selectedPoId}
                    setSelectedPoId={(poId) => {
                      setSelectedPoId(poId)
                      const purchaseOrder = progressPayment.purchaseOrders.find((item) => item.po_id === poId)
                      setClaimForm({ degree: '1', claim_amount: String(purchaseOrder?.current_po_amount ?? 0) })
                    }}
                    claimForm={claimForm}
                    setClaimForm={setClaimForm}
                    selectedPaymentPoIds={selectedPaymentPoIds}
                    setSelectedPaymentPoIds={setSelectedPaymentPoIds}
                    createProgressClaim={createProgressClaim}
                    deleteProgressClaim={deleteProgressClaim}
                    requestPaymentApprovals={requestPaymentApprovals}
                  />
                )}
                {activeTab === '현장 작업일지' && selectedProjectId && (
                  <DailyLogPanel project={selectedProject} projectId={selectedProjectId} />
                )}
                {activeTab === '현장 회의록' && selectedProjectId && (
                  <MeetingMinutesPanel project={selectedProject} projectId={selectedProjectId} />
                )}
                {activeTab === '준공정산' && settlement && (
                  <SettlementPanel
                    settlement={settlement}
                    form={settlementForm}
                    setForm={setSettlementForm}
                    requestSettlementApproval={requestSettlementApproval}
                  />
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

        {activePage === 'approvals' && (
          <section className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
            <ApprovalInboxPanel
              approvals={approvals}
              selectedApproval={selectedApproval}
              statusFilter={approvalStatusFilter}
              typeFilter={approvalTypeFilter}
              rejectComment={rejectComment}
              setStatusFilter={setApprovalStatusFilter}
              setTypeFilter={setApprovalTypeFilter}
              setRejectComment={setRejectComment}
              loadApprovalDetail={loadApprovalDetail}
              approveApproval={(approvalId) => decideApproval(approvalId, 'approve')}
              rejectApproval={(approvalId) => decideApproval(approvalId, 'reject')}
              cancelApproval={(approvalId) => decideApproval(approvalId, 'cancel')}
            />
          </section>
        )}

        {activePage === 'executive' && <ExecutiveDashboard />}

        {activePage === 'changes' && (
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
                  onChange={(event) => setSelectedProjectId(event.target.value || null)}
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
                <RevisionManagementPanel projectId={selectedProjectId} projectCode={selectedProject?.project_code} projectName={selectedProject?.project_name} />
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
    </main>
  )
}

export default App
