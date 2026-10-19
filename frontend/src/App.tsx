import { useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import type { ApprovalItem, ChangeOrder, CustomerContract, CustomerContractItem, ExecutionBudgetDetail, ExecutionBudgetDetailResponse, ExecutionBudgetSummaryItem, IntegratedSummary, ProgressPaymentResponse, Project, ProjectForm, ProjectSettlement, PurchaseOrderDraft, PurchaseOrderHistoryItem, PurchaseOrderHistoryResponse, VendorComparisonItem, VendorComparisonResponse } from './types/domain'
import { approvalApi } from './features/approvals/api'
import { getChangeOrders } from './features/changes/api'
import { contractApi } from './features/contracts/api'
import { executionBudgetApi } from './features/execution-budget/api'
import { paymentApi } from './features/payments/api'
import { procurementApi } from './features/procurement/api'
import { projectApi } from './features/projects/api'
import { settlementApi } from './features/settlement/api'
import { SubMetric } from './components/ui/Metrics'
import { ProjectCreationModal } from './features/projects/ProjectCreationModal'
import { ProjectList } from './features/projects/ProjectList'
import { ApprovalInboxPanel } from './features/approvals/ApprovalInboxPanel'
import { ChangeHistoryPanel } from './features/changes/ChangeHistoryPanel'
import { CostBreakdownTable } from './features/dashboard/CostBreakdownTable'
import { ContractManagementPanel } from './features/contracts/ContractManagementPanel'
import { ExecutionBudgetDetailPanel } from './features/execution-budget/ExecutionBudgetDetailPanel'
import { ProgressPaymentPanel } from './features/payments/ProgressPaymentPanel'
import { PurchaseOrderHistoryPanel } from './features/procurement/PurchaseOrderHistoryPanel'
import { VendorComparisonPanel } from './features/procurement/VendorComparisonPanel'
import { SettlementPanel } from './features/settlement/SettlementPanel'
import { formatWon, toNumber } from './lib/format'
import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Banknote,
  BriefcaseBusiness,
  CheckCircle2,
  ClipboardList,
  FileClock,
  FolderPlus,
  MapPin,
} from 'lucide-react'

const tabs = ['고객 견적·계약 관리', '세부실행예산', '업체 비교견적', '통합결재함', '공종별 흐름 추적', '계약/발주 이력', '기성 및 지급 현황', '준공정산', '증빙 및 파일']
const projectTabs = tabs.filter((tab) => tab !== '통합결재함')

const initialForm: ProjectForm = {
  project_name: '',
  client_name: '',
  site_address: '',
  contract_date: '',
  start_date: '',
  end_date: '',
  team_leader_id: '',
  manager_id: '',
  target_profit_rate: '0',
  note: '',
}



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
    budget_item_id: 'EX-001',
    category_name: '인테리어공사',
    item_name: '철거/폐기물',
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
  const [activePage, setActivePage] = useState<'projects' | 'approvals' | 'changes'>('projects')
  const [projects, setProjects] = useState<Project[]>([])
  const [keyword, setKeyword] = useState('')
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null)
  const [summary, setSummary] = useState<IntegratedSummary | null>(null)
  const [contract, setContract] = useState<CustomerContract | null>(null)
  const [contractItems, setContractItems] = useState<CustomerContractItem[]>([])
  const [executionBudget, setExecutionBudget] = useState<ExecutionBudgetDetailResponse | null>(null)
  const [executionDetails, setExecutionDetails] = useState<ExecutionBudgetDetail[]>([])
  const [executionBudgetApprovalFiles, setExecutionBudgetApprovalFiles] = useState<File[]>([])
  const [selectedExecutionItemId, setSelectedExecutionItemId] = useState('EX-001')
  const [vendorComparison, setVendorComparison] = useState<VendorComparisonResponse | null>(null)
  const [vendorComparisons, setVendorComparisons] = useState<VendorComparisonItem[]>([])
  const [purchaseOrderDraft, setPurchaseOrderDraft] = useState<PurchaseOrderDraft | null>(null)
  const [purchaseOrderHistory, setPurchaseOrderHistory] = useState<PurchaseOrderHistoryItem[]>([])
  const [changeOrders, setChangeOrders] = useState<ChangeOrder[]>([])
  const [approvals, setApprovals] = useState<ApprovalItem[]>([])
  const [selectedApproval, setSelectedApproval] = useState<ApprovalItem | null>(null)
  const [approvalStatusFilter, setApprovalStatusFilter] = useState('REQUESTED')
  const [approvalTypeFilter, setApprovalTypeFilter] = useState('')
  const [rejectComment, setRejectComment] = useState('')
  const [progressPayment, setProgressPayment] = useState<ProgressPaymentResponse | null>(null)
  const [settlement, setSettlement] = useState<ProjectSettlement | null>(null)
  const [selectedPoId, setSelectedPoId] = useState('')
  const [claimForm, setClaimForm] = useState({ degree: '1', claim_amount: '0' })
  const [selectedPaymentPoIds, setSelectedPaymentPoIds] = useState<string[]>([])
  const [settlementForm, setSettlementForm] = useState({ uncollected_receivable: '0', settlement_notes: '', force_complete_if_unpaid: false, unpaid_handling_reason: '' })
  const [targetProfitRate, setTargetProfitRate] = useState('0')
  const [selectedFileType, setSelectedFileType] = useState('CONTRACT')
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [form, setForm] = useState<ProjectForm>(initialForm)
  const [activeTab, setActiveTab] = useState(tabs[0])
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const tabRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    loadProjects()
  }, [])

  useEffect(() => {
    const handle = window.setTimeout(() => loadProjects(keyword), 200)
    return () => window.clearTimeout(handle)
  }, [keyword])

  useEffect(() => {
    if (!selectedProjectId) {
      setSummary(null)
      setContract(null)
      setContractItems([])
      setExecutionBudget(null)
      setExecutionDetails([])
      setExecutionBudgetApprovalFiles([])
      setVendorComparison(null)
      setVendorComparisons([])
      setPurchaseOrderDraft(null)
      setPurchaseOrderHistory([])
      setChangeOrders([])
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
    loadChangeOrders(selectedProjectId)
    loadSettlement(selectedProjectId)
  }, [selectedProjectId])

  useEffect(() => {
    if (!selectedProjectId && activePage !== 'approvals') {
      return
    }
    loadApprovals()
  }, [approvalStatusFilter, approvalTypeFilter, activePage, selectedProjectId])

  const selectedProject = useMemo(
    () => projects.find((project) => project.project_id === selectedProjectId) ?? summary?.project_info,
    [projects, selectedProjectId, summary],
  )

  async function loadProjects(search = '') {
    try {
      const response = await projectApi.list(search)
      if (!response.ok) {
        throw new Error(`프로젝트 목록 조회 실패: ${response.status}`)
      }
      const data: Project[] = await response.json()
      setProjects(data)
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : '프로젝트 목록을 불러오지 못했습니다.')
      setIsLoading(false)
    }
  }

  async function submitProject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const payload = {
      ...form,
      target_profit_rate: Number(form.target_profit_rate || 0),
    }

    const response = await projectApi.create(payload)

    if (!response.ok) {
      setError(`프로젝트 등록 실패: ${response.status}`)
      return
    }

    await response.json()
    setForm(initialForm)
    setIsModalOpen(false)
    await loadProjects()
    setSelectedProjectId(null)
  }

  async function deleteProject(project: Project) {
    const confirmed = window.confirm(
      `“${project.project_name}” 프로젝트와 연결된 계약·예산·발주·기성·지급·결재·변경 데이터를 모두 영구 삭제할까요?\n정산완료 프로젝트만 삭제할 수 없습니다.`,
    )
    if (!confirmed) {
      return
    }

    const response = await projectApi.remove(project.project_id)

    if (!response.ok) {
      const body = await response.json().catch(() => null) as { detail?: string; message?: string } | null
      setError(body?.detail ?? body?.message ?? `프로젝트 삭제 실패: ${response.status}`)
      return
    }

    setError(null)
    if (selectedProjectId === project.project_id) {
      setSelectedProjectId(null)
    }
    await loadProjects(keyword)
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

  async function saveCustomerContract() {
    if (!selectedProjectId) {
      return
    }
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

    const response = await contractApi.save(selectedProjectId, payload)

    if (!response.ok) {
      setError(`고객 계약 저장 실패: ${response.status}`)
      return
    }

    const data: CustomerContract = await response.json()
    setContract(data)
    setContractItems(data.items.length > 0 ? data.items : [newContractItem()])
    setTargetProfitRate(String(data.target_profit_rate ?? 0))
    await loadProjects(keyword)
    if (selectedProjectId) {
      const summaryResponse = await projectApi.integratedSummary(selectedProjectId)
      if (summaryResponse.ok) {
        setSummary(await summaryResponse.json())
      }
      await loadSettlement(selectedProjectId)
    }
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
      const response = await executionBudgetApi.get(projectId)
      if (!response.ok) {
        throw new Error(`세부실행예산 조회 실패: ${response.status}`)
      }
      const data: ExecutionBudgetDetailResponse = await response.json()
      setExecutionBudget(data)
      setExecutionDetails(data.details.length > 0 ? data.details : [newExecutionBudgetDetail(data.project_code)])
      if (data.summary_by_item.length > 0) {
        setSelectedExecutionItemId((current) => current || data.summary_by_item[0].budget_item_id)
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

    const response = await executionBudgetApi.save(selectedProjectId, payload)
    if (!response.ok) {
      setError(`세부실행예산 저장 실패: ${response.status}`)
      return
    }

    const data: ExecutionBudgetDetailResponse = await response.json()
    setExecutionBudget(data)
    setExecutionDetails(data.details.length > 0 ? data.details : [newExecutionBudgetDetail(data.project_code)])
    await loadSettlement(selectedProjectId)
  }

  async function correctAmountByExecutive() {
    if (!selectedProjectId || !contract) return
    const targetText = window.prompt('변경할 계약 공급가액을 입력하세요. 변경 금액은 즉시 반영되며 원본은 유지됩니다.', String(contract.current_contract_supply_amount ?? contract.total_contract_supply_amount))
    if (targetText === null) return
    const targetAmount = Number(targetText.replaceAll(',', '').trim())
    if (!Number.isFinite(targetAmount) || targetAmount < 0) {
      setError('계약 공급가액을 0 이상의 숫자로 입력하세요.')
      return
    }
    const reason = window.prompt('변경 사유를 입력하세요.')
    if (!reason?.trim()) return
    const response = await contractApi.executiveCorrection(selectedProjectId, {
      target_contract_supply_amount: targetAmount,
      reason: reason.trim(),
      apply_execution_budget: true,
      apply_purchase_orders: true,
    })
    if (!response.ok) {
      const message = await response.text()
      setError(message || `대표 금액 정정 실패: ${response.status}`)
      return
    }
    await Promise.all([
      loadCustomerContract(selectedProjectId),
      loadExecutionBudgetDetails(selectedProjectId),
      loadPurchaseOrders(selectedProjectId),
      loadChangeOrders(selectedProjectId),
      loadProgressPayments(selectedProjectId),
      loadSettlement(selectedProjectId),
      loadProjects(keyword),
    ])
    const summaryResponse = await projectApi.integratedSummary(selectedProjectId)
    if (summaryResponse.ok) setSummary(await summaryResponse.json())
    setError(null)
  }

  async function loadChangeOrders(projectId: string) {
    const response = await getChangeOrders(projectId)
    if (response.ok) setChangeOrders(await response.json())
  }

  async function requestExecutionBudgetApproval() {
    if (!selectedProjectId) return
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
  }

  async function saveVendorComparisons() {
    if (!selectedProjectId || !selectedExecutionItemId) return
    const response = await procurementApi.saveComparisons(selectedProjectId, selectedExecutionItemId, {
      comparisons: vendorComparisons
        .filter((comparison) => comparison.vendor_name.trim())
        .map((comparison) => ({
          comparison_code: comparison.comparison_code,
          vendor_name: comparison.vendor_name,
          quoted_amount: comparison.quoted_amount,
          vat_type: comparison.vat_type,
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
      return
    }
    const data: VendorComparisonResponse = await response.json()
    setVendorComparison(data)
    setVendorComparisons(data.comparisons.length > 0 ? data.comparisons : [newVendorComparison(selectedExecutionItemId, 'A')])
    await loadPurchaseOrderDraft(selectedProjectId, selectedExecutionItemId, false)
  }

  async function requestPurchaseOrder() {
    if (!selectedProjectId || !selectedExecutionItemId) return
    const response = await procurementApi.requestDraft(selectedProjectId, selectedExecutionItemId)
    if (!response.ok) {
      setError(`발주품의 요청 실패: ${response.status}`)
      return
    }
    setPurchaseOrderDraft(await response.json())
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
        setSelectedPoId(data.purchaseOrders[0].po_id)
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
        settlement_notes: data.settlement_notes ?? form.settlement_notes,
        unpaid_handling_reason: data.unpaid_handling_reason ?? form.unpaid_handling_reason,
      }))
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : '준공정산 정보를 불러오지 못했습니다.')
    }
  }

  async function completeSettlement() {
    if (!selectedProjectId) return
    const response = await settlementApi.complete(selectedProjectId, {
      uncollected_receivable: toNumber(settlementForm.uncollected_receivable),
      settlement_notes: settlementForm.settlement_notes,
      force_complete_if_unpaid: settlementForm.force_complete_if_unpaid,
      unpaid_handling_reason: settlementForm.unpaid_handling_reason,
      settled_by: selectedProject?.manager_id ?? 'system',
    })
    if (!response.ok) {
      const message = await response.text()
      setError(message || `준공정산 완료 처리 실패: ${response.status}`)
      return
    }
    setSettlement(await response.json())
    await loadProjects(keyword)
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
    setClaimForm({ degree: String((Number(claimForm.degree || 0) || 0) + 1), claim_amount: '0' })
    await loadSettlement(selectedProjectId)
  }

  async function approveProgressClaim(claimId: string) {
    if (!selectedProjectId) return
    const response = await paymentApi.approveClaim(selectedProjectId, claimId)
    if (!response.ok) {
      setError(`기성 승인 실패: ${response.status}`)
      return
    }
    setProgressPayment(await response.json())
    await loadSettlement(selectedProjectId)
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

  const flowCards = summary
    ? [
        ['현재 계약금액', summary.flow_summary.current_contract_amount, '계약/발주 이력'],
        ['현재 실행예산', summary.flow_summary.current_execution_budget, '공종별 흐름 추적'],
        ['현재 발주금액', summary.flow_summary.current_purchase_amount, '계약/발주 이력'],
        ['누적 기성금액', summary.flow_summary.accumulated_completed_amount, '기성 및 지급 현황'],
        ['실제 지급금액', summary.flow_summary.actual_paid_amount, '기성 및 지급 현황'],
      ]
    : []

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
    const summary = new Map<string, ExecutionBudgetSummaryItem>()
    executionDetails.forEach((detail) => {
      const current = summary.get(detail.budget_item_id) ?? {
        budget_item_id: detail.budget_item_id,
        category_name: detail.category_name,
        item_name: detail.item_name,
		approval_status: executionBudget?.approval_status ?? 'DRAFT',
        rolled_up_amount: 0,
      }
      current.category_name = detail.category_name
      current.item_name = detail.item_name
      current.rolled_up_amount += (detail.quantity || 0) * (detail.unit_price || 0)
      summary.set(detail.budget_item_id, current)
    })
    return [...summary.values()].sort((a, b) => a.budget_item_id.localeCompare(b.budget_item_id))
	}, [executionBudget?.approval_status, executionDetails])

  return (
    <main className="min-h-screen bg-[#f5f7fb] text-slate-900">
      <div className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-4 py-6 sm:px-6 lg:px-8">
        <header className="flex flex-col gap-4 border-b border-slate-200 pb-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm font-semibold text-slate-500">Project Single View</p>
            <h1 className="mt-2 text-3xl font-semibold tracking-normal text-slate-950">
              {activePage === 'projects' ? '프로젝트 등록 및 통합현황' : activePage === 'approvals' ? '통합결재함' : '변경관리 이력'}
            </h1>
            <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600">
              {activePage === 'projects'
                ? '프로젝트를 선택하면 계약, 실행예산, 발주, 기성, 지급, 예상손익을 하나의 흐름으로 추적합니다.'
                : activePage === 'approvals'
                  ? '모든 프로젝트의 실행예산 및 발주품의 결재를 한 곳에서 처리합니다.'
                  : '선택한 프로젝트의 금액 변경 이력과 연결 영향을 확인합니다.'}
            </p>
            <div className="mt-4 flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => setActivePage('projects')}
                className={`rounded-md px-3 py-2 text-sm font-semibold ${activePage === 'projects' ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}
              >
                프로젝트 현황
              </button>
              <button
                type="button"
                onClick={() => setActivePage('approvals')}
                className={`rounded-md px-3 py-2 text-sm font-semibold ${activePage === 'approvals' ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}
              >
                통합결재함
              </button>
              <button
                type="button"
                onClick={() => setActivePage('changes')}
                className={`rounded-md px-3 py-2 text-sm font-semibold ${activePage === 'changes' ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}
              >
                변경관리 이력
              </button>
            </div>
          </div>
          {activePage === 'projects' && (
            <button
              type="button"
              onClick={() => setIsModalOpen(true)}
              className="inline-flex items-center justify-center gap-2 rounded-md bg-cyan-700 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-cyan-800"
            >
              <FolderPlus className="h-4 w-4" aria-hidden="true" />
              프로젝트 등록
            </button>
          )}
        </header>

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
        ) : (
          <section className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
              <div>
                <button
                  type="button"
                  onClick={() => setSelectedProjectId(null)}
                  className="mb-3 inline-flex items-center gap-2 rounded-md border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                >
                  <ArrowLeft className="h-4 w-4" aria-hidden="true" />
                  목록
                </button>
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="text-xl font-semibold text-slate-950">
                    {selectedProject?.project_name ?? '프로젝트'}
                  </h2>
                  {selectedProject?.status && (
                    <span className="rounded-full bg-cyan-100 px-2.5 py-1 text-xs font-semibold text-cyan-800">
                      {selectedProject.status}
                    </span>
                  )}
                </div>
                <p className="mt-2 text-sm text-slate-600">
                  {selectedProject?.project_code} · {selectedProject?.client_name ?? '발주처 미입력'}
                </p>
                <p className="mt-4 flex items-center gap-2 text-sm text-slate-600">
                  <MapPin className="h-4 w-4 text-slate-400" aria-hidden="true" />
                  {selectedProject?.site_address ?? '현장주소 미입력'}
                </p>
              </div>
              <div className="rounded-md border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600 lg:text-right">
                <p className="font-medium text-slate-800">목표 이익률 {selectedProject?.target_profit_rate ?? 0}%</p>
                <p className="mt-1">담당자 {selectedProject?.manager_id ?? '미지정'}</p>
                <p className="mt-1">팀장 {selectedProject?.team_leader_id ?? '미지정'}</p>
              </div>
            </div>
          </section>
        )}

        {isLoading && !summary ? (
          <section className="rounded-md border border-slate-200 bg-white p-8 text-center text-sm text-slate-500">
            통합현황을 불러오는 중입니다.
          </section>
        ) : summary ? (
          <>
            <section className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
              <div className="grid gap-3 xl:grid-cols-5">
                {flowCards.map(([label, value, targetTab], index) => (
                  <button
                    key={label}
                    type="button"
                    onClick={() => moveToTab(targetTab as string)}
                    className="group rounded-md border border-slate-200 bg-white p-4 text-left shadow-sm transition hover:border-cyan-300 hover:bg-cyan-50"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-sm font-medium text-slate-500">{label}</p>
                      {index < flowCards.length - 1 && <ArrowRight className="h-4 w-4 text-slate-300 group-hover:text-cyan-700" aria-hidden="true" />}
                    </div>
                    <p className="mt-3 text-xl font-semibold text-slate-950">{formatWon(value as number)}</p>
                  </button>
                ))}
              </div>
            </section>

            <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              <SubMetric label="목표 원가한도" value={formatWon(summary.profit_and_balance.target_cost_limit)} icon={<CheckCircle2 />} />
              <SubMetric label="예상 최종원가" value={formatWon(summary.profit_and_balance.expected_final_cost)} icon={<ClipboardList />} />
              <SubMetric
                label="예상 이익 / 이익률"
                value={`${formatWon(summary.profit_and_balance.expected_profit)} / ${summary.profit_and_balance.expected_profit_rate}%`}
                icon={<BriefcaseBusiness />}
                accent={summary.profit_and_balance.expected_profit >= 0 ? 'text-cyan-700' : 'text-rose-700'}
              />
              <SubMetric label="미발주 예정원가" value={formatWon(summary.profit_and_balance.unpurchased_budget_amount)} icon={<FileClock />} />
              <SubMetric label="미기성금액" value={formatWon(summary.profit_and_balance.unbilled_amount)} icon={<Banknote />} />
              <SubMetric label="승인 후 미지급" value={formatWon(summary.profit_and_balance.approved_unpaid_amount)} icon={<AlertTriangle />} />
              <SubMetric label="총 미지급 잔액" value={formatWon(summary.profit_and_balance.total_unpaid_balance)} icon={<Banknote />} />
            </section>

            <section ref={tabRef} className="rounded-md border border-slate-200 bg-white shadow-sm">
              <div className="flex flex-wrap gap-2 border-b border-slate-200 px-4 py-3">
                {projectTabs.map((tab) => (
                  <button
                    key={tab}
                    type="button"
                    onClick={() => setActiveTab(tab)}
                    className={`rounded-md px-3 py-2 text-sm font-semibold ${
                      activeTab === tab ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                  >
                    {tab}
                  </button>
                ))}
              </div>
              <div className="p-4">
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
                    correctAmountByExecutive={correctAmountByExecutive}
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
                    openPurchaseOrderHistory={() => moveToTab('계약/발주 이력')}
                  />
                )}
                {activeTab === '계약/발주 이력' && (
                  <PurchaseOrderHistoryPanel purchaseOrders={purchaseOrderHistory} />
                )}
                {activeTab === '기성 및 지급 현황' && progressPayment && (
                  <ProgressPaymentPanel
                    progressPayment={progressPayment}
                    selectedPoId={selectedPoId}
                    setSelectedPoId={setSelectedPoId}
                    claimForm={claimForm}
                    setClaimForm={setClaimForm}
                    selectedPaymentPoIds={selectedPaymentPoIds}
                    setSelectedPaymentPoIds={setSelectedPaymentPoIds}
                    createProgressClaim={createProgressClaim}
                    approveProgressClaim={approveProgressClaim}
                    requestPaymentApprovals={requestPaymentApprovals}
                  />
                )}
                {activeTab === '준공정산' && settlement && (
                  <SettlementPanel
                    settlement={settlement}
                    form={settlementForm}
                    setForm={setSettlementForm}
                    completeSettlement={completeSettlement}
                  />
                )}
                {activeTab === '공종별 흐름 추적' && <CostBreakdownTable rows={summary.cost_breakdown} />}
                {activeTab !== '고객 견적·계약 관리' && activeTab !== '세부실행예산' && activeTab !== '업체 비교견적' && activeTab !== '계약/발주 이력' && activeTab !== '기성 및 지급 현황' && activeTab !== '준공정산' && activeTab !== '공종별 흐름 추적' && (
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

        {activePage === 'changes' && (
          <section className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
            {selectedProjectId ? (
              <ChangeHistoryPanel changeOrders={changeOrders} />
            ) : (
              <div className="rounded-md border border-dashed border-slate-300 bg-slate-50 p-8 text-center text-sm text-slate-500">
                프로젝트 현황에서 프로젝트를 선택하면 해당 프로젝트의 변경관리 이력을 조회할 수 있습니다.
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
