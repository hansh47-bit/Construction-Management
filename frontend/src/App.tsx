import { useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Banknote,
  BriefcaseBusiness,
  CheckCircle2,
  ClipboardList,
  Download,
  FileClock,
  FolderPlus,
  MapPin,
  Plus,
  Save,
  Search,
  Trash2,
  Upload,
  X,
} from 'lucide-react'

type Project = {
  project_id: string
  project_code: string
  project_name: string
  client_name?: string
  site_address?: string
  contract_date?: string
  start_date?: string
  end_date?: string
  team_leader_id?: string
  manager_id?: string
  status: string
  target_profit_rate: number
  contract_supply_amount?: number
  contract_total_amount?: number
  note?: string
}

type FlowSummary = {
  current_contract_amount: number
  current_execution_budget: number
  current_purchase_amount: number
  accumulated_completed_amount: number
  actual_paid_amount: number
}

type ProfitAndBalance = {
  target_cost_limit: number
  unpurchased_budget_amount: number
  expected_final_cost: number
  expected_profit: number
  expected_profit_rate: number
  unbilled_amount: number
  approved_unpaid_amount: number
  total_unpaid_balance: number
}

type CostBreakdown = {
  budget_id: string
  work_category: string
  initial_execution_budget: number
  current_execution_budget: number
  purchase_amount: number
  accumulated_completed_amount: number
  actual_paid_amount: number
  budget_variance: number
}

type IntegratedSummary = {
  project_info: Project
  flow_summary: FlowSummary
  profit_and_balance: ProfitAndBalance
  cost_breakdown: CostBreakdown[]
}

type CustomerContractItem = {
  item_id?: string
  work_category: string
  quoted_amount: number
  contract_amount: number
  vat_amount: number
  total_amount?: number
  note?: string
}

type ContractFile = {
  file_id: string
  file_name: string
  file_type: string
  download_url: string
}

type CustomerContract = {
  project_id: string
  project_code: string
  project_name: string
  client_name?: string
  target_profit_rate: number
  total_contract_supply_amount: number
  current_contract_supply_amount: number
  total_vat_amount: number
  total_contract_with_vat: number
  current_contract_with_vat: number
  target_cost_limit: number
  items: CustomerContractItem[]
  files: ContractFile[]
}

type ExecutionBudgetDetail = {
  detail_id?: string
  budget_item_id: string
  category_name: string
  item_name: string
  cost_type: string
  vendor_description?: string
  unit?: string
  quantity: number
  unit_price: number
  amount?: number
  evidence_link?: string
  note?: string
}

type ExecutionBudgetSummaryItem = {
  budget_item_id: string
  category_name: string
  item_name: string
  rolled_up_amount: number
}

type ExecutionBudgetDetailResponse = {
  project_id: string
  project_code: string
  project_name: string
  contract_supply_amount: number
  target_cost_limit: number
  approval_status: string
  approval_id?: string
  approval_comment?: string
  approved_amount?: number
  total_detail_amount: number
  over_target_limit: boolean
  summary_by_item: ExecutionBudgetSummaryItem[]
  details: ExecutionBudgetDetail[]
}

type VendorComparisonItem = {
  comparison_id?: string
  comparison_code?: string
  vendor_name: string
  quoted_amount: number
  vat_type: string
  construction_period?: string
  scope_and_notes?: string
  payment_terms?: string
  estimate_file_url?: string
  is_selected: boolean
  selection_reason?: string
  budget_variance?: number
}

type VendorComparisonResponse = {
  project_id: string
  project_code: string
  execution_item_id: string
  work_category: string
  execution_item_name: string
  approved_execution_budget: number
  comparisons: VendorComparisonItem[]
}

type PurchaseOrderDraft = {
  project_code: string
  execution_item_id: string
  work_category: string
  execution_item_name: string
  approved_execution_budget: number
  selected_vendor_name: string
  proposed_po_amount: number
  budget_variance: number
  variance_type: string
  display_color: string
  selection_reason: string
  approval_status: string
  po_id?: string
  approved_at?: string
  approver?: string
}

type ChangeOrderImpact = { category: string; target_id: string; label: string; delta_amount: number }
type ChangeOrder = {
  change_order_id: string
  project_id: string
  title: string
  reason: string
  requested_by: string
  status: string
  created_at: string
  contract_delta_amount: number
  budget_delta_amount: number
  purchase_order_delta_amount: number
  impacts: ChangeOrderImpact[]
}

type PurchaseOrderHistoryItem = {
  po_id: string
  requisition_id?: string
  approval_id?: string
  selected_comparison_id?: string
  execution_item_id: string
  work_category: string
  execution_item_name: string
  vendor_name: string
  initial_po_amount: number
  current_po_amount: number
  approved_execution_budget: number
  budget_variance: number
  selection_reason?: string
  construction_period?: string
  scope_and_notes?: string
  payment_terms?: string
  estimate_file_url?: string
  approved_at?: string
  approver?: string
  approval_comment?: string
  approved_claim_total: number
  actual_paid_total: number
  total_unpaid_balance: number
}

type PurchaseOrderHistoryResponse = {
  project_id: string
  project_code: string
  items: PurchaseOrderHistoryItem[]
}

type ApprovalItem = {
  approval_id: string
  project_id: string
  project_code: string
  project_name: string
  approval_type: string
  target_id: string
  title: string
  requested_amount: number
  previous_amount: number
  variance_amount: number
  request_user: string
  request_reason?: string
  status: string
  approver?: string
  comment?: string
  requested_at?: string
  decided_at?: string
  cancelled_by?: string
  cancellation_reason?: string
  cancelled_at?: string
  detail?: unknown
  attachments?: ApprovalAttachment[]
}

type ApprovalAttachment = {
  attachment_id: string
  file_name: string
  content_type?: string
  download_url: string
}

type PaymentStatus = {
  payment_id: string
  paid_amount: number
  paid_date: string
  payment_status: string
  account_info?: string
  receipt_link?: string
}

type ClaimStatus = {
  claim_id: string
  degree: number
  claim_amount: number
  approval_status: string
  payment_state: string
  paid_total: number
  remaining_payable: number
  payments: PaymentStatus[]
}

type PurchaseOrderPaymentStatus = {
  po_id: string
  vendor_name: string
  current_po_amount: number
  approved_claim_total: number
  actual_paid_total: number
  unclaimed_amount: number
  approved_unpaid_amount: number
  total_unpaid_balance: number
  payment_request_status: string
  claims: ClaimStatus[]
}

type ProgressPaymentResponse = {
  project_id: string
  project_code: string
  all_payments_approved: boolean
  pending_payment_approval_count: number
  purchaseOrders: PurchaseOrderPaymentStatus[]
}

type SettlementAmountSummary = {
  initial_amount: number
  change_amount: number
  final_amount: number
}

type ProjectSettlement = {
  project_id: string
  project_code: string
  project_name: string
  contract: SettlementAmountSummary
  execution_budget: SettlementAmountSummary
  purchase_orders: SettlementAmountSummary
  payments: {
    approved_claims_total: number
    actual_paid_total: number
    unpaid_balance: number
    uncollected_receivable: number
  }
  profitability: {
    final_cost: number
    final_profit: number
    profit_margin_rate: number
  }
  is_settled: boolean
  settlement_id?: string
  settlement_status?: string
  settlement_notes?: string
  unpaid_handling_reason?: string
  settled_by?: string
  settled_at?: string
}

type ProjectForm = {
  project_name: string
  client_name: string
  site_address: string
  contract_date: string
  start_date: string
  end_date: string
  team_leader_id: string
  manager_id: string
  target_profit_rate: string
  note: string
}

const tabs = ['고객 견적·계약 관리', '세부실행예산', '업체 비교견적', '통합결재함', '공종별 흐름 추적', '계약/발주 이력', '기성 및 지급 현황', '준공정산', '증빙 및 파일']
const projectTabs = tabs.filter((tab) => tab !== '통합결재함')
const costTypes = ['외주비', '폐기물', '인건비', '장비비', '운반비', '자재비', '현장경비']
const vatTypes = ['별도', '포함', '면세']

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

const currency = new Intl.NumberFormat('ko-KR')
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

function apiUrl(path: string) {
  return `${apiBaseUrl}${path}`
}

function formatWon(value: number | undefined) {
  return `${currency.format(Math.round(value ?? 0))} 원`
}

function formatDate(value: string | undefined) {
  return value || '-'
}

function formatDateTime(value: string | undefined) {
  if (!value) return '-'
  const [date, time] = value.replace('T', ' ').split(' ')
  return time ? `${formatDate(date)} ${time.slice(0, 5)}` : formatDate(date)
}

function toNumber(value: string) {
  const normalized = value.replaceAll(',', '')
  return Number(normalized || 0)
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
        const response = await fetch(apiUrl(`/api/v1/projects/${selectedProjectId}/integrated-summary`), {
          signal: controller.signal,
        })
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
      const query = search.trim() ? `?keyword=${encodeURIComponent(search.trim())}` : ''
      const response = await fetch(apiUrl(`/api/v1/projects${query}`))
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

    const response = await fetch(apiUrl('/api/v1/projects'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })

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

    const response = await fetch(apiUrl(`/api/v1/projects/${project.project_id}`), {
      method: 'DELETE',
    })

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
      const response = await fetch(apiUrl(`/api/v1/projects/${projectId}/contracts`))
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

    const response = await fetch(apiUrl(`/api/v1/projects/${selectedProjectId}/contracts`), {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })

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
      const summaryResponse = await fetch(apiUrl(`/api/v1/projects/${selectedProjectId}/integrated-summary`))
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

    const response = await fetch(apiUrl(`/api/v1/projects/${selectedProjectId}/contract-files`), {
      method: 'POST',
      body: formData,
    })

    if (!response.ok) {
      setError(`계약 파일 업로드 실패: ${response.status}`)
      return
    }

    const data: CustomerContract = await response.json()
    setContract(data)
  }

  async function loadExecutionBudgetDetails(projectId: string) {
    try {
      const response = await fetch(apiUrl(`/api/v1/projects/${projectId}/execution-budget-details`))
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

    const response = await fetch(apiUrl(`/api/v1/projects/${selectedProjectId}/execution-budget-details`), {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })
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
    const response = await fetch(apiUrl(`/api/v1/projects/${selectedProjectId}/change-orders/executive-correction`), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        target_contract_supply_amount: targetAmount,
        reason: reason.trim(),
        apply_execution_budget: true,
        apply_purchase_orders: true,
      }),
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
    const summaryResponse = await fetch(apiUrl(`/api/v1/projects/${selectedProjectId}/integrated-summary`))
    if (summaryResponse.ok) setSummary(await summaryResponse.json())
    setError(null)
  }

  async function loadChangeOrders(projectId: string) {
    const response = await fetch(apiUrl(`/api/v1/projects/${projectId}/change-orders`))
    if (response.ok) setChangeOrders(await response.json())
  }

  async function requestExecutionBudgetApproval() {
    if (!selectedProjectId) return
    const formData = new FormData()
    formData.append('request', new Blob([
      JSON.stringify({ request_user: 'system', request_reason: '최초 실행예산 승인 요청' }),
    ], { type: 'application/json' }))
    executionBudgetApprovalFiles.forEach((file) => formData.append('files', file))
    const response = await fetch(apiUrl(`/api/v1/projects/${selectedProjectId}/execution-budget-details/request-approval`), {
      method: 'POST',
      body: formData,
    })
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
    const response = await fetch(apiUrl(`/api/v1/approvals?${params.toString()}`))
    if (!response.ok) {
      setError(`통합결재함 조회 실패: ${response.status}`)
      return
    }
    const data: ApprovalItem[] = await response.json()
    setApprovals(data)
    setSelectedApproval((current) => current ? data.find((approval) => approval.approval_id === current.approval_id) ?? null : null)
  }

  async function loadApprovalDetail(approvalId: string) {
    const response = await fetch(apiUrl(`/api/v1/approvals/${approvalId}`))
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
    const response = await fetch(apiUrl(`/api/v1/approvals/${approvalId}/${decision}`), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ approver: '대표', comment: decision === 'approve' ? '승인' : rejectComment }),
    })
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
      const summaryResponse = await fetch(apiUrl(`/api/v1/projects/${selectedProjectId}/integrated-summary`))
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
      const response = await fetch(apiUrl(`/api/v1/projects/${projectId}/execution-items/${itemId}/comparisons`))
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
    const response = await fetch(apiUrl(`/api/v1/projects/${projectId}/purchase-orders/draft/${itemId}`))
    if (!response.ok) {
      setPurchaseOrderDraft(null)
      if (showError) setError(`발주품의 초안 조회 실패: ${response.status}`)
      return
    }
    setPurchaseOrderDraft(await response.json())
    await loadSettlement(projectId)
  }

  async function loadPurchaseOrders(projectId: string) {
    const response = await fetch(apiUrl(`/api/v1/projects/${projectId}/purchase-orders`))
    if (!response.ok) {
      setError(`발주 이력 조회 실패: ${response.status}`)
      return
    }
    const data: PurchaseOrderHistoryResponse = await response.json()
    setPurchaseOrderHistory(data.items)
  }

  async function saveVendorComparisons() {
    if (!selectedProjectId || !selectedExecutionItemId) return
    const response = await fetch(apiUrl(`/api/v1/projects/${selectedProjectId}/execution-items/${selectedExecutionItemId}/comparisons`), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
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
      }),
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
    const response = await fetch(apiUrl(`/api/v1/projects/${selectedProjectId}/purchase-orders/draft/${selectedExecutionItemId}/request`), { method: 'POST' })
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
      const response = await fetch(apiUrl(`/api/v1/projects/${projectId}/progress-payments`))
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
      const response = await fetch(apiUrl(`/api/v1/projects/${projectId}/settlement`))
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
    const response = await fetch(apiUrl(`/api/v1/projects/${selectedProjectId}/settlement/complete`), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        uncollected_receivable: toNumber(settlementForm.uncollected_receivable),
        settlement_notes: settlementForm.settlement_notes,
        force_complete_if_unpaid: settlementForm.force_complete_if_unpaid,
        unpaid_handling_reason: settlementForm.unpaid_handling_reason,
        settled_by: selectedProject?.manager_id ?? 'system',
      }),
    })
    if (!response.ok) {
      const message = await response.text()
      setError(message || `준공정산 완료 처리 실패: ${response.status}`)
      return
    }
    setSettlement(await response.json())
    await loadProjects(keyword)
    const summaryResponse = await fetch(apiUrl(`/api/v1/projects/${selectedProjectId}/integrated-summary`))
    if (summaryResponse.ok) {
      setSummary(await summaryResponse.json())
    }
  }

  async function createProgressClaim() {
    if (!selectedProjectId || !selectedPoId) return
    const response = await fetch(apiUrl(`/api/v1/projects/${selectedProjectId}/progress-claims`), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ po_id: selectedPoId, degree: Number(claimForm.degree || 0), claim_amount: toNumber(claimForm.claim_amount) }),
    })
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
    const response = await fetch(apiUrl(`/api/v1/projects/${selectedProjectId}/progress-claims/${claimId}/approve`), { method: 'POST' })
    if (!response.ok) {
      setError(`기성 승인 실패: ${response.status}`)
      return
    }
    setProgressPayment(await response.json())
    await loadSettlement(selectedProjectId)
  }

  async function requestPaymentApprovals() {
    if (!selectedProjectId || selectedPaymentPoIds.length === 0) return
    const response = await fetch(apiUrl(`/api/v1/projects/${selectedProjectId}/payment-requests`), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ po_ids: selectedPaymentPoIds }),
    })
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
        rolled_up_amount: 0,
      }
      current.category_name = detail.category_name
      current.item_name = detail.item_name
      current.rolled_up_amount += (detail.quantity || 0) * (detail.unit_price || 0)
      summary.set(detail.budget_item_id, current)
    })
    return [...summary.values()].sort((a, b) => a.budget_item_id.localeCompare(b.budget_item_id))
  }, [executionDetails])

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

function ProjectList({
  projects,
  keyword,
  setKeyword,
  onSelect,
  onDelete,
}: {
  projects: Project[]
  keyword: string
  setKeyword: (value: string) => void
  onSelect: (projectId: string) => void
  onDelete: (project: Project) => void
}) {
  return (
    <section className="rounded-md border border-slate-200 bg-white shadow-sm">
      <div className="flex flex-col gap-3 border-b border-slate-200 px-5 py-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h2 className="text-lg font-semibold text-slate-950">프로젝트 목록</h2>
          <p className="mt-1 text-sm text-slate-500">프로젝트를 선택하면 통합현황 상세 화면으로 이동합니다.</p>
        </div>
        <div className="flex w-full items-center gap-2 rounded-md border border-slate-300 bg-white px-3 py-2 lg:w-80">
          <Search className="h-4 w-4 text-slate-400" aria-hidden="true" />
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="프로젝트명 또는 프로젝트번호"
            className="w-full border-0 bg-transparent text-sm outline-none"
          />
        </div>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-[1320px] w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">관리</th>
              <th className="px-4 py-3">프로젝트명(프로젝트번호)</th>
              <th className="px-4 py-3">고객/발주처</th>
              <th className="px-4 py-3">현장주소</th>
              <th className="px-4 py-3">계약일</th>
              <th className="px-4 py-3">착공일</th>
              <th className="px-4 py-3">준공예정일</th>
              <th className="px-4 py-3">담당 팀장/담당자</th>
              <th className="px-4 py-3">프로젝트 상태</th>
              <th className="px-4 py-3">비고</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {projects.length === 0 ? (
              <tr>
                <td colSpan={10} className="px-4 py-8 text-center text-slate-500">
                  등록된 프로젝트가 없습니다.
                </td>
              </tr>
            ) : (
              projects.map((project) => (
                <tr
                  key={project.project_id}
                  onClick={() => onSelect(project.project_id)}
                  className="cursor-pointer hover:bg-cyan-50"
                >
                  <td className="px-4 py-3">
                    <button
                      type="button"
                      onClick={(event) => {
                        event.stopPropagation()
                        onDelete(project)
                      }}
                      disabled={project.status === '정산완료'}
                      className="inline-flex items-center gap-1 rounded-md border border-rose-200 px-2.5 py-1.5 text-xs font-semibold text-rose-700 hover:bg-rose-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-400 disabled:hover:bg-transparent"
                      aria-label={`${project.project_name} 삭제`}
                    >
                      <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                      {project.status === '정산완료' ? '정산완료' : '삭제'}
                    </button>
                  </td>
                  <td className="px-4 py-3">
                    <p className="font-semibold text-slate-950">{project.project_name}</p>
                    <p className="mt-1 text-xs text-slate-500">{project.project_code}</p>
                  </td>
                  <td className="px-4 py-3 text-slate-600">{project.client_name ?? '-'}</td>
                  <td className="px-4 py-3 text-slate-600">{project.site_address ?? '-'}</td>
                  <td className="px-4 py-3 text-slate-600">{formatDate(project.contract_date)}</td>
                  <td className="px-4 py-3 text-slate-600">{formatDate(project.start_date)}</td>
                  <td className="px-4 py-3 text-slate-600">{formatDate(project.end_date)}</td>
                  <td className="px-4 py-3 text-slate-600">
                    <p>{project.team_leader_id ?? '-'}</p>
                    <p className="mt-1 text-xs text-slate-500">{project.manager_id ?? '-'}</p>
                  </td>
                  <td className="px-4 py-3">
                    <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-700">
                      {project.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-slate-600">{project.note ?? '-'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function ContractManagementPanel({
  contract,
  contractItems,
  contractTotal,
  targetProfitRate,
  selectedFileType,
  setTargetProfitRate,
  setSelectedFileType,
  saveCustomerContract,
  correctAmountByExecutive,
  uploadContractFile,
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
  saveCustomerContract: () => void
  correctAmountByExecutive: () => void
  uploadContractFile: (file: File | null) => void
  updateContractItem: (index: number, field: keyof CustomerContractItem, value: string) => void
  addContractItem: () => void
  removeContractItem: (index: number) => void
}) {
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
            onClick={correctAmountByExecutive}
            className="inline-flex items-center justify-center gap-2 rounded-md border border-amber-500 bg-amber-50 px-4 py-2 text-sm font-semibold text-amber-800 hover:bg-amber-100"
          >
            대표 금액 정정
          </button>
          <button
            type="button"
            onClick={saveCustomerContract}
            className="inline-flex items-center justify-center gap-2 rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-800"
          >
            <Save className="h-4 w-4" aria-hidden="true" />
            계약 저장
          </button>
        </div>
      </div>

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
                      list="work-categories"
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
          <div>
            <h3 className="text-sm font-semibold text-slate-900">견적/계약 증빙 파일</h3>
            <p className="mt-1 text-sm text-slate-500">제출 견적서와 최종 계약서 파일을 프로젝트에 첨부합니다.</p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
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
              <a key={file.file_id} href={apiUrl(file.download_url)} className="flex items-center justify-between gap-3 px-4 py-3 text-sm hover:bg-slate-50">
                <span>
                  <span className="font-semibold text-slate-900">{file.file_name}</span>
                  <span className="ml-2 rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">
                    {file.file_type}
                  </span>
                </span>
                <Download className="h-4 w-4 text-slate-400" aria-hidden="true" />
              </a>
            ))
          )}
        </div>
      </div>
    </div>
  )
}

function ExecutionBudgetDetailPanel({
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
  const isLocked = executionBudget.approval_status === 'REQUESTED' || executionBudget.approval_status === 'APPROVED'
  const displayedBudgetAmount = executionBudget.approval_status === 'APPROVED' ? (executionBudget.approved_amount ?? totalAmount) : totalAmount

  return (
    <div>
      <div className="flex flex-col gap-4 border-b border-slate-200 pb-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 className="text-lg font-semibold text-slate-950">세부실행예산 입력</h3>
          <p className="mt-1 text-sm text-slate-500">세부 항목의 수량과 단가를 입력하면 실행항목ID별 상위 예산이 자동 집계됩니다.</p>
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
          helper={`결재상태 ${executionBudget.approval_status}`}
          accent={executionBudget.over_target_limit || displayedBudgetAmount > executionBudget.target_cost_limit ? 'text-rose-700' : 'text-cyan-700'}
        />
      </div>

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
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
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
        <h3 className="mb-3 text-sm font-semibold text-slate-900">세부실행예산 입력 항목</h3>
        <div className="mb-3 flex justify-end text-xs font-medium text-slate-500">
          {details.length > 0 ? `${pageStart + 1}-${Math.min(pageStart + pageSize, details.length)} / ${details.length}` : '0 / 0'}
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-[1640px] text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
				<th className="px-3 py-3">순번</th>
                <th className="px-3 py-3">No</th>
                <th className="px-3 py-3">프로젝트</th>
                <th className="px-3 py-3">실행항목ID</th>
                <th className="px-3 py-3">공종</th>
                <th className="px-3 py-3">실행항목</th>
                <th className="px-3 py-3">원가구분</th>
                <th className="px-3 py-3">업체/내용</th>
                <th className="px-3 py-3">규격/단위</th>
                <th className="px-3 py-3">수량</th>
                <th className="px-3 py-3">단가</th>
                <th className="px-3 py-3">금액</th>
                <th className="px-3 py-3">증빙링크</th>
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
                    <input value={detail.budget_item_id} onChange={(event) => updateExecutionDetail(index, 'budget_item_id', event.target.value)} className="input" />
                  </td>
                  <td className="px-3 py-3">
                    <input value={detail.category_name} onChange={(event) => updateExecutionDetail(index, 'category_name', event.target.value)} className="input" />
                  </td>
                  <td className="px-3 py-3">
                    <input value={detail.item_name} onChange={(event) => updateExecutionDetail(index, 'item_name', event.target.value)} className="input" />
                  </td>
                  <td className="px-3 py-3">
                    <select value={detail.cost_type} onChange={(event) => updateExecutionDetail(index, 'cost_type', event.target.value)} className="input">
                      {costTypes.map((type) => (
                        <option key={type} value={type}>{type}</option>
                      ))}
                    </select>
                  </td>
                  <td className="px-3 py-3">
                    <input value={detail.vendor_description ?? ''} onChange={(event) => updateExecutionDetail(index, 'vendor_description', event.target.value)} className="input" />
                  </td>
                  <td className="px-3 py-3">
                    <input value={detail.unit ?? ''} onChange={(event) => updateExecutionDetail(index, 'unit', event.target.value)} className="input" />
                  </td>
                  <td className="px-3 py-3">
                    <input value={detail.quantity} onChange={(event) => updateExecutionDetail(index, 'quantity', event.target.value)} inputMode="decimal" className="input" />
                  </td>
                  <td className="px-3 py-3">
                    <MoneyInput value={detail.unit_price} onChange={(value) => updateExecutionDetail(index, 'unit_price', value)} />
                  </td>
                  <td className="px-3 py-3 font-semibold text-slate-900">{formatWon((detail.quantity || 0) * (detail.unit_price || 0))}</td>
                  <td className="px-3 py-3">
                    <input value={detail.evidence_link ?? ''} onChange={(event) => updateExecutionDetail(index, 'evidence_link', event.target.value)} className="input" />
                  </td>
                  <td className="px-3 py-3">
                    <input value={detail.note ?? ''} onChange={(event) => updateExecutionDetail(index, 'note', event.target.value)} className="input" />
                  </td>
                  <td className="px-3 py-3">
                    <div className="flex gap-1">
                      <button type="button" onClick={() => addExecutionDetail(index)} className="rounded-md p-2 text-slate-500 hover:bg-cyan-50 hover:text-cyan-700" aria-label="행 복사">
                        <Plus className="h-4 w-4" aria-hidden="true" />
                      </button>
                      <button type="button" onClick={() => removeExecutionDetail(index)} className="rounded-md p-2 text-slate-500 hover:bg-rose-50 hover:text-rose-700" aria-label="행 삭제">
                        <Trash2 className="h-4 w-4" aria-hidden="true" />
                      </button>
                    </div>
                  </td>
                </tr>
                )
              })}
              <tr className="bg-slate-50 font-semibold text-slate-900">
                <td className="px-3 py-3" colSpan={10}>합계</td>
                <td className="px-3 py-3">{formatWon(totalAmount)}</td>
                <td className="px-3 py-3" colSpan={3}></td>
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
          className="mt-4 inline-flex items-center gap-2 rounded-md border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
        >
          <Plus className="h-4 w-4" aria-hidden="true" />
          행 추가
        </button>
      </div>
    </div>
  )
}

function ProgressPaymentPanel({
  progressPayment,
  selectedPoId,
  setSelectedPoId,
  claimForm,
  setClaimForm,
  selectedPaymentPoIds,
  setSelectedPaymentPoIds,
  createProgressClaim,
  approveProgressClaim,
  requestPaymentApprovals,
}: {
  progressPayment: ProgressPaymentResponse
  selectedPoId: string
  setSelectedPoId: (value: string) => void
  claimForm: { degree: string; claim_amount: string }
  setClaimForm: (value: { degree: string; claim_amount: string }) => void
  selectedPaymentPoIds: string[]
  setSelectedPaymentPoIds: (poIds: string[]) => void
  createProgressClaim: () => void
  approveProgressClaim: (claimId: string) => void
  requestPaymentApprovals: () => void
}) {
  const selectedPo = progressPayment.purchaseOrders.find((po) => po.po_id === selectedPoId) ?? progressPayment.purchaseOrders[0]
  const paymentRows = progressPayment.purchaseOrders.flatMap((po) => po.claims.map((claim) => ({ po, claim })))
  const totalApprovedClaims = progressPayment.purchaseOrders.reduce((sum, po) => sum + po.approved_claim_total, 0)
  const totalPaid = progressPayment.purchaseOrders.reduce((sum, po) => sum + po.actual_paid_total, 0)
  const selectablePoIds = progressPayment.purchaseOrders.filter((po) => po.approved_unpaid_amount > 0 && po.payment_request_status !== 'REQUESTED').map((po) => po.po_id)

  if (!selectedPo) {
    return <div className="rounded-md border border-dashed border-slate-300 bg-slate-50 p-8 text-center text-sm text-slate-500">등록된 발주가 없습니다.</div>
  }

  return (
    <div>
      <div className="flex flex-col gap-4 border-b border-slate-200 pb-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 className="text-lg font-semibold text-slate-950">기성 및 지급 현황</h3>
          <p className="mt-1 text-sm text-slate-500">프로젝트 전체 업체의 기성·지급 현황을 확인하고, 선택 업체만 지급결재를 요청합니다.</p>
        </div>
        <button type="button" onClick={requestPaymentApprovals} disabled={selectedPaymentPoIds.length === 0} className="rounded-md bg-cyan-700 px-4 py-2 text-sm font-semibold text-white hover:bg-cyan-800 disabled:cursor-not-allowed disabled:bg-slate-300">선택 업체 지급요청 ({selectedPaymentPoIds.length})</button>
      </div>

      <div className="grid gap-3 border-b border-slate-200 py-5 md:grid-cols-3">
        <MetricPanel label="발주 업체 수" value={`${progressPayment.purchaseOrders.length} 곳`} helper={`지급결재 대기 ${progressPayment.pending_payment_approval_count}건`} />
        <MetricPanel label="승인 기성누계" value={formatWon(totalApprovedClaims)} helper="전체 업체 합계" />
        <MetricPanel label="실제 지급누계" value={formatWon(totalPaid)} helper="지급결재 승인 반영" />
        <MetricPanel label="전체 승인 후 미지급" value={formatWon(totalApprovedClaims - totalPaid)} helper="지급요청 가능 금액" accent={totalApprovedClaims > totalPaid ? 'text-rose-700' : 'text-slate-950'} />
        <MetricPanel label="준공정산 이동 조건" value={progressPayment.all_payments_approved ? '충족' : '미충족'} helper="모든 정식 발주 지급 완료 기준" accent={progressPayment.all_payments_approved ? 'text-cyan-700' : 'text-slate-700'} />
      </div>

      <div className="border-b border-slate-200 py-5">
        <h3 className="mb-3 text-sm font-semibold text-slate-900">업체별 지급 대시보드</h3>
        <div className="overflow-x-auto"><table className="min-w-[1150px] text-left text-sm"><thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500"><tr><th className="px-3 py-3"><input type="checkbox" checked={selectablePoIds.length > 0 && selectablePoIds.every((id) => selectedPaymentPoIds.includes(id))} onChange={(event) => setSelectedPaymentPoIds(event.target.checked ? selectablePoIds : [])} /></th><th className="px-3 py-3">업체</th><th className="px-3 py-3">발주번호</th><th className="px-3 py-3">현재 발주금액</th><th className="px-3 py-3">승인 기성</th><th className="px-3 py-3">지급누계</th><th className="px-3 py-3">승인 후 미지급</th><th className="px-3 py-3">지급결재 상태</th></tr></thead><tbody className="divide-y divide-slate-100">{progressPayment.purchaseOrders.map((po) => { const selectable = selectablePoIds.includes(po.po_id); return <tr key={po.po_id}><td className="px-3 py-3"><input type="checkbox" disabled={!selectable} checked={selectedPaymentPoIds.includes(po.po_id)} onChange={(event) => setSelectedPaymentPoIds(event.target.checked ? [...selectedPaymentPoIds, po.po_id] : selectedPaymentPoIds.filter((id) => id !== po.po_id))} /></td><td className="px-3 py-3 font-semibold text-slate-900">{po.vendor_name}</td><td className="px-3 py-3 text-cyan-800">{po.po_id}</td><td className="px-3 py-3">{formatWon(po.current_po_amount)}</td><td className="px-3 py-3">{formatWon(po.approved_claim_total)}</td><td className="px-3 py-3">{formatWon(po.actual_paid_total)}</td><td className="px-3 py-3 font-semibold">{formatWon(po.approved_unpaid_amount)}</td><td className="px-3 py-3">{po.payment_request_status}</td></tr> })}</tbody></table></div>
      </div>

      <div className="grid gap-3 border-b border-slate-200 py-5 md:grid-cols-[220px_120px_1fr_auto]">
        <label className="block text-sm font-semibold text-slate-700">
          기성 작성 대상
          <select value={selectedPo.po_id} onChange={(event) => setSelectedPoId(event.target.value)} className="input mt-2">
            {progressPayment.purchaseOrders.map((po) => <option key={po.po_id} value={po.po_id}>{po.vendor_name} · {po.po_id}</option>)}
          </select>
        </label>
        <label className="block text-sm font-semibold text-slate-700">
          차수
          <input value={claimForm.degree} onChange={(event) => setClaimForm({ ...claimForm, degree: event.target.value })} className="input mt-2" />
        </label>
        <label className="block text-sm font-semibold text-slate-700">
          기성금액
          <MoneyInput value={toNumber(claimForm.claim_amount)} onChange={(value) => setClaimForm({ ...claimForm, claim_amount: value })} />
        </label>
        <button type="button" onClick={createProgressClaim} className="self-end rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-800">
          기성금액 작성
        </button>
      </div>

      <div className="py-5">
        <h3 className="mb-3 text-sm font-semibold text-slate-900">전체 업체 기성 및 지급 내역</h3>
        <div className="overflow-x-auto">
          <table className="min-w-[1180px] text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-3 py-3">업체</th>
                <th className="px-3 py-3">발주번호</th>
                <th className="px-3 py-3">차수</th>
                <th className="px-3 py-3">기성금액</th>
                <th className="px-3 py-3">결재상태</th>
                <th className="px-3 py-3">지급상태</th>
                <th className="px-3 py-3">지급누계</th>
                <th className="px-3 py-3">지급가능잔액</th>
                <th className="px-3 py-3">처리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {paymentRows.map(({ po, claim }) => {
                return (
                  <tr key={claim.claim_id}>
                    <td className="px-3 py-3 font-semibold text-slate-900">{po.vendor_name}</td>
                    <td className="px-3 py-3 text-cyan-800">{po.po_id}</td>
                    <td className="px-3 py-3 font-semibold text-slate-900">{claim.degree}차</td>
                    <td className="px-3 py-3">{formatWon(claim.claim_amount)}</td>
                    <td className="px-3 py-3">{claim.approval_status}</td>
                    <td className="px-3 py-3">
                      <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${claim.payment_state === 'PAID' ? 'bg-cyan-100 text-cyan-800' : claim.payment_state === 'WAITING_FOR_PAYMENT' ? 'bg-amber-100 text-amber-800' : 'bg-slate-100 text-slate-700'}`}>
                        {claim.payment_state}
                      </span>
                    </td>
                    <td className="px-3 py-3">{formatWon(claim.paid_total)}</td>
                    <td className="px-3 py-3">{formatWon(claim.remaining_payable)}</td>
                    <td className="px-3 py-3">
                      <div className="flex gap-2">
                        {claim.approval_status !== 'APPROVED' && (
                          <button type="button" onClick={() => approveProgressClaim(claim.claim_id)} className="rounded-md border border-slate-300 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50">대표 승인</button>
                        )}
                      </div>
                    </td>
                  </tr>
                )
              })}
              {paymentRows.length === 0 && (
                <tr>
                  <td colSpan={9} className="px-3 py-8 text-center text-slate-500">등록된 기성 차수가 없습니다.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

function SettlementPanel({
  settlement,
  form,
  setForm,
  completeSettlement,
}: {
  settlement: ProjectSettlement
  form: { uncollected_receivable: string; settlement_notes: string; force_complete_if_unpaid: boolean; unpaid_handling_reason: string }
  setForm: (value: { uncollected_receivable: string; settlement_notes: string; force_complete_if_unpaid: boolean; unpaid_handling_reason: string }) => void
  completeSettlement: () => void
}) {
  const hasUnpaidBalance = settlement.payments.unpaid_balance > 0
  const sections = [
    ['계약 및 매출 현황', settlement.contract, '최종 계약금액'],
    ['실행예산 현황', settlement.execution_budget, '최종 실행예산'],
    ['발주 및 집행 현황', settlement.purchase_orders, '최종 발주금액'],
  ] as const

  return (
    <div>
      <div className="flex flex-col gap-4 border-b border-slate-200 pb-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 className="text-lg font-semibold text-slate-950">준공정산</h3>
          <p className="mt-1 text-sm text-slate-500">계약, 실행예산, 발주, 기성, 지급 누계를 집계해 최종 손익과 마감 가능 여부를 확인합니다.</p>
        </div>
        <span className={`inline-flex w-fit items-center rounded-full px-3 py-1 text-xs font-semibold ${settlement.is_settled ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}`}>
          {settlement.is_settled ? '정산완료' : '작성중'}
        </span>
      </div>

      <div className="grid gap-4 py-5 lg:grid-cols-3">
        {sections.map(([title, amount, finalLabel]) => (
          <article key={title} className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
            <h4 className="text-sm font-semibold text-slate-900">{title}</h4>
            <dl className="mt-4 space-y-3 text-sm">
              <div className="flex items-center justify-between gap-4">
                <dt className="text-slate-500">최초 금액</dt>
                <dd className="font-semibold text-slate-900">{formatWon(amount.initial_amount)}</dd>
              </div>
              <div className="flex items-center justify-between gap-4">
                <dt className="text-slate-500">변경 금액</dt>
                <dd className={varianceClass(amount.change_amount)}>{formatWon(amount.change_amount)}</dd>
              </div>
              <div className="flex items-center justify-between gap-4 border-t border-slate-100 pt-3">
                <dt className="font-semibold text-slate-700">{finalLabel}</dt>
                <dd className="text-base font-semibold text-slate-950">{formatWon(amount.final_amount)}</dd>
              </div>
            </dl>
          </article>
        ))}
      </div>

      <div className="grid gap-4 border-t border-slate-200 py-5 lg:grid-cols-2">
        <article className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
          <h4 className="text-sm font-semibold text-slate-900">기성 및 지급 현황</h4>
          <dl className="mt-4 space-y-3 text-sm">
            <div className="flex items-center justify-between gap-4">
              <dt className="text-slate-500">승인 기성누계</dt>
              <dd className="font-semibold text-slate-900">{formatWon(settlement.payments.approved_claims_total)}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-slate-500">실제 지급누계</dt>
              <dd className="font-semibold text-slate-900">{formatWon(settlement.payments.actual_paid_total)}</dd>
            </div>
            <div className="flex items-center justify-between gap-4">
              <dt className="text-slate-500">미지급금액</dt>
              <dd className={`font-semibold ${hasUnpaidBalance ? 'text-rose-700' : 'text-slate-900'}`}>{formatWon(settlement.payments.unpaid_balance)}</dd>
            </div>
            <div>
              <dt className="mb-2 text-slate-500">미수금</dt>
              <dd><MoneyInput value={toNumber(form.uncollected_receivable)} onChange={(value) => setForm({ ...form, uncollected_receivable: value })} /></dd>
            </div>
          </dl>
        </article>

        <article className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
          <h4 className="text-sm font-semibold text-slate-900">최종 손익 분석</h4>
          <div className="mt-4 grid gap-3 sm:grid-cols-3">
            <div className="border-l-2 border-slate-200 pl-3">
              <p className="text-xs font-semibold text-slate-500">최종 원가</p>
              <p className="mt-2 text-lg font-semibold text-slate-950">{formatWon(settlement.profitability.final_cost)}</p>
              <p className="mt-1 text-xs text-slate-500">최종 발주금액 기준</p>
            </div>
            <div className="border-l-2 border-slate-200 pl-3">
              <p className="text-xs font-semibold text-slate-500">최종 이익</p>
              <p className={`mt-2 text-lg font-semibold ${settlement.profitability.final_profit < 0 ? 'text-rose-700' : 'text-cyan-700'}`}>{formatWon(settlement.profitability.final_profit)}</p>
              <p className="mt-1 text-xs text-slate-500">최종 계약금액 - 최종 원가</p>
            </div>
            <div className="border-l-2 border-slate-200 pl-3">
              <p className="text-xs font-semibold text-slate-500">최종 이익률</p>
              <p className="mt-2 text-lg font-semibold text-slate-950">{settlement.profitability.profit_margin_rate.toFixed(2)}%</p>
              <p className="mt-1 text-xs text-slate-500">최종 이익 / 최종 계약금액</p>
            </div>
          </div>
          {hasUnpaidBalance && (
            <div className="mt-4 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
              미지급 잔액이 남아 있습니다. 정산완료를 진행하려면 강제 완료 확인과 처리 사유가 필요합니다.
            </div>
          )}
        </article>
      </div>

      <div className="border-t border-slate-200 py-5">
        <div className="grid gap-4 lg:grid-cols-2">
          <Field label="정산 비고">
            <textarea value={form.settlement_notes} onChange={(event) => setForm({ ...form, settlement_notes: event.target.value })} className="input min-h-24" />
          </Field>
          <Field label="미지급 처리 사유">
            <textarea value={form.unpaid_handling_reason} onChange={(event) => setForm({ ...form, unpaid_handling_reason: event.target.value })} className="input min-h-24" disabled={!hasUnpaidBalance} />
          </Field>
        </div>
        <label className="mt-4 flex items-center gap-2 text-sm font-semibold text-slate-700">
          <input
            type="checkbox"
            checked={form.force_complete_if_unpaid}
            onChange={(event) => setForm({ ...form, force_complete_if_unpaid: event.target.checked })}
            disabled={!hasUnpaidBalance}
          />
          미지급 잔액이 있어도 사유를 남기고 정산완료 처리
        </label>
        <div className="mt-5 flex justify-end">
          <button
            type="button"
            onClick={completeSettlement}
            disabled={settlement.is_settled || (hasUnpaidBalance && (!form.force_complete_if_unpaid || !form.unpaid_handling_reason.trim()))}
            className="inline-flex items-center gap-2 rounded-md bg-cyan-700 px-4 py-2 text-sm font-semibold text-white hover:bg-cyan-800 disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
            준공정산 완료
          </button>
        </div>
        {settlement.is_settled && (
          <p className="mt-3 text-right text-xs text-slate-500">
            {settlement.settlement_id} · {settlement.settled_by ?? 'system'} · {settlement.settled_at ?? '-'}
          </p>
        )}
      </div>
    </div>
  )
}

function VendorComparisonPanel({
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
  const selected = comparisons.find((comparison) => comparison.is_selected)
  const approvedBudget = vendorComparison.approved_execution_budget || 0
  const isPurchaseOrderLocked = purchaseOrderDraft?.approval_status === 'REQUESTED' || purchaseOrderDraft?.approval_status === 'APPROVED'
  const canRequestPurchaseOrder = executionBudget.approval_status === 'APPROVED' && !!selected && !isPurchaseOrderLocked

  return (
    <div>
      <div className="flex flex-col gap-4 border-b border-slate-200 pb-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 className="text-lg font-semibold text-slate-950">업체 비교견적 및 발주품의</h3>
          <p className="mt-1 text-sm text-slate-500">승인 실행예산을 기준으로 업체 견적을 비교하고 선정 업체를 발주품의로 연결합니다.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" onClick={saveVendorComparisons} disabled={isPurchaseOrderLocked} className="inline-flex items-center gap-2 rounded-md bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-300">
            <Save className="h-4 w-4" aria-hidden="true" />
            비교견적 저장
          </button>
          <button type="button" onClick={requestPurchaseOrder} disabled={!canRequestPurchaseOrder} className="inline-flex items-center gap-2 rounded-md bg-cyan-700 px-4 py-2 text-sm font-semibold text-white hover:bg-cyan-800 disabled:cursor-not-allowed disabled:bg-slate-300">
            발주품의 요청
          </button>
        </div>
      </div>

      <div className="grid gap-4 border-b border-slate-200 py-5 lg:grid-cols-[260px_1fr_1fr]">
        <label className="block text-sm font-semibold text-slate-700">
          실행항목ID
          <select value={selectedExecutionItemId} onChange={(event) => setSelectedExecutionItemId(event.target.value)} className="input mt-2">
            {executionBudget.summary_by_item.map((item) => (
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

function PurchaseOrderHistoryPanel({ purchaseOrders, changeOrders = [] }: { purchaseOrders: PurchaseOrderHistoryItem[]; changeOrders?: ChangeOrder[] }) {
  return (
    <div>
      <div className="border-b border-slate-200 pb-4">
        <h3 className="text-lg font-semibold text-slate-950">계약·발주 이력</h3>
        <p className="mt-1 text-sm text-slate-500">발주 결재가 승인된 선정 업체만 정식 발주로 표시합니다.</p>
      </div>
      {false && <section className="border-b border-slate-200 py-5">
        <h4 className="text-sm font-semibold text-slate-900">변경 이력</h4>
        <p className="mt-1 text-sm text-slate-500">대표 정정은 현재 금액에 즉시 반영되고, 최초 금액과 증감 내역은 아래 이력으로 보관됩니다.</p>
        <div className="mt-3 overflow-x-auto rounded-md border border-slate-200">
          <table className="min-w-[900px] text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500"><tr><th className="px-3 py-3">변경번호</th><th className="px-3 py-3">일시</th><th className="px-3 py-3">사유</th><th className="px-3 py-3">계약 증감</th><th className="px-3 py-3">실행예산 증감</th><th className="px-3 py-3">발주 증감</th><th className="px-3 py-3">연결 항목</th></tr></thead>
            <tbody className="divide-y divide-slate-100">
              {changeOrders.map((change) => <tr key={change.change_order_id}><td className="px-3 py-3 font-semibold text-cyan-800">{change.change_order_id}</td><td className="px-3 py-3 text-slate-600">{formatDate(change.created_at?.slice(0, 10))}</td><td className="px-3 py-3 text-slate-700">{change.reason}</td><td className="px-3 py-3 font-semibold">{formatWon(change.contract_delta_amount)}</td><td className="px-3 py-3">{formatWon(change.budget_delta_amount)}</td><td className="px-3 py-3">{formatWon(change.purchase_order_delta_amount)}</td><td className="px-3 py-3 text-slate-600">{change.impacts.map((impact) => `${impact.label} (${formatWon(impact.delta_amount)})`).join(', ')}</td></tr>)}
              {changeOrders.length === 0 && <tr><td colSpan={7} className="px-3 py-8 text-center text-slate-500">등록된 변경 이력이 없습니다.</td></tr>}
            </tbody>
          </table>
        </div>
      </section>}
      <div className="overflow-x-auto py-5">
        <table className="min-w-[1500px] text-left text-sm">
          <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
            <tr>
              <th className="px-3 py-3">발주번호</th><th className="px-3 py-3">실행항목</th><th className="px-3 py-3">선정 업체</th><th className="px-3 py-3">승인 실행예산</th><th className="px-3 py-3">현재 발주금액</th><th className="px-3 py-3">예산 대비</th><th className="px-3 py-3">승인 정보</th><th className="px-3 py-3">기성 / 지급 / 미지급</th><th className="px-3 py-3">선정 근거</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {purchaseOrders.map((purchaseOrder) => (
              <tr key={purchaseOrder.po_id}>
                <td className="px-3 py-3 font-semibold text-cyan-800">{purchaseOrder.po_id}</td>
                <td className="px-3 py-3 text-slate-700">{purchaseOrder.execution_item_id} · {purchaseOrder.work_category}<br />{purchaseOrder.execution_item_name}</td>
                <td className="px-3 py-3 font-semibold text-slate-900">{purchaseOrder.vendor_name}</td>
                <td className="px-3 py-3 text-slate-700">{formatWon(purchaseOrder.approved_execution_budget)}</td>
                <td className="px-3 py-3 font-semibold text-slate-900">{formatWon(purchaseOrder.current_po_amount)}</td>
                <td className={`px-3 py-3 font-semibold ${varianceClass(purchaseOrder.budget_variance)}`}>{formatWon(purchaseOrder.budget_variance)}</td>
                <td className="px-3 py-3 text-slate-600">{formatDate(purchaseOrder.approved_at?.slice(0, 10))}<br />{purchaseOrder.approver ?? '-'}</td>
                <td className="px-3 py-3 text-slate-600">{formatWon(purchaseOrder.approved_claim_total)} / {formatWon(purchaseOrder.actual_paid_total)} / {formatWon(purchaseOrder.total_unpaid_balance)}</td>
                <td className="px-3 py-3 text-slate-600"><p>{purchaseOrder.selection_reason || '-'}</p><p className="mt-1 text-xs">{purchaseOrder.construction_period || '-'} · {purchaseOrder.payment_terms || '-'}</p></td>
              </tr>
            ))}
            {purchaseOrders.length === 0 && <tr><td colSpan={9} className="px-3 py-10 text-center text-slate-500">승인된 정식 발주가 없습니다.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function ChangeHistoryPanel({ changeOrders }: { changeOrders: ChangeOrder[] }) {
  return (
    <div>
      <div className="border-b border-slate-200 pb-4">
        <h3 className="text-lg font-semibold text-slate-950">변경관리 이력</h3>
        <p className="mt-1 text-sm text-slate-500">대표 금액 정정으로 즉시 반영된 현재 금액과, 최초 금액을 보존하기 위한 증감 이력을 관리합니다.</p>
      </div>
      <div className="py-5">
        <div className="overflow-x-auto rounded-md border border-slate-200">
          <table className="min-w-[1050px] text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr><th className="px-3 py-3">변경번호</th><th className="px-3 py-3">일시</th><th className="px-3 py-3">처리자</th><th className="px-3 py-3">사유</th><th className="px-3 py-3">계약 증감</th><th className="px-3 py-3">실행예산 증감</th><th className="px-3 py-3">발주 증감</th><th className="px-3 py-3">연결 항목</th></tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {changeOrders.map((change) => (
                <tr key={change.change_order_id}>
                  <td className="px-3 py-3 font-semibold text-cyan-800">{change.change_order_id}</td>
                  <td className="px-3 py-3 text-slate-600">{formatDate(change.created_at?.slice(0, 10))}</td>
                  <td className="px-3 py-3 text-slate-700">{change.requested_by}</td>
                  <td className="px-3 py-3 text-slate-700">{change.reason}</td>
                  <td className="px-3 py-3 font-semibold">{formatWon(change.contract_delta_amount)}</td>
                  <td className="px-3 py-3">{formatWon(change.budget_delta_amount)}</td>
                  <td className="px-3 py-3">{formatWon(change.purchase_order_delta_amount)}</td>
                  <td className="px-3 py-3 text-slate-600">{change.impacts.map((impact) => `${impact.label} (${formatWon(impact.delta_amount)})`).join(', ')}</td>
                </tr>
              ))}
              {changeOrders.length === 0 && <tr><td colSpan={8} className="px-3 py-10 text-center text-slate-500">등록된 변경 이력이 없습니다.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

function varianceClass(value: number) {
  if (value > 0) return 'text-rose-700'
  if (value < 0) return 'text-blue-700'
  return 'text-slate-900'
}

function SubMetric({
  label,
  value,
  icon,
  accent = 'text-slate-950',
}: {
  label: string
  value: string
  icon: ReactNode
  accent?: string
}) {
  return (
    <article className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between gap-2">
        <p className="text-sm font-medium text-slate-500">{label}</p>
        <span className="text-slate-400 [&>svg]:h-4 [&>svg]:w-4">{icon}</span>
      </div>
      <p className={`mt-3 text-lg font-semibold ${accent}`}>{value}</p>
    </article>
  )
}

function MetricPanel({
  label,
  value,
  helper,
  accent = 'text-slate-950',
}: {
  label: string
  value: string
  helper: string
  accent?: string
}) {
  return (
    <article className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
      <p className="text-sm font-medium text-slate-500">{label}</p>
      <p className={`mt-2 text-xl font-semibold ${accent}`}>{value}</p>
      <p className="mt-1 text-sm text-slate-500">{helper}</p>
    </article>
  )
}

function ApprovalInboxPanel({
  approvals,
  selectedApproval,
  statusFilter,
  typeFilter,
  rejectComment,
  setStatusFilter,
  setTypeFilter,
  setRejectComment,
  loadApprovalDetail,
  approveApproval,
  rejectApproval,
  cancelApproval,
}: {
  approvals: ApprovalItem[]
  selectedApproval: ApprovalItem | null
  statusFilter: string
  typeFilter: string
  rejectComment: string
  setStatusFilter: (value: string) => void
  setTypeFilter: (value: string) => void
  setRejectComment: (value: string) => void
  loadApprovalDetail: (approvalId: string) => void
  approveApproval: (approvalId: string) => void
  rejectApproval: (approvalId: string) => void
  cancelApproval: (approvalId: string) => void
}) {
  const pending = selectedApproval?.status === 'REQUESTED'
  const canCancelApprovedExecutionBudget = selectedApproval?.status === 'APPROVED'
    && selectedApproval.approval_type === 'EXECUTION_BUDGET'

  return (
    <div>
      <div className="flex flex-col gap-4 border-b border-slate-200 pb-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 className="text-lg font-semibold text-slate-950">통합결재함</h3>
          <p className="mt-1 text-sm text-slate-500">실행예산과 발주품의 결재를 한 화면에서 승인하거나 반려합니다.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)} className="input w-36">
            <option value="">전체 상태</option>
            <option value="REQUESTED">결재대기</option>
            <option value="APPROVED">승인</option>
            <option value="REJECTED">반려</option>
            <option value="CANCELLED">승인취소</option>
          </select>
          <select value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)} className="input w-44">
            <option value="">전체 유형</option>
            <option value="EXECUTION_BUDGET">실행예산</option>
            <option value="PURCHASE_ORDER">발주</option>
          </select>
        </div>
      </div>

      <div className="grid gap-5 py-5 xl:grid-cols-[minmax(0,1.4fr)_minmax(360px,0.8fr)]">
        <div className="overflow-x-auto">
          <table className="min-w-[980px] text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-3 py-3">요청일</th>
                <th className="px-3 py-3">프로젝트</th>
                <th className="px-3 py-3">유형</th>
                <th className="px-3 py-3">제목</th>
                <th className="px-3 py-3">요청금액</th>
                <th className="px-3 py-3">증감</th>
                <th className="px-3 py-3">상태</th>
                <th className="px-3 py-3"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {approvals.map((approval, index) => (
                <tr key={approval.approval_id}>
				  <td className="px-3 py-3 text-slate-500">{index + 1}</td>
                  <td className="px-3 py-3 text-slate-600">{formatDateTime(approval.requested_at)}</td>
                  <td className="px-3 py-3 font-semibold text-slate-900">{approval.project_code}</td>
                  <td className="px-3 py-3 text-slate-600">{approval.approval_type}</td>
                  <td className="px-3 py-3 text-slate-600">{approval.title}</td>
                  <td className="px-3 py-3 font-semibold text-slate-900">{formatWon(approval.requested_amount)}</td>
                  <td className={`px-3 py-3 font-semibold ${varianceClass(approval.variance_amount)}`}>{formatWon(approval.variance_amount)}</td>
                  <td className="px-3 py-3 text-slate-600">{approval.status}</td>
                  <td className="px-3 py-3">
                    <button type="button" onClick={() => loadApprovalDetail(approval.approval_id)} className="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50">
                      상세
                    </button>
                  </td>
                </tr>
              ))}
              {approvals.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-3 py-8 text-center text-slate-500">표시할 결재 건이 없습니다.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <aside className="rounded-md border border-slate-200 bg-slate-50 p-4">
          {selectedApproval ? (
            <div>
              <div className="border-b border-slate-200 pb-3">
                <p className="text-xs font-semibold text-slate-500">{selectedApproval.approval_type}</p>
                <h4 className="mt-1 text-base font-semibold text-slate-950">{selectedApproval.title}</h4>
                <p className="mt-1 text-sm text-slate-500">{selectedApproval.project_code} · {selectedApproval.status}</p>
              </div>
              <div className="space-y-2 py-4 text-sm text-slate-700">
                <p>요청금액: <strong>{formatWon(selectedApproval.requested_amount)}</strong></p>
                <p>기준금액: <strong>{formatWon(selectedApproval.previous_amount)}</strong></p>
                <p>증감액: <strong className={varianceClass(selectedApproval.variance_amount)}>{formatWon(selectedApproval.variance_amount)}</strong></p>
                <p>요청자: {selectedApproval.request_user}</p>
                <p>요청사유: {selectedApproval.request_reason || '-'}</p>
                {selectedApproval.comment && <p>처리의견: {selectedApproval.comment}</p>}
                {selectedApproval.cancellation_reason && (
                  <p>승인취소 사유: {selectedApproval.cancellation_reason} ({selectedApproval.cancelled_by || '-'} / {formatDate(selectedApproval.cancelled_at?.slice(0, 10))})</p>
                )}
                {(selectedApproval.attachments ?? []).length > 0 && (
                  <div className="pt-2">
                    <p className="mb-2 font-semibold text-slate-800">결재 첨부파일</p>
                    <div className="divide-y rounded-md border border-slate-200 bg-white">
                      {selectedApproval.attachments?.map((attachment) => (
                        <a
                          key={attachment.attachment_id}
                          href={apiUrl(attachment.download_url)}
                          className="flex items-center justify-between gap-3 px-3 py-2 text-sm text-cyan-800 hover:bg-cyan-50"
                        >
                          <span className="truncate">{attachment.file_name}</span>
                          <Download className="h-4 w-4 shrink-0" aria-hidden="true" />
                        </a>
                      ))}
                    </div>
                  </div>
                )}
              </div>
              {pending && (
                <div className="border-t border-slate-200 pt-4">
                  <textarea value={rejectComment} onChange={(event) => setRejectComment(event.target.value)} placeholder="반려 사유" className="input min-h-24" />
                  <div className="mt-3 flex justify-end gap-2">
                    <button type="button" onClick={() => rejectApproval(selectedApproval.approval_id)} disabled={!rejectComment.trim()} className="rounded-md border border-rose-300 px-3 py-2 text-sm font-semibold text-rose-700 hover:bg-rose-50 disabled:cursor-not-allowed disabled:opacity-40">
                      반려
                    </button>
                    <button type="button" onClick={() => approveApproval(selectedApproval.approval_id)} className="rounded-md bg-cyan-700 px-3 py-2 text-sm font-semibold text-white hover:bg-cyan-800">
                      승인
                    </button>
                  </div>
                </div>
              )}
              {canCancelApprovedExecutionBudget && (
                <div className="border-t border-amber-200 pt-4">
                  <p className="mb-2 text-sm font-semibold text-amber-900">승인취소 후 재작업</p>
                  <textarea value={rejectComment} onChange={(event) => setRejectComment(event.target.value)} placeholder="승인취소 사유" className="input min-h-24" />
                  <div className="mt-3 flex justify-end">
                    <button type="button" onClick={() => cancelApproval(selectedApproval.approval_id)} disabled={!rejectComment.trim()} className="rounded-md border border-amber-400 px-3 py-2 text-sm font-semibold text-amber-800 hover:bg-amber-50 disabled:cursor-not-allowed disabled:opacity-40">
                      승인취소 후 재작업
                    </button>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <p className="py-12 text-center text-sm text-slate-500">결재 건을 선택하세요.</p>
          )}
        </aside>
      </div>
    </div>
  )
}

function MoneyInput({ value, onChange, disabled = false }: { value: number; onChange: (value: string) => void; disabled?: boolean }) {
  return (
    <div className="flex items-center gap-2 rounded-md border border-slate-300 bg-white px-3 py-2 focus-within:border-cyan-700 focus-within:ring-2 focus-within:ring-cyan-700/15">
      <input
        disabled={disabled}
        value={currency.format(Math.round(value || 0))}
        onChange={(event) => onChange(event.target.value)}
        inputMode="numeric"
        className="w-full border-0 bg-transparent text-sm outline-none disabled:cursor-not-allowed"
      />
      <span className="shrink-0 text-xs font-semibold text-slate-400">원</span>
    </div>
  )
}

function CostBreakdownTable({ rows }: { rows: CostBreakdown[] }) {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full text-left text-sm">
        <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
          <tr>
            <th className="px-3 py-3">공종명</th>
            <th className="px-3 py-3">최초 실행예산</th>
            <th className="px-3 py-3">현재 실행예산</th>
            <th className="px-3 py-3">발주금액</th>
            <th className="px-3 py-3">누적 기성</th>
            <th className="px-3 py-3">실제 지급</th>
            <th className="px-3 py-3">예산 대비</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {rows.map((row) => (
            <tr key={row.budget_id}>
              <td className="px-3 py-3 font-semibold text-slate-900">{row.work_category}</td>
              <td className="px-3 py-3 text-slate-600">{formatWon(row.initial_execution_budget)}</td>
              <td className="px-3 py-3 text-slate-600">{formatWon(row.current_execution_budget)}</td>
              <td className="px-3 py-3 text-slate-600">{formatWon(row.purchase_amount)}</td>
              <td className="px-3 py-3 text-slate-600">{formatWon(row.accumulated_completed_amount)}</td>
              <td className="px-3 py-3 text-slate-600">{formatWon(row.actual_paid_amount)}</td>
              <td className={`px-3 py-3 font-semibold ${row.budget_variance > 0 ? 'text-rose-700' : 'text-cyan-700'}`}>
                {formatWon(row.budget_variance)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function ProjectCreationModal({
  form,
  setForm,
  onClose,
  onSubmit,
}: {
  form: ProjectForm
  setForm: (form: ProjectForm) => void
  onClose: () => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}) {
  function update(field: keyof ProjectForm, value: string) {
    setForm({ ...form, [field]: value })
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4">
      <form onSubmit={onSubmit} className="max-h-[92vh] w-full max-w-4xl overflow-auto rounded-md bg-white shadow-xl">
        <div className="sticky top-0 flex items-center justify-between border-b border-slate-200 bg-white px-5 py-4">
          <div>
            <h2 className="text-lg font-semibold text-slate-950">신규 프로젝트 등록</h2>
            <p className="mt-1 text-sm text-slate-500">프로젝트번호는 등록 시 자동 생성됩니다.</p>
          </div>
          <button type="button" onClick={onClose} className="rounded-md p-2 text-slate-500 hover:bg-slate-100" aria-label="닫기">
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>

        <div className="grid gap-4 p-5 md:grid-cols-2">
          <Field label="프로젝트명" required>
            <input required value={form.project_name} onChange={(e) => update('project_name', e.target.value)} placeholder="성가수녀원 리뉴얼공사" className="input" />
          </Field>
          <Field label="고객/발주처">
            <input value={form.client_name} onChange={(e) => update('client_name', e.target.value)} className="input" />
          </Field>
          <Field label="현장주소">
            <input value={form.site_address} onChange={(e) => update('site_address', e.target.value)} className="input" />
          </Field>
          <Field label="계약일">
            <input type="date" value={form.contract_date} onChange={(e) => update('contract_date', e.target.value)} className="input" />
          </Field>
          <Field label="착공일">
            <input type="date" value={form.start_date} onChange={(e) => update('start_date', e.target.value)} className="input" />
          </Field>
          <Field label="준공예정일">
            <input type="date" value={form.end_date} onChange={(e) => update('end_date', e.target.value)} className="input" />
          </Field>
          <Field label="담당 팀장">
            <input value={form.team_leader_id} onChange={(e) => update('team_leader_id', e.target.value)} placeholder="usr_tl_01" className="input" />
          </Field>
          <Field label="담당자">
            <input value={form.manager_id} onChange={(e) => update('manager_id', e.target.value)} placeholder="usr_pm_02" className="input" />
          </Field>
          <Field label="비고">
            <input value={form.note} onChange={(e) => update('note', e.target.value)} className="input" />
          </Field>
        </div>

        <div className="sticky bottom-0 flex justify-end gap-2 border-t border-slate-200 bg-white px-5 py-4">
          <button type="button" onClick={onClose} className="rounded-md border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">
            취소
          </button>
          <button type="submit" className="rounded-md bg-cyan-700 px-4 py-2 text-sm font-semibold text-white hover:bg-cyan-800">
            등록
          </button>
        </div>
      </form>
    </div>
  )
}

function Field({ label, required, children }: { label: string; required?: boolean; children: ReactNode }) {
  return (
    <label className="block text-sm font-semibold text-slate-700">
      {label}{required && <span className="text-rose-600"> *</span>}
      <span className="mt-1 block">{children}</span>
    </label>
  )
}

export default App
