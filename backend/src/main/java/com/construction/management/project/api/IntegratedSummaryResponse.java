package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record IntegratedSummaryResponse(
		@JsonProperty("project_info")
		ProjectInfo projectInfo,
		@JsonProperty("flow_summary")
		FlowSummary flowSummary,
		@JsonProperty("profit_and_balance")
		ProfitAndBalance profitAndBalance,
		@JsonProperty("cost_breakdown")
		List<CostBreakdown> costBreakdown
) {
	public record ProjectInfo(
			@JsonProperty("project_id")
			String projectId,
			@JsonProperty("project_code")
			String projectCode,
			@JsonProperty("project_name")
			String projectName,
			@JsonProperty("client_name")
			String clientName,
			@JsonProperty("site_address")
			String siteAddress,
			String status,
			@JsonProperty("team_leader_id")
			String teamLeaderId,
			@JsonProperty("manager_id")
			String managerId,
			@JsonProperty("target_profit_rate")
			BigDecimal targetProfitRate
	) {
	}

	public record FlowSummary(
			@JsonProperty("current_contract_amount")
			BigDecimal currentContractAmount,
			@JsonProperty("current_execution_budget")
			BigDecimal currentExecutionBudget,
			@JsonProperty("current_purchase_amount")
			BigDecimal currentPurchaseAmount,
			@JsonProperty("accumulated_completed_amount")
			BigDecimal accumulatedCompletedAmount,
			@JsonProperty("actual_paid_amount")
			BigDecimal actualPaidAmount
	) {
	}

	public record ProfitAndBalance(
			@JsonProperty("target_cost_limit")
			BigDecimal targetCostLimit,
			@JsonProperty("unpurchased_budget_amount")
			BigDecimal unpurchasedBudgetAmount,
			@JsonProperty("expected_final_cost")
			BigDecimal expectedFinalCost,
			@JsonProperty("expected_profit")
			BigDecimal expectedProfit,
			@JsonProperty("expected_profit_rate")
			BigDecimal expectedProfitRate,
			@JsonProperty("unbilled_amount")
			BigDecimal unbilledAmount,
			@JsonProperty("approved_unpaid_amount")
			BigDecimal approvedUnpaidAmount,
			@JsonProperty("total_unpaid_balance")
			BigDecimal totalUnpaidBalance
	) {
	}

	public record CostBreakdown(
			@JsonProperty("budget_id")
			String budgetId,
			@JsonProperty("work_category")
			String workCategory,
			@JsonProperty("initial_execution_budget")
			BigDecimal initialExecutionBudget,
			@JsonProperty("current_execution_budget")
			BigDecimal currentExecutionBudget,
			@JsonProperty("purchase_amount")
			BigDecimal purchaseAmount,
			@JsonProperty("accumulated_completed_amount")
			BigDecimal accumulatedCompletedAmount,
			@JsonProperty("actual_paid_amount")
			BigDecimal actualPaidAmount,
			@JsonProperty("budget_variance")
			BigDecimal budgetVariance
	) {
	}
}
