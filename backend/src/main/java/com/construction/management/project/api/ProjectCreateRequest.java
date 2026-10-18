package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectCreateRequest(
		@JsonProperty("project_name")
		@NotBlank
		String projectName,
		@JsonProperty("client_name")
		String clientName,
		@JsonProperty("site_address")
		String siteAddress,
		@JsonProperty("contract_date")
		LocalDate contractDate,
		@JsonProperty("start_date")
		LocalDate startDate,
		@JsonProperty("end_date")
		LocalDate endDate,
		@JsonProperty("team_leader_id")
		String teamLeaderId,
		@JsonProperty("manager_id")
		String managerId,
		@JsonProperty("target_profit_rate")
		BigDecimal targetProfitRate,
		String note
) {
}
