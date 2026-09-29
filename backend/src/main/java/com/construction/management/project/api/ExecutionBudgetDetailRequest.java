package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record ExecutionBudgetDetailRequest(
		@Valid
		@NotNull
		List<Detail> details
) {
	public record Detail(
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
			@JsonProperty("evidence_link")
			String evidenceLink,
			String note
	) {
	}
}
