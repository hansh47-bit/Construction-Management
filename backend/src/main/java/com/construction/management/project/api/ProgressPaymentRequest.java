package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ProgressPaymentRequest {
	public record Claim(
			@JsonProperty("po_id") String poId,
			Integer degree,
			@JsonProperty("claim_amount") BigDecimal claimAmount
	) {
	}

	public record Payment(
			@JsonProperty("paid_amount") BigDecimal paidAmount,
			@JsonProperty("paid_date") LocalDate paidDate,
			@JsonProperty("account_info") String accountInfo,
			@JsonProperty("receipt_link") String receiptLink
	) {
	}
}
