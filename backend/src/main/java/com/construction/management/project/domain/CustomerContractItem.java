package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_contract_items")
public class CustomerContractItem {

	@Id
	@Column(name = "item_id", nullable = false, length = 64)
	private String itemId;

	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "work_category", nullable = false)
	private String workCategory;

	@Column(name = "quoted_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal quotedAmount = BigDecimal.ZERO;

	@Column(name = "contract_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal contractAmount = BigDecimal.ZERO;

	@Column(name = "vat_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal vatAmount = BigDecimal.ZERO;

	@Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	@Column(name = "note", length = 1000)
	private String note;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected CustomerContractItem() {
	}

	public CustomerContractItem(
			String itemId,
			String projectId,
			String workCategory,
			BigDecimal quotedAmount,
			BigDecimal contractAmount,
			BigDecimal vatAmount,
			String note
	) {
		this.itemId = itemId;
		this.projectId = projectId;
		this.workCategory = workCategory;
		this.quotedAmount = defaultZero(quotedAmount);
		this.contractAmount = defaultZero(contractAmount);
		this.vatAmount = vatAmount == null ? this.contractAmount.multiply(BigDecimal.valueOf(0.1)) : vatAmount;
		this.totalAmount = this.contractAmount.add(this.vatAmount);
		this.note = note;
	}

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public String getItemId() {
		return itemId;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getWorkCategory() {
		return workCategory;
	}

	public BigDecimal getQuotedAmount() {
		return quotedAmount;
	}

	public BigDecimal getContractAmount() {
		return contractAmount;
	}

	public BigDecimal getVatAmount() {
		return vatAmount;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public String getNote() {
		return note;
	}

	private static BigDecimal defaultZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
