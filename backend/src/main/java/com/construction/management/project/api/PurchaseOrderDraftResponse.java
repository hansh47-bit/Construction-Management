package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record PurchaseOrderDraftResponse(
		@JsonProperty("project_code") String projectCode,
		@JsonProperty("execution_item_id") String executionItemId,
		@JsonProperty("work_category") String workCategory,
		@JsonProperty("execution_item_name") String executionItemName,
		@JsonProperty("approved_execution_budget") BigDecimal approvedExecutionBudget,
		@JsonProperty("selected_vendor_name") String selectedVendorName,
		@JsonProperty("proposed_po_amount") BigDecimal proposedPoAmount,
		@JsonProperty("budget_variance") BigDecimal budgetVariance,
		@JsonProperty("variance_type") String varianceType,
		@JsonProperty("display_color") String displayColor,
		@JsonProperty("selection_reason") String selectionReason,
		@JsonProperty("approval_status") String approvalStatus
) {
}
