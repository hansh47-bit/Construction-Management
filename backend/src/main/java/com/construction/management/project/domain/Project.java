package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
public class Project {

	@Id
	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "project_code", nullable = false, unique = true, length = 32)
	private String projectCode;

	@Column(name = "project_name", nullable = false)
	private String projectName;

	@Column(name = "client_name")
	private String clientName;

	@Column(name = "site_address")
	private String siteAddress;

	@Column(name = "contract_date")
	private LocalDate contractDate;

	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Column(name = "team_leader_id")
	private String teamLeaderId;

	@Column(name = "manager_id")
	private String managerId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private ProjectStatus status = ProjectStatus.견적;

	@Column(name = "target_profit_rate", nullable = false, precision = 7, scale = 2)
	private BigDecimal targetProfitRate = BigDecimal.ZERO;

	@Column(name = "note", length = 1000)
	private String note;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Project() {
	}

	public Project(String projectId, String projectCode, String projectName) {
		this.projectId = projectId;
		this.projectCode = projectCode;
		this.projectName = projectName;
	}

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public String getProjectId() {
		return projectId;
	}

	public String getProjectCode() {
		return projectCode;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getClientName() {
		return clientName;
	}

	public String getSiteAddress() {
		return siteAddress;
	}

	public LocalDate getContractDate() {
		return contractDate;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public String getTeamLeaderId() {
		return teamLeaderId;
	}

	public String getManagerId() {
		return managerId;
	}

	public ProjectStatus getStatus() {
		return status;
	}

	public BigDecimal getTargetProfitRate() {
		return targetProfitRate;
	}

	public String getNote() {
		return note;
	}

	public Project updateDetails(
			String clientName,
			String siteAddress,
			LocalDate contractDate,
			LocalDate startDate,
			LocalDate endDate,
			String teamLeaderId,
			String managerId,
			ProjectStatus status,
			BigDecimal targetProfitRate,
			String note
	) {
		this.clientName = clientName;
		this.siteAddress = siteAddress;
		this.contractDate = contractDate;
		this.startDate = startDate;
		this.endDate = endDate;
		this.teamLeaderId = teamLeaderId;
		this.managerId = managerId;
		this.status = status == null ? ProjectStatus.견적 : status;
		this.targetProfitRate = targetProfitRate == null ? BigDecimal.ZERO : targetProfitRate;
		this.note = note;
		return this;
	}

	public void updateTargetProfitRate(BigDecimal targetProfitRate) {
		this.targetProfitRate = targetProfitRate == null ? BigDecimal.ZERO : targetProfitRate;
	}

	public void changeStatus(ProjectStatus status) {
		this.status = status == null ? this.status : status;
	}
}
