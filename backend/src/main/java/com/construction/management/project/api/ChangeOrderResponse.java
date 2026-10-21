package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ChangeOrderResponse(
		@JsonProperty("change_order_id") String changeOrderId,
		@JsonProperty("project_id") String projectId,
		String title,
		String reason,
		@JsonProperty("requested_by") String requestedBy,
		String status,
		@JsonProperty("created_at") LocalDateTime createdAt,
		@JsonProperty("revision_number") Integer revisionNumber,
		@JsonProperty("contract_change_enabled") boolean contractChangeEnabled,
		@JsonProperty("budget_change_enabled") boolean budgetChangeEnabled,
		@JsonProperty("contract_delta_amount") BigDecimal contractDeltaAmount,
		@JsonProperty("budget_delta_amount") BigDecimal budgetDeltaAmount,
		@JsonProperty("purchase_order_delta_amount") BigDecimal purchaseOrderDeltaAmount,
		@JsonProperty("requires_reconciliation") boolean requiresReconciliation,
		@JsonProperty("reconciliation_message") String reconciliationMessage,
		List<Impact> impacts
) {
	public record Impact(String category, @JsonProperty("target_id") String targetId, String label, @JsonProperty("delta_amount") BigDecimal deltaAmount) { }
}
