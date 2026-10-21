package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProgressPaymentResponse(
		@JsonProperty("project_id") String projectId,
		@JsonProperty("project_code") String projectCode,
		@JsonProperty("revision_number") Integer revisionNumber,
		@JsonProperty("all_payments_approved") boolean allPaymentsApproved,
		@JsonProperty("pending_payment_approval_count") long pendingPaymentApprovalCount,
		List<PurchaseOrderStatus> purchaseOrders
) {
	public record PurchaseOrderStatus(
			@JsonProperty("po_id") String poId,
			@JsonProperty("vendor_name") String vendorName,
			@JsonProperty("execution_item_id") String executionItemId,
			@JsonProperty("work_category") String workCategory,
			@JsonProperty("execution_item_name") String executionItemName,
			@JsonProperty("current_po_amount") BigDecimal currentPoAmount,
			@JsonProperty("approved_claim_total") BigDecimal approvedClaimTotal,
			@JsonProperty("actual_paid_total") BigDecimal actualPaidTotal,
			@JsonProperty("unclaimed_amount") BigDecimal unclaimedAmount,
			@JsonProperty("approved_unpaid_amount") BigDecimal approvedUnpaidAmount,
			@JsonProperty("total_unpaid_balance") BigDecimal totalUnpaidBalance,
			@JsonProperty("payment_request_status") String paymentRequestStatus,
			List<ClaimStatus> claims
	) {
	}

	public record ClaimStatus(
			@JsonProperty("claim_id") String claimId,
			Integer degree,
			@JsonProperty("claim_amount") BigDecimal claimAmount,
			@JsonProperty("approval_status") String approvalStatus,
			@JsonProperty("payment_state") String paymentState,
			@JsonProperty("paid_total") BigDecimal paidTotal,
			@JsonProperty("remaining_payable") BigDecimal remainingPayable,
			List<PaymentStatus> payments
	) {
	}

	public record PaymentStatus(
			@JsonProperty("payment_id") String paymentId,
			@JsonProperty("paid_amount") BigDecimal paidAmount,
			@JsonProperty("paid_date") LocalDate paidDate,
			@JsonProperty("payment_status") String paymentStatus,
			@JsonProperty("account_info") String accountInfo,
			@JsonProperty("receipt_link") String receiptLink
	) {
	}
}
