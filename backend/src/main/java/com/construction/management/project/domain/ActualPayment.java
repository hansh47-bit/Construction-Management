package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "actual_payments")
public class ActualPayment {

	@Id
	@Column(name = "payment_id", nullable = false, length = 64)
	private String paymentId;

	@Column(name = "claim_id", nullable = false, length = 64)
	private String claimId;

	@Column(name = "paid_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal paidAmount;

	@Column(name = "paid_date", nullable = false)
	private LocalDate paidDate;

	@Column(name = "payment_status")
	private String paymentStatus = "PAID";

	@Column(name = "account_info")
	private String accountInfo;

	@Column(name = "receipt_link", length = 1000)
	private String receiptLink;

	@Column(name = "recorded_at", updatable = false)
	private LocalDateTime recordedAt;

	protected ActualPayment() {
	}

	public ActualPayment(String paymentId, String claimId, BigDecimal paidAmount, LocalDate paidDate) {
		this.paymentId = paymentId;
		this.claimId = claimId;
		this.paidAmount = paidAmount;
		this.paidDate = paidDate;
	}

	public ActualPayment(String paymentId, String claimId, BigDecimal paidAmount, LocalDate paidDate, String accountInfo, String receiptLink) {
		this(paymentId, claimId, paidAmount, paidDate);
		this.accountInfo = accountInfo;
		this.receiptLink = receiptLink;
	}

	@PrePersist
	void onCreate() {
		this.recordedAt = LocalDateTime.now();
	}

	public BigDecimal getPaidAmount() {
		return paidAmount;
	}

	public String getClaimId() {
		return claimId;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public LocalDate getPaidDate() {
		return paidDate;
	}

	public String getPaymentStatus() {
		return paymentStatus;
	}

	public String getAccountInfo() {
		return accountInfo;
	}

	public String getReceiptLink() {
		return receiptLink;
	}
}
