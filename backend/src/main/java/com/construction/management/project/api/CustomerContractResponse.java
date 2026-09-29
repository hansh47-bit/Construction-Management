package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record CustomerContractResponse(
		@JsonProperty("project_id")
		String projectId,
		@JsonProperty("project_code")
		String projectCode,
		@JsonProperty("project_name")
		String projectName,
		@JsonProperty("client_name")
		String clientName,
		@JsonProperty("target_profit_rate")
		BigDecimal targetProfitRate,
		@JsonProperty("total_contract_supply_amount")
		BigDecimal totalContractSupplyAmount,
		@JsonProperty("total_vat_amount")
		BigDecimal totalVatAmount,
		@JsonProperty("total_contract_with_vat")
		BigDecimal totalContractWithVat,
		@JsonProperty("target_cost_limit")
		BigDecimal targetCostLimit,
		List<Item> items,
		List<FileResponse> files
) {
	public record Item(
			@JsonProperty("item_id")
			String itemId,
			@JsonProperty("work_category")
			String workCategory,
			@JsonProperty("quoted_amount")
			BigDecimal quotedAmount,
			@JsonProperty("contract_amount")
			BigDecimal contractAmount,
			@JsonProperty("vat_amount")
			BigDecimal vatAmount,
			@JsonProperty("total_amount")
			BigDecimal totalAmount,
			String note
	) {
	}

	public record FileResponse(
			@JsonProperty("file_id")
			String fileId,
			@JsonProperty("file_name")
			String fileName,
			@JsonProperty("file_type")
			String fileType,
			@JsonProperty("download_url")
			String downloadUrl
	) {
	}
}
