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
import java.time.LocalDateTime;

@Entity
@Table(name = "approvals")
public class Approval {

	@Id
	@Column(name = "approval_id", nullable = false, length = 64)
	private String approvalId;

	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "approval_type", nullable = false, length = 64)
	private String approvalType;

	@Column(name = "target_id", nullable = false, length = 128)
	private String targetId;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "requested_amount", precision = 18, scale = 2)
	private BigDecimal requestedAmount = BigDecimal.ZERO;

	@Column(name = "previous_amount", precision = 18, scale = 2)
	private BigDecimal previousAmount = BigDecimal.ZERO;

	@Column(name = "variance_amount", precision = 18, scale = 2)
	private BigDecimal varianceAmount = BigDecimal.ZERO;

	@Column(name = "request_user", nullable = false)
	private String requestUser;

	@Column(name = "request_reason", length = 1000)
	private String requestReason;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private ApprovalStatus status = ApprovalStatus.REQUESTED;

	@Column(name = "approver")
	private String approver;

	@Column(name = "comment", length = 1000)
	private String comment;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "decided_at")
	private LocalDateTime decidedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Approval() {
	}

	public Approval(String approvalId, String projectId, String approvalType, String targetId, String title, BigDecimal requestedAmount, BigDecimal previousAmount, BigDecimal varianceAmount, String requestUser, String requestReason) {
		this.approvalId = approvalId;
		this.projectId = projectId;
		this.approvalType = approvalType;
		this.targetId = targetId;
		this.title = title;
		this.requestedAmount = requestedAmount == null ? BigDecimal.ZERO : requestedAmount;
		this.previousAmount = previousAmount == null ? BigDecimal.ZERO : previousAmount;
		this.varianceAmount = varianceAmount == null ? BigDecimal.ZERO : varianceAmount;
		this.requestUser = requestUser;
		this.requestReason = requestReason;
		this.requestedAt = LocalDateTime.now();
	}

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (requestedAt == null) {
			requestedAt = now;
		}
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public void approve(String approver, String comment) {
		this.status = ApprovalStatus.APPROVED;
		this.approver = approver;
		this.comment = comment;
		this.decidedAt = LocalDateTime.now();
	}

	public void reject(String approver, String comment) {
		this.status = ApprovalStatus.REJECTED;
		this.approver = approver;
		this.comment = comment;
		this.decidedAt = LocalDateTime.now();
	}

	public String getApprovalId() { return approvalId; }
	public String getProjectId() { return projectId; }
	public String getApprovalType() { return approvalType; }
	public String getTargetId() { return targetId; }
	public String getTitle() { return title; }
	public BigDecimal getRequestedAmount() { return requestedAmount; }
	public BigDecimal getPreviousAmount() { return previousAmount; }
	public BigDecimal getVarianceAmount() { return varianceAmount; }
	public String getRequestUser() { return requestUser; }
	public String getRequestReason() { return requestReason; }
	public ApprovalStatus getStatus() { return status; }
	public String getApprover() { return approver; }
	public String getComment() { return comment; }
	public LocalDateTime getRequestedAt() { return requestedAt; }
	public LocalDateTime getDecidedAt() { return decidedAt; }
}
