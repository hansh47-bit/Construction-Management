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
public class BudgetChange extends com.construction.management.auth.AuthoredEntity {

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

	@Column(name = "approval_id", length = 64)
	private String approvalId;

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

	public String getChangeId() { return changeId; }
	public String getChangeCode() { return changeCode; }
	public ApprovalStatus getApprovalStatus() { return approvalStatus; }
	public String getApprovalId() { return approvalId; }
	public void linkApproval(String approvalId) { this.approvalId = approvalId; this.approvalStatus = ApprovalStatus.REQUESTED; }
	public void approve() { this.approvalStatus = ApprovalStatus.APPROVED; }
	public void reject() { this.approvalStatus = ApprovalStatus.REJECTED; }
}
