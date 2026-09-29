package com.construction.management.project.api;

import com.construction.management.project.domain.Project;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectResponse(
		@JsonProperty("project_id")
		String projectId,
		@JsonProperty("project_code")
		String projectCode,
		@JsonProperty("project_name")
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
		String status,
		@JsonProperty("target_profit_rate")
		BigDecimal targetProfitRate,
		String note,
		@JsonProperty("contract_supply_amount")
		BigDecimal contractSupplyAmount,
		@JsonProperty("contract_total_amount")
		BigDecimal contractTotalAmount
) {
	public static ProjectResponse from(Project project) {
		return from(project, BigDecimal.ZERO, BigDecimal.ZERO);
	}

	public static ProjectResponse from(Project project, BigDecimal contractSupplyAmount, BigDecimal contractTotalAmount) {
		return new ProjectResponse(
				project.getProjectId(),
				project.getProjectCode(),
				project.getProjectName(),
				project.getClientName(),
				project.getSiteAddress(),
				project.getContractDate(),
				project.getStartDate(),
				project.getEndDate(),
				project.getTeamLeaderId(),
				project.getManagerId(),
				project.getStatus().name(),
				project.getTargetProfitRate(),
				project.getNote(),
				contractSupplyAmount,
				contractTotalAmount
		);
	}
}
