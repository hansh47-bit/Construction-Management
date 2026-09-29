package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "progress_claims")
public class ProgressClaim {

	@Id
	@Column(name = "claim_id", nullable = false, length = 64)
	private String claimId;

	@Column(name = "po_id", nullable = false, length = 64)
	private String poId;

	@Column(name = "degree", nullable = false)
	private Integer degree;

	@Column(name = "claim_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal claimAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "approval_status", nullable = false)
	private ApprovalStatus approvalStatus = ApprovalStatus.REQUESTED;

	@Column(name = "requested_at", updatable = false)
	private LocalDateTime requestedAt;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	protected ProgressClaim() {
	}

	public ProgressClaim(String claimId, String poId, Integer degree, BigDecimal claimAmount, ApprovalStatus approvalStatus) {
		this.claimId = claimId;
		this.poId = poId;
		this.degree = degree;
		this.claimAmount = claimAmount;
		this.approvalStatus = approvalStatus;
		this.approvedAt = approvalStatus == ApprovalStatus.APPROVED ? LocalDateTime.now() : null;
	}

	@PrePersist
	void onCreate() {
		this.requestedAt = LocalDateTime.now();
	}

	public String getClaimId() {
		return claimId;
	}

	public String getPoId() {
		return poId;
	}

	public BigDecimal getClaimAmount() {
		return claimAmount;
	}

	public Integer getDegree() {
		return degree;
	}

	public ApprovalStatus getApprovalStatus() {
		return approvalStatus;
	}

	public LocalDateTime getApprovedAt() {
		return approvedAt;
	}

	public void approve() {
		this.approvalStatus = ApprovalStatus.APPROVED;
		this.approvedAt = LocalDateTime.now();
	}
}
