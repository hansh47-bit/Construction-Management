package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "po_requisitions")
public class PoRequisition {

	@Id
	@Column(name = "requisition_id", nullable = false, length = 64)
	private String requisitionId;

	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "execution_item_id", nullable = false, length = 64)
	private String executionItemId;

	@Column(name = "selected_comparison_id", nullable = false, length = 64)
	private String selectedComparisonId;

	@Column(name = "approved_budget_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal approvedBudgetAmount;

	@Column(name = "po_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal poAmount;

	@Column(name = "budget_variance", nullable = false, precision = 18, scale = 2)
	private BigDecimal budgetVariance;

	@Column(name = "selection_reason", nullable = false, length = 1000)
	private String selectionReason;

	@Column(name = "approval_status", nullable = false)
	private String approvalStatus = ApprovalStatus.REQUESTED.name();

	@Column(name = "requested_by", nullable = false)
	private String requestedBy;

	@Column(name = "approver")
	private String approver;

	@Column(name = "comment", length = 1000)
	private String comment;

	@Column(name = "decided_at")
	private LocalDateTime decidedAt;

	@Column(name = "approval_id", length = 64)
	private String approvalId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected PoRequisition() {
	}

	public PoRequisition(String requisitionId, String projectId, String executionItemId, String selectedComparisonId, BigDecimal approvedBudgetAmount, BigDecimal poAmount, BigDecimal budgetVariance, String selectionReason, String requestedBy) {
		this.requisitionId = requisitionId;
		this.projectId = projectId;
		this.executionItemId = executionItemId;
		this.selectedComparisonId = selectedComparisonId;
		this.approvedBudgetAmount = approvedBudgetAmount;
		this.poAmount = poAmount;
		this.budgetVariance = budgetVariance;
		this.selectionReason = selectionReason;
		this.requestedBy = requestedBy;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	public void linkApproval(String approvalId) {
		this.approvalId = approvalId;
	}

	public void approve(String approver, String comment) {
		this.approvalStatus = ApprovalStatus.APPROVED.name();
		this.approver = approver;
		this.comment = comment;
		this.decidedAt = LocalDateTime.now();
	}

	public void reject(String approver, String comment) {
		this.approvalStatus = ApprovalStatus.REJECTED.name();
		this.approver = approver;
		this.comment = comment;
		this.decidedAt = LocalDateTime.now();
	}

	public String getRequisitionId() { return requisitionId; }
	public String getProjectId() { return projectId; }
	public String getExecutionItemId() { return executionItemId; }
	public String getSelectedComparisonId() { return selectedComparisonId; }
	public BigDecimal getApprovedBudgetAmount() { return approvedBudgetAmount; }
	public BigDecimal getPoAmount() { return poAmount; }
	public BigDecimal getBudgetVariance() { return budgetVariance; }
	public String getSelectionReason() { return selectionReason; }
	public String getApprovalStatus() { return approvalStatus; }
	public String getApprovalId() { return approvalId; }
}
