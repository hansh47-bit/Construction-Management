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
public class Approval extends com.construction.management.auth.AuthoredEntity {

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

	@Enumerated(EnumType.STRING)
	private ApprovalStage approvalStage;
	private String requesterPosition;
	private String teamLeaderId;
	private String teamLeaderName;
	private String teamLeaderApprovedBy;
	private String teamLeaderApprovedUserId;
	private LocalDateTime teamLeaderApprovedAt;
	@Column(length = 1000)
	private String teamLeaderComment;

	@Column(name = "approver")
	private String approver;
	private String approverUserId;
	private String cancelledUserId;

	@Column(name = "comment", length = 1000)
	private String comment;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "decided_at")
	private LocalDateTime decidedAt;

	@Column(name = "cancelled_by")
	private String cancelledBy;

	@Column(name = "cancellation_reason", length = 1000)
	private String cancellationReason;

	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;

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
		this.requestUser = com.construction.management.auth.CurrentActor.optionalId() == null ? requestUser : com.construction.management.auth.CurrentActor.name();
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
		if (status != ApprovalStatus.REQUESTED || getApprovalStage() != ApprovalStage.CEO) {
			throw new IllegalStateException("팀장 승인 후 대표 결재 대기 상태에서만 최종 승인할 수 있습니다.");
		}
		this.status = ApprovalStatus.APPROVED;
		this.approvalStage = ApprovalStage.COMPLETE;
		this.approver = com.construction.management.auth.CurrentActor.name();
		this.approverUserId = com.construction.management.auth.CurrentActor.id();
		this.comment = comment;
		this.decidedAt = LocalDateTime.now();
	}

	public void configureRoute(String requesterPosition, String teamLeaderId, String teamLeaderName) {
		if (this.approvalStage != null) throw new IllegalStateException("이미 설정된 결재선은 변경할 수 없습니다.");
		this.requesterPosition = requesterPosition;
		this.teamLeaderId = teamLeaderId;
		this.teamLeaderName = teamLeaderName;
		this.approvalStage = "STAFF".equals(requesterPosition) ? ApprovalStage.TEAM_LEADER : ApprovalStage.CEO;
	}

	public void approveByTeamLeader(String comment) {
		if (status != ApprovalStatus.REQUESTED || getApprovalStage() != ApprovalStage.TEAM_LEADER
				|| !java.util.Objects.equals(teamLeaderId, com.construction.management.auth.CurrentActor.id())) {
			throw new IllegalStateException("지정된 팀장만 1차 결재를 처리할 수 있습니다.");
		}
		this.teamLeaderApprovedBy = com.construction.management.auth.CurrentActor.name();
		this.teamLeaderApprovedUserId = com.construction.management.auth.CurrentActor.id();
		this.teamLeaderApprovedAt = LocalDateTime.now();
		this.teamLeaderComment = comment;
		this.approvalStage = ApprovalStage.CEO;
	}

	public void reject(String approver, String comment) {
		this.status = ApprovalStatus.REJECTED;
		this.approver = com.construction.management.auth.CurrentActor.name();
		this.approverUserId = com.construction.management.auth.CurrentActor.id();
		this.comment = comment;
		this.decidedAt = LocalDateTime.now();
	}

	public void cancel(String cancelledBy, String cancellationReason) {
		this.status = ApprovalStatus.CANCELLED;
		this.cancelledBy = com.construction.management.auth.CurrentActor.name();
		this.cancelledUserId = com.construction.management.auth.CurrentActor.id();
		this.cancellationReason = cancellationReason;
		this.cancelledAt = LocalDateTime.now();
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
	public ApprovalStage getApprovalStage() { return approvalStage == null ? (status == ApprovalStatus.APPROVED ? ApprovalStage.COMPLETE : ApprovalStage.CEO) : approvalStage; }
	public String getRequesterPosition() { return requesterPosition; }
	public String getTeamLeaderId() { return teamLeaderId; }
	public String getTeamLeaderName() { return teamLeaderName; }
	public String getTeamLeaderApprovedBy() { return teamLeaderApprovedBy; }
	public String getTeamLeaderApprovedUserId() { return teamLeaderApprovedUserId; }
	public LocalDateTime getTeamLeaderApprovedAt() { return teamLeaderApprovedAt; }
	public String getTeamLeaderComment() { return teamLeaderComment; }
	public String getApproverUserId() { return approverUserId; }
	public String getCancelledUserId() { return cancelledUserId; }
	public String getApprover() { return approver; }
	public String getComment() { return comment; }
	public LocalDateTime getRequestedAt() { return requestedAt; }
	public LocalDateTime getDecidedAt() { return decidedAt; }
	public String getCancelledBy() { return cancelledBy; }
	public String getCancellationReason() { return cancellationReason; }
	public LocalDateTime getCancelledAt() { return cancelledAt; }
}
