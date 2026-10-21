package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponse(
		@JsonProperty("project_id") String projectId,
		@JsonProperty("project_code") String projectCode,
		@JsonProperty("revision_number") Integer revisionNumber,
		List<Item> items
) {
	public record Item(
			@JsonProperty("po_id") String poId,
			@JsonProperty("requisition_id") String requisitionId,
			@JsonProperty("approval_id") String approvalId,
			@JsonProperty("selected_comparison_id") String selectedComparisonId,
			@JsonProperty("execution_item_id") String executionItemId,
			@JsonProperty("work_category") String workCategory,
			@JsonProperty("execution_item_name") String executionItemName,
			@JsonProperty("vendor_name") String vendorName,
			@JsonProperty("initial_po_amount") BigDecimal initialPoAmount,
			@JsonProperty("current_po_amount") BigDecimal currentPoAmount,
			@JsonProperty("approved_execution_budget") BigDecimal approvedExecutionBudget,
			@JsonProperty("budget_variance") BigDecimal budgetVariance,
			@JsonProperty("selection_reason") String selectionReason,
			@JsonProperty("construction_period") String constructionPeriod,
			@JsonProperty("scope_and_notes") String scopeAndNotes,
			@JsonProperty("payment_terms") String paymentTerms,
			@JsonProperty("estimate_file_url") String estimateFileUrl,
			@JsonProperty("approved_at") LocalDateTime approvedAt,
			String approver,
			@JsonProperty("approval_comment") String approvalComment,
			@JsonProperty("approved_claim_total") BigDecimal approvedClaimTotal,
			@JsonProperty("actual_paid_total") BigDecimal actualPaidTotal,
			@JsonProperty("total_unpaid_balance") BigDecimal totalUnpaidBalance
	) {
	}
}
