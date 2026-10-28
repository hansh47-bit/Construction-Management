export type Project = {
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

export type FlowSummary = { current_contract_amount: number; current_execution_budget: number; current_purchase_amount: number; accumulated_completed_amount: number; actual_paid_amount: number }
export type ProfitAndBalance = { target_cost_limit: number; unpurchased_budget_amount: number; unpurchased_procurement_budget_amount: number; unexecuted_direct_settlement_amount: number; expected_final_cost: number; expected_profit: number; expected_profit_rate: number; unbilled_amount: number; approved_unpaid_amount: number; total_unpaid_balance: number }
export type CostBreakdown = { budget_id: string; work_category: string; initial_execution_budget: number; current_execution_budget: number; purchase_amount: number; accumulated_completed_amount: number; actual_paid_amount: number; budget_variance: number }
export type IntegratedSummary = { project_info: Project; flow_summary: FlowSummary; profit_and_balance: ProfitAndBalance; cost_breakdown: CostBreakdown[] }

export type CustomerContractItem = { item_id?: string; work_category: string; quoted_amount: number; contract_amount: number; vat_amount: number; total_amount?: number; note?: string }
export type ContractFile = { file_id: string; file_name: string; file_type: string; download_url: string }
export type CustomerContract = { project_id: string; project_code: string; project_name: string; client_name?: string; target_profit_rate: number; total_contract_supply_amount: number; current_contract_supply_amount: number; total_vat_amount: number; total_contract_with_vat: number; current_contract_with_vat: number; target_cost_limit: number; items: CustomerContractItem[]; files: ContractFile[] }

export type ExecutionBudgetDetail = { detail_id?: string; budget_item_id: string; category_name: string; item_name: string; cost_type: string; vendor_description?: string; unit?: string; quantity: number; unit_price: number; amount?: number; evidence_link?: string; note?: string }
export type ExecutionBudgetSummaryItem = { budget_item_id: string; category_name: string; item_name: string; approval_status: string; purchase_order_status: string; procurement_required: boolean; rolled_up_amount: number }
export type ExecutionBudgetDetailResponse = { project_id: string; project_code: string; project_name: string; contract_supply_amount: number; target_cost_limit: number; approval_status: string; approval_id?: string; approval_comment?: string; execution_budget_rewrite_available: boolean; applied_revision_id?: string; applied_revision_number?: number; applied_revision_status?: string; approved_amount?: number; total_detail_amount: number; over_target_limit: boolean; summary_by_item: ExecutionBudgetSummaryItem[]; details: ExecutionBudgetDetail[] }

export type VendorComparisonItem = { comparison_id?: string; comparison_code?: string; vendor_name: string; quoted_amount: number; vat_type: string; construction_period?: string; scope_and_notes?: string; payment_terms?: string; estimate_file_url?: string; attachments?: ApprovalAttachment[]; is_selected: boolean; selection_reason?: string; budget_variance?: number }
export type VendorComparisonResponse = { project_id: string; project_code: string; execution_item_id: string; work_category: string; execution_item_name: string; approved_execution_budget: number; comparisons: VendorComparisonItem[] }
export type PurchaseOrderDraft = { project_code: string; execution_item_id: string; work_category: string; execution_item_name: string; approved_execution_budget: number; selected_vendor_name: string; proposed_po_amount: number; budget_variance: number; variance_type: string; display_color: string; selection_reason: string; approval_status: string; po_id?: string; approved_at?: string; approver?: string }
export type PurchaseOrderHistoryItem = { po_id: string; requisition_id?: string; approval_id?: string; selected_comparison_id?: string; execution_item_id: string; work_category: string; execution_item_name: string; vendor_name: string; initial_po_amount: number; current_po_amount: number; approved_execution_budget: number; budget_variance: number; selection_reason?: string; construction_period?: string; scope_and_notes?: string; payment_terms?: string; estimate_file_url?: string; approved_at?: string; approver?: string; approval_comment?: string; approved_claim_total: number; actual_paid_total: number; total_unpaid_balance: number }
export type PurchaseOrderHistoryResponse = { project_id: string; project_code: string; revision_number?: number; items: PurchaseOrderHistoryItem[] }

export type ChangeOrderImpact = { category: string; target_id: string; label: string; delta_amount: number }
export type ChangeOrder = { change_order_id: string; project_id: string; title: string; reason: string; requested_by: string; status: string; created_at: string; revision_number?: number; contract_change_enabled?: boolean; budget_change_enabled?: boolean; contract_delta_amount: number; budget_delta_amount: number; purchase_order_delta_amount: number; requires_reconciliation?: boolean; reconciliation_message?: string; impacts: ChangeOrderImpact[] }
export type ProjectRevisionContractItem = { work_category: string; quoted_amount: number; contract_amount: number; vat_amount: number; note?: string }
export type ProjectRevisionBudgetDetail = { budget_item_id: string; category_name: string; item_name: string; cost_type: string; vendor_description?: string; unit?: string; quantity: number; unit_price: number; evidence_link?: string; note?: string }
export type ProjectRevision = { revision_id: string; project_id: string; revision_number: number; title: string; reason: string; status: string; created_at: string; base_contract_total: number; contract_total: number; target_profit_rate: number; base_budget_total: number; budget_total: number; base_contract_items: ProjectRevisionContractItem[]; contract_items: ProjectRevisionContractItem[]; base_budget_details: ProjectRevisionBudgetDetail[]; budget_details: ProjectRevisionBudgetDetail[] }
export type ProjectRevisionHistory = { revision_number: number; comparisons: { execution_item_id: string; vendor_name: string; quoted_amount: number; selected: boolean; selection_reason?: string; construction_period?: string }[]; purchase_orders: { po_id: string; execution_item_id: string; vendor_name: string; po_amount: number; approved_budget_amount: number; claims: { claim_id: string; degree: number; claim_amount: number; approval_status: string; payments: { payment_id: string; paid_amount: number; paid_date: string; payment_status: string }[] }[] }[] }

export type ApprovalAttachment = { attachment_id: string; file_name: string; content_type?: string; download_url: string }
export type ExecutionBudgetApprovalDetail = { contract_supply_amount: number; target_cost_limit: number; total_detail_amount: number; over_target_limit: boolean; summary_by_item: ExecutionBudgetSummaryItem[]; details: ExecutionBudgetDetail[] }
export type PurchaseOrderApprovalDetail = { po_id?: string; execution_item_id: string; work_category: string; execution_item_name: string; approved_execution_budget: number; selected_vendor_name: string; proposed_po_amount: number; budget_variance: number; selection_reason: string; comparisons: VendorComparisonItem[] }
export type ProgressClaimApprovalDetail = { claim_id: string; po_id: string; vendor_name: string; execution_item_id: string; work_category?: string; execution_item_name?: string; degree: number; claim_amount: number; approved_claim_total: number }
export type FinalSettlementApprovalDetail = { final_contract_amount: number; final_execution_budget: number; final_purchase_order_amount: number; settlement_expense: number; settlement_site_expense: number; settlement_labor_expense: number; settlement_tax: number; unpaid_balance: number; final_cost: number; final_profit: number; profit_margin_rate: number }
  export type ApprovalDetail = ExecutionBudgetApprovalDetail | PurchaseOrderApprovalDetail | ProgressClaimApprovalDetail | FinalSettlementApprovalDetail
export type ApprovalItem = { approval_id: string; project_id: string; project_code: string; project_name: string; approval_type: string; target_id: string; title: string; requested_amount: number; previous_amount: number; variance_amount: number; request_user: string; request_reason?: string; status: string; approver?: string; comment?: string; requested_at?: string; decided_at?: string; cancelled_by?: string; cancellation_reason?: string; cancelled_at?: string; detail?: ApprovalDetail; attachments?: ApprovalAttachment[] }

export type PaymentStatus = { payment_id: string; paid_amount: number; paid_date: string; payment_status: string; account_info?: string; receipt_link?: string }
export type ClaimStatus = { claim_id: string; degree: number; claim_amount: number; approval_status: string; payment_state: string; paid_total: number; remaining_payable: number; payments: PaymentStatus[] }
export type PurchaseOrderPaymentStatus = { po_id: string; vendor_name: string; execution_item_id?: string; work_category?: string; execution_item_name?: string; current_po_amount: number; approved_claim_total: number; actual_paid_total: number; unclaimed_amount: number; approved_unpaid_amount: number; total_unpaid_balance: number; payment_request_status: string; claims: ClaimStatus[] }
export type ProgressPaymentResponse = { project_id: string; project_code: string; revision_number?: number; all_payments_approved: boolean; pending_payment_approval_count: number; purchaseOrders: PurchaseOrderPaymentStatus[] }

export type SettlementAmountSummary = { initial_amount: number; change_amount: number; final_amount: number }
export type ProjectSettlement = { project_id: string; project_code: string; project_name: string; revision_number?: number; contract: SettlementAmountSummary; execution_budget: SettlementAmountSummary; purchase_orders: SettlementAmountSummary; settlement_expense: number; settlement_site_expense: number; approved_site_expense_amount: number; settlement_labor_expense: number; approved_labor_expense_amount: number; approved_direct_settlement_amount: number; settlement_tax: number; payments: { approved_claims_total: number; actual_paid_total: number; unpaid_balance: number; uncollected_receivable: number }; profitability: { final_cost: number; final_profit: number; profit_margin_rate: number }; is_settled: boolean; settlement_id?: string; settlement_status?: string; settlement_notes?: string; unpaid_handling_reason?: string; settled_by?: string; settled_at?: string }

export type ProjectForm = { project_name: string; client_name: string; site_address: string; contract_date: string; start_date: string; end_date: string; team_leader_id: string; manager_id: string; target_profit_rate: string; note: string }

export type DailyLogLaborEntry = { id?: number; trade_name: string; workforce_type?: string; vendor_name?: string; headcount: number; note?: string }
export type DailyLogEquipmentEntry = { id?: number; equipment_name: string; quantity: number; note?: string }
export type DailyLogMaterialEntry = { id?: number; entry_type: 'INBOUND' | 'WASTE_OUTBOUND'; item_name: string; quantity?: number; unit?: string; note?: string }
export type DailyLogPhoto = { photo_id: string; phase: 'BEFORE' | 'DURING' | 'AFTER'; file_name: string; mime_type: string; file_size: number; caption?: string; download_url: string; uploaded_at?: string }
export type DailyLog = { daily_log_id: string; project_id: string; log_date: string; weather_condition?: string; temperature_c?: number; site_manager_name: string; today_work_content?: string; tomorrow_plan?: string; safety_training_completed: boolean; safety_training_note?: string; complaint_note?: string; design_change_issue_note?: string; other_note?: string; status: 'DRAFT' | 'SHARED' | 'ARCHIVED' | 'DELETED'; created_by?: string; created_at?: string; updated_at?: string; labor_entries: DailyLogLaborEntry[]; equipment_entries: DailyLogEquipmentEntry[]; material_entries: DailyLogMaterialEntry[]; photos: DailyLogPhoto[] }
export type DailyLogPayload = Omit<DailyLog, 'daily_log_id' | 'project_id' | 'created_at' | 'updated_at' | 'photos'>

export type MeetingAttendee = { id?: number; organization_type: 'CLIENT' | 'CONTRACTOR' | 'VENDOR' | 'OTHER'; organization_name: string; attendee_name: string; position_name?: string; contact?: string }
export type MeetingActionItem = { id?: number; action_content: string; assignee_name?: string; assignee_user_id?: string; due_date?: string; status: 'TODO' | 'IN_PROGRESS' | 'DONE'; completed_at?: string }
export type MeetingAttachment = { attachment_id: string; file_name: string; mime_type: string; file_size: number; download_url: string; uploaded_at?: string }
export type MeetingMinutes = { meeting_id: string; project_id: string; meeting_at: string; location?: string; subject: string; agenda_content?: string; decision_content?: string; status: 'DRAFT' | 'SHARED' | 'ARCHIVED' | 'DELETED'; created_by?: string; created_at?: string; updated_at?: string; open_action_item_count: number; attendees: MeetingAttendee[]; action_items: MeetingActionItem[]; attachments: MeetingAttachment[] }
export type MeetingMinutesPayload = Omit<MeetingMinutes, 'meeting_id' | 'project_id' | 'created_at' | 'updated_at' | 'open_action_item_count' | 'attachments'>
