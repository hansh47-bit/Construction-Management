package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "budget_changes")
public class BudgetChange {

	@Id
	@Column(name = "change_id", nullable = false, length = 64)
	private String changeId;

	@Column(name = "budget_id", nullable = false, length = 64)
	private String budgetId;

	@Column(name = "change_code")
	private String changeCode;

	@Column(name = "change_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal changeAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "approval_status", nullable = false)
	private ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;

	protected BudgetChange() {
	}

	public BudgetChange(String changeId, String budgetId, String changeCode, BigDecimal changeAmount, ApprovalStatus approvalStatus) {
		this.changeId = changeId;
		this.budgetId = budgetId;
		this.changeCode = changeCode;
		this.changeAmount = changeAmount;
		this.approvalStatus = approvalStatus;
	}

	public BigDecimal getChangeAmount() {
		return changeAmount;
	}

	public String getBudgetId() {
		return budgetId;
	}
}
