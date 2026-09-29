package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record VendorComparisonRequest(@Valid @NotNull List<Item> comparisons) {
	public record Item(
			@JsonProperty("comparison_code") String comparisonCode,
			@JsonProperty("vendor_name") String vendorName,
			@JsonProperty("quoted_amount") BigDecimal quotedAmount,
			@JsonProperty("vat_type") String vatType,
			@JsonProperty("construction_period") String constructionPeriod,
			@JsonProperty("scope_and_notes") String scopeAndNotes,
			@JsonProperty("payment_terms") String paymentTerms,
			@JsonProperty("estimate_file_url") String estimateFileUrl,
			@JsonProperty("is_selected") boolean selected,
			@JsonProperty("selection_reason") String selectionReason
	) {
	}
}
