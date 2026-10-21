package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectRevisionResponse(
		@JsonProperty("revision_id") String revisionId,
		@JsonProperty("project_id") String projectId,
		@JsonProperty("revision_number") int revisionNumber,
		String title,
		String reason,
		String status,
		@JsonProperty("created_at") LocalDateTime createdAt,
		@JsonProperty("base_contract_total") BigDecimal baseContractTotal,
		@JsonProperty("contract_total") BigDecimal contractTotal,
		@JsonProperty("target_profit_rate") BigDecimal targetProfitRate,
		@JsonProperty("base_budget_total") BigDecimal baseBudgetTotal,
		@JsonProperty("budget_total") BigDecimal budgetTotal,
		@JsonProperty("base_contract_items") List<ContractItem> baseContractItems,
		@JsonProperty("contract_items") List<ContractItem> contractItems,
		@JsonProperty("base_budget_details") List<BudgetDetail> baseBudgetDetails,
		@JsonProperty("budget_details") List<BudgetDetail> budgetDetails
) {
	public record ContractItem(
			@JsonProperty("work_category") String workCategory,
			@JsonProperty("quoted_amount") BigDecimal quotedAmount,
			@JsonProperty("contract_amount") BigDecimal contractAmount,
			@JsonProperty("vat_amount") BigDecimal vatAmount,
			String note
	) { }

	public record BudgetDetail(
			@JsonProperty("budget_item_id") String budgetItemId,
			@JsonProperty("category_name") String categoryName,
			@JsonProperty("item_name") String itemName,
			@JsonProperty("cost_type") String costType,
			@JsonProperty("vendor_description") String vendorDescription,
			String unit,
			BigDecimal quantity,
			@JsonProperty("unit_price") BigDecimal unitPrice,
			@JsonProperty("evidence_link") String evidenceLink,
			String note
	) { }
}
