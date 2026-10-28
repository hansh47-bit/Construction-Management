package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
public record ExecutiveCorrectionRequest(
		@NotNull @JsonProperty("target_contract_supply_amount") BigDecimal targetContractSupplyAmount,
		@NotBlank String reason,
		@JsonProperty("apply_execution_budget") Boolean applyExecutionBudget,
		@JsonProperty("apply_purchase_orders") Boolean applyPurchaseOrders
) { }
