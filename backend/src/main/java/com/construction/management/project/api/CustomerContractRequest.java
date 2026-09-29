package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record CustomerContractRequest(
		@JsonProperty("target_profit_rate")
		BigDecimal targetProfitRate,
		@Valid
		@NotNull
		List<Item> items
) {
	public record Item(
			@JsonProperty("work_category")
			String workCategory,
			@JsonProperty("quoted_amount")
			BigDecimal quotedAmount,
			@JsonProperty("contract_amount")
			BigDecimal contractAmount,
			@JsonProperty("vat_amount")
			BigDecimal vatAmount,
			String note
	) {
	}
}
