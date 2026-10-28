package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
public record ProjectSettlementCompleteRequest(
		@JsonProperty("uncollected_receivable")
		BigDecimal uncollectedReceivable,
		@JsonProperty("settlement_expense")
		BigDecimal settlementExpense,
		@JsonProperty("settlement_site_expense")
		BigDecimal settlementSiteExpense,
		@JsonProperty("settlement_labor_expense")
		BigDecimal settlementLaborExpense,
		@JsonProperty("settlement_tax")
		BigDecimal settlementTax,
		@JsonProperty("settlement_notes")
		String settlementNotes,
		@JsonProperty("force_complete_if_unpaid")
		Boolean forceCompleteIfUnpaid,
		@JsonProperty("unpaid_handling_reason")
		String unpaidHandlingReason,
		@JsonProperty("settled_by")
		String settledBy
) {
}
