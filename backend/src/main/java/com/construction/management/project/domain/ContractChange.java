package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract_changes")
public class ContractChange extends com.construction.management.auth.AuthoredEntity {

	@Id
	@Column(name = "change_id", nullable = false, length = 64)
	private String changeId;

	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "change_code")
	private String changeCode;

	@Column(name = "change_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal changeAmount;

	@Column(name = "reason")
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(name = "approval_status", nullable = false)
	private ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@Column(name = "approval_id", length = 64)
	private String approvalId;

	protected ContractChange() {
	}

	public ContractChange(String changeId, String projectId, String changeCode, BigDecimal changeAmount, ApprovalStatus approvalStatus) {
		this(changeId, projectId, changeCode, changeAmount, null, approvalStatus);
	}

	public ContractChange(String changeId, String projectId, String changeCode, BigDecimal changeAmount, String reason, ApprovalStatus approvalStatus) {
		this.changeId = changeId;
		this.projectId = projectId;
		this.changeCode = changeCode;
		this.changeAmount = changeAmount;
		this.reason = reason;
		this.approvalStatus = approvalStatus;
		this.approvedAt = approvalStatus == ApprovalStatus.APPROVED ? LocalDateTime.now() : null;
	}

	public BigDecimal getChangeAmount() {
		return changeAmount;
	}

	public String getChangeId() { return changeId; }
	public String getChangeCode() { return changeCode; }
	public String getReason() { return reason; }
	public ApprovalStatus getApprovalStatus() { return approvalStatus; }
	public LocalDateTime getApprovedAt() { return approvedAt; }
	public String getApprovalId() { return approvalId; }
	public void linkApproval(String approvalId) { this.approvalId = approvalId; this.approvalStatus = ApprovalStatus.REQUESTED; }
	public void approve() { this.approvalStatus = ApprovalStatus.APPROVED; this.approvedAt = LocalDateTime.now(); }
	public void reject() { this.approvalStatus = ApprovalStatus.REJECTED; }
}
