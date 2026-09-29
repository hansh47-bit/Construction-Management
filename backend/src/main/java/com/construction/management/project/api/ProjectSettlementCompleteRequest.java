package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ProjectSettlementCompleteRequest(
		@JsonProperty("uncollected_receivable")
		BigDecimal uncollectedReceivable,
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
