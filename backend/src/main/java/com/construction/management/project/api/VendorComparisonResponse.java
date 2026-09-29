package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record VendorComparisonResponse(
		@JsonProperty("project_id") String projectId,
		@JsonProperty("project_code") String projectCode,
		@JsonProperty("execution_item_id") String executionItemId,
		@JsonProperty("work_category") String workCategory,
		@JsonProperty("execution_item_name") String executionItemName,
		@JsonProperty("approved_execution_budget") BigDecimal approvedExecutionBudget,
		List<Item> comparisons
) {
	public record Item(
			@JsonProperty("comparison_id") String comparisonId,
			@JsonProperty("comparison_code") String comparisonCode,
			@JsonProperty("vendor_name") String vendorName,
			@JsonProperty("quoted_amount") BigDecimal quotedAmount,
			@JsonProperty("vat_type") String vatType,
			@JsonProperty("construction_period") String constructionPeriod,
			@JsonProperty("scope_and_notes") String scopeAndNotes,
			@JsonProperty("payment_terms") String paymentTerms,
			@JsonProperty("estimate_file_url") String estimateFileUrl,
			@JsonProperty("is_selected") boolean selected,
			@JsonProperty("selection_reason") String selectionReason,
			@JsonProperty("budget_variance") BigDecimal budgetVariance
	) {
	}
}
