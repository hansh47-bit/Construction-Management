package com.construction.management.project.api;

import com.construction.management.project.domain.ProjectStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectCreateRequest(
		@JsonProperty("project_code")
		String projectCode,
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
		ProjectStatus status,
		@JsonProperty("target_profit_rate")
		BigDecimal targetProfitRate,
		String note
) {
}
