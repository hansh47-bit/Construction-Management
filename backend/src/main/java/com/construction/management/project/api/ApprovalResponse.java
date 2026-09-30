package com.construction.management.project.api;

import com.construction.management.project.domain.Approval;
import com.construction.management.project.domain.Project;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ApprovalResponse(
		@JsonProperty("approval_id")
		String approvalId,
		@JsonProperty("project_id")
		String projectId,
		@JsonProperty("project_code")
		String projectCode,
		@JsonProperty("project_name")
		String projectName,
		@JsonProperty("approval_type")
		String approvalType,
		@JsonProperty("target_id")
		String targetId,
		String title,
		@JsonProperty("requested_amount")
		BigDecimal requestedAmount,
		@JsonProperty("previous_amount")
		BigDecimal previousAmount,
		@JsonProperty("variance_amount")
		BigDecimal varianceAmount,
		@JsonProperty("request_user")
		String requestUser,
		@JsonProperty("request_reason")
		String requestReason,
		String status,
		String approver,
		String comment,
		@JsonProperty("requested_at")
		LocalDateTime requestedAt,
		@JsonProperty("decided_at")
		LocalDateTime decidedAt,
		Object detail
) {
	public static ApprovalResponse from(Approval approval, Project project) {
		return from(approval, project, null);
	}

	public static ApprovalResponse from(Approval approval, Project project, Object detail) {
		return new ApprovalResponse(
				approval.getApprovalId(),
				approval.getProjectId(),
				project.getProjectCode(),
				project.getProjectName(),
				approval.getApprovalType(),
				approval.getTargetId(),
				approval.getTitle(),
				approval.getRequestedAmount(),
				approval.getPreviousAmount(),
				approval.getVarianceAmount(),
				approval.getRequestUser(),
				approval.getRequestReason(),
				approval.getStatus().name(),
				approval.getApprover(),
				approval.getComment(),
				approval.getRequestedAt(),
				approval.getDecidedAt(),
				detail
		);
	}

	public record ExecutionBudgetDetail(
			@JsonProperty("contract_supply_amount")
			BigDecimal contractSupplyAmount,
			@JsonProperty("target_cost_limit")
			BigDecimal targetCostLimit,
			@JsonProperty("total_detail_amount")
			BigDecimal totalDetailAmount,
			@JsonProperty("over_target_limit")
			boolean overTargetLimit,
			@JsonProperty("summary_by_item")
			List<ExecutionBudgetDetailResponse.SummaryItem> summaryByItem,
			List<ExecutionBudgetDetailResponse.Detail> details
	) {
	}

	public record PurchaseOrderDetail(
			@JsonProperty("execution_item_id")
			String executionItemId,
			@JsonProperty("work_category")
			String workCategory,
			@JsonProperty("execution_item_name")
			String executionItemName,
			@JsonProperty("approved_execution_budget")
			BigDecimal approvedExecutionBudget,
			@JsonProperty("selected_vendor_name")
			String selectedVendorName,
			@JsonProperty("proposed_po_amount")
			BigDecimal proposedPoAmount,
			@JsonProperty("budget_variance")
			BigDecimal budgetVariance,
			@JsonProperty("selection_reason")
			String selectionReason,
			List<VendorComparisonResponse.Item> comparisons
	) {
	}
}
