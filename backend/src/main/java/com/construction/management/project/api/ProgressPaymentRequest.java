package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ProgressPaymentRequest {
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
public record Claim(
			@JsonProperty("po_id") String poId,
			Integer degree,
			@JsonProperty("claim_amount") BigDecimal claimAmount
	) {
	}

	@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
public record Payment(
			@JsonProperty("paid_amount") BigDecimal paidAmount,
			@JsonProperty("paid_date") LocalDate paidDate,
			@JsonProperty("account_info") String accountInfo,
			@JsonProperty("receipt_link") String receiptLink
	) {
	}
}
