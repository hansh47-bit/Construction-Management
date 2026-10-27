package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "change_orders")
public class ChangeOrder {

	@Id
	@Column(name = "change_order_id", nullable = false, length = 64)
	private String changeOrderId;

	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "title", nullable = false, length = 255)
	private String title;

	@Column(name = "reason", nullable = false, length = 1000)
	private String reason;

	@Column(name = "requested_by", nullable = false, length = 255)
	private String requestedBy;

	@Column(name = "status", nullable = false, length = 32)
	private String status;

	@Column(name = "revision_number")
	private Integer revisionNumber;

	@Column(name = "contract_change_enabled", nullable = false)
	private boolean contractChangeEnabled = true;

	@Column(name = "budget_change_enabled", nullable = false)
	private boolean budgetChangeEnabled = true;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	protected ChangeOrder() { }

	public ChangeOrder(String changeOrderId, String projectId, String title, String reason, String requestedBy) {
		this(changeOrderId, projectId, title, reason, requestedBy, null, true, true);
	}

	public ChangeOrder(
			String changeOrderId,
			String projectId,
			String title,
			String reason,
			String requestedBy,
			Integer revisionNumber,
			boolean contractChangeEnabled,
			boolean budgetChangeEnabled
	) {
		this.changeOrderId = changeOrderId;
		this.projectId = projectId;
		this.title = title;
		this.reason = reason;
		this.requestedBy = requestedBy;
		this.revisionNumber = revisionNumber;
		this.contractChangeEnabled = contractChangeEnabled;
		this.budgetChangeEnabled = budgetChangeEnabled;
		this.status = "DRAFT";
		this.createdAt = LocalDateTime.now();
	}

	public String getChangeOrderId() { return changeOrderId; }
	public String getProjectId() { return projectId; }
	public String getTitle() { return title; }
	public String getReason() { return reason; }
	public String getRequestedBy() { return requestedBy; }
	public String getStatus() { return status; }
	public Integer getRevisionNumber() { return revisionNumber; }
	public boolean isContractChangeEnabled() { return contractChangeEnabled; }
	public boolean isBudgetChangeEnabled() { return budgetChangeEnabled; }
	public boolean hasContractChange() { return revisionNumber == null || contractChangeEnabled; }
	public boolean hasBudgetChange() { return revisionNumber == null || budgetChangeEnabled; }
	public LocalDateTime getCreatedAt() { return createdAt; }

	public void markReconciliationRequired() {
		this.status = "RECONCILIATION_REQUIRED";
	}

	public void markRequested() { this.status = "REQUESTED"; }
	public void markPartiallyApproved() { this.status = "PARTIALLY_APPROVED"; }
	public void markApproved() { this.status = "APPROVED"; }
	public void markRejected() { this.status = "REJECTED"; }
	public void markCancelled() { this.status = "CANCELED"; }
}
