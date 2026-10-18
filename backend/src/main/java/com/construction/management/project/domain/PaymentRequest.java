package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_requests")
public class PaymentRequest {
	@Id
	@Column(name = "payment_request_id", nullable = false, length = 64)
	private String paymentRequestId;

	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "po_id", nullable = false, length = 64)
	private String poId;

	@Column(name = "claim_ids", nullable = false, length = 2000)
	private String claimIds;

	@Column(name = "requested_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal requestedAmount;

	@Column(name = "approval_status", nullable = false)
	private String approvalStatus = ApprovalStatus.REQUESTED.name();

	@Column(name = "approval_id", length = 64)
	private String approvalId;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "decided_at")
	private LocalDateTime decidedAt;

	protected PaymentRequest() { }

	public PaymentRequest(String paymentRequestId, String projectId, String poId, String claimIds, BigDecimal requestedAmount, String approvalId) {
		this.paymentRequestId = paymentRequestId;
		this.projectId = projectId;
		this.poId = poId;
		this.claimIds = claimIds;
		this.requestedAmount = requestedAmount;
		this.approvalId = approvalId;
	}

	@PrePersist
	void onCreate() { this.requestedAt = LocalDateTime.now(); }

	public void approve() { this.approvalStatus = ApprovalStatus.APPROVED.name(); this.decidedAt = LocalDateTime.now(); }
	public void reject() { this.approvalStatus = ApprovalStatus.REJECTED.name(); this.decidedAt = LocalDateTime.now(); }
	public String getPaymentRequestId() { return paymentRequestId; }
	public String getProjectId() { return projectId; }
	public String getPoId() { return poId; }
	public String getClaimIds() { return claimIds; }
	public BigDecimal getRequestedAmount() { return requestedAmount; }
	public String getApprovalStatus() { return approvalStatus; }
	public String getApprovalId() { return approvalId; }
}
