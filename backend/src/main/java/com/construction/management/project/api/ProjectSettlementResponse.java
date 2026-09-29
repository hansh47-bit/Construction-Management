package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProjectSettlementResponse(
		@JsonProperty("project_id")
		String projectId,
		@JsonProperty("project_code")
		String projectCode,
		@JsonProperty("project_name")
		String projectName,
		AmountSummary contract,
		@JsonProperty("execution_budget")
		AmountSummary executionBudget,
		@JsonProperty("purchase_orders")
		AmountSummary purchaseOrders,
		PaymentSummary payments,
		Profitability profitability,
		@JsonProperty("is_settled")
		boolean settled,
		@JsonProperty("settlement_id")
		String settlementId,
		@JsonProperty("settlement_status")
		String settlementStatus,
		@JsonProperty("settlement_notes")
		String settlementNotes,
		@JsonProperty("unpaid_handling_reason")
		String unpaidHandlingReason,
		@JsonProperty("settled_by")
		String settledBy,
		@JsonProperty("settled_at")
		LocalDateTime settledAt
) {
	public record AmountSummary(
			@JsonProperty("initial_amount")
			BigDecimal initialAmount,
			@JsonProperty("change_amount")
			BigDecimal changeAmount,
			@JsonProperty("final_amount")
			BigDecimal finalAmount
	) {
	}

	public record PaymentSummary(
			@JsonProperty("approved_claims_total")
			BigDecimal approvedClaimsTotal,
			@JsonProperty("actual_paid_total")
			BigDecimal actualPaidTotal,
			@JsonProperty("unpaid_balance")
			BigDecimal unpaidBalance,
			@JsonProperty("uncollected_receivable")
			BigDecimal uncollectedReceivable
	) {
	}

	public record Profitability(
			@JsonProperty("final_cost")
			BigDecimal finalCost,
			@JsonProperty("final_profit")
			BigDecimal finalProfit,
			@JsonProperty("profit_margin_rate")
			BigDecimal profitMarginRate
	) {
	}
}
