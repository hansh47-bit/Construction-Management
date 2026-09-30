package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record ExecutionBudgetDetailResponse(
		@JsonProperty("project_id")
		String projectId,
		@JsonProperty("project_code")
		String projectCode,
		@JsonProperty("project_name")
		String projectName,
		@JsonProperty("contract_supply_amount")
		BigDecimal contractSupplyAmount,
		@JsonProperty("target_cost_limit")
		BigDecimal targetCostLimit,
		@JsonProperty("approval_status")
		String approvalStatus,
		@JsonProperty("approval_id")
		String approvalId,
		@JsonProperty("approval_comment")
		String approvalComment,
		@JsonProperty("approved_amount")
		BigDecimal approvedAmount,
		@JsonProperty("total_detail_amount")
		BigDecimal totalDetailAmount,
		@JsonProperty("over_target_limit")
		boolean overTargetLimit,
		@JsonProperty("summary_by_item")
		List<SummaryItem> summaryByItem,
		List<Detail> details
) {
	public record SummaryItem(
			@JsonProperty("budget_item_id")
			String budgetItemId,
			@JsonProperty("category_name")
			String categoryName,
			@JsonProperty("item_name")
			String itemName,
			@JsonProperty("rolled_up_amount")
			BigDecimal rolledUpAmount
	) {
	}

	public record Detail(
			@JsonProperty("detail_id")
			String detailId,
			@JsonProperty("budget_item_id")
			String budgetItemId,
			@JsonProperty("category_name")
			String categoryName,
			@JsonProperty("item_name")
			String itemName,
			@JsonProperty("cost_type")
			String costType,
			@JsonProperty("vendor_description")
			String vendorDescription,
			String unit,
			BigDecimal quantity,
			@JsonProperty("unit_price")
			BigDecimal unitPrice,
			BigDecimal amount,
			@JsonProperty("evidence_link")
			String evidenceLink,
			String note
	) {
	}
}
