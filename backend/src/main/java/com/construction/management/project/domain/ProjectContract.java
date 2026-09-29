package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "project_contracts")
public class ProjectContract {

	@Id
	@Column(name = "contract_id", nullable = false, length = 64)
	private String contractId;

	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "initial_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal initialAmount = BigDecimal.ZERO;

	@Column(name = "vat_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal vatAmount = BigDecimal.ZERO;

	protected ProjectContract() {
	}

	public ProjectContract(String contractId, String projectId, BigDecimal initialAmount, BigDecimal vatAmount) {
		this.contractId = contractId;
		this.projectId = projectId;
		this.initialAmount = initialAmount == null ? BigDecimal.ZERO : initialAmount;
		this.vatAmount = vatAmount == null ? BigDecimal.ZERO : vatAmount;
	}

	public String getProjectId() {
		return projectId;
	}

	public BigDecimal getInitialAmount() {
		return initialAmount;
	}
}
