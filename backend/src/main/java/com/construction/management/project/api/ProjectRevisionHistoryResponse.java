package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProjectRevisionHistoryResponse(
		@JsonProperty("revision_number") int revisionNumber,
		List<Comparison> comparisons,
		@JsonProperty("purchase_orders") List<PurchaseOrder> purchaseOrders
) {
	public record Comparison(
			@JsonProperty("execution_item_id") String executionItemId,
			@JsonProperty("vendor_name") String vendorName,
			@JsonProperty("quoted_amount") BigDecimal quotedAmount,
			boolean selected,
			@JsonProperty("selection_reason") String selectionReason,
			@JsonProperty("construction_period") String constructionPeriod
	) { }

	public record PurchaseOrder(
			@JsonProperty("po_id") String poId,
			@JsonProperty("execution_item_id") String executionItemId,
			@JsonProperty("vendor_name") String vendorName,
			@JsonProperty("po_amount") BigDecimal poAmount,
			@JsonProperty("approved_budget_amount") BigDecimal approvedBudgetAmount,
			List<Claim> claims
	) { }

	public record Claim(
			@JsonProperty("claim_id") String claimId,
			int degree,
			@JsonProperty("claim_amount") BigDecimal claimAmount,
			@JsonProperty("approval_status") String approvalStatus,
			List<Payment> payments
	) { }

	public record Payment(
			@JsonProperty("payment_id") String paymentId,
			@JsonProperty("paid_amount") BigDecimal paidAmount,
			@JsonProperty("paid_date") LocalDate paidDate,
			@JsonProperty("payment_status") String paymentStatus
	) { }
}
