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
@Table(name = "vendor_comparisons")
public class VendorComparison {

	@Id
	@Column(name = "comparison_id", nullable = false, length = 64)
	private String comparisonId;

	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "execution_item_id", nullable = false, length = 64)
	private String executionItemId;

	@Column(name = "vendor_name", nullable = false)
	private String vendorName;

	@Column(name = "quoted_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal quotedAmount = BigDecimal.ZERO;

	@Column(name = "vat_type", nullable = false)
	private String vatType = "별도";

	@Column(name = "construction_period")
	private String constructionPeriod;

	@Column(name = "scope_and_notes", length = 1000)
	private String scopeAndNotes;

	@Column(name = "payment_terms")
	private String paymentTerms;

	@Column(name = "estimate_file_url", length = 1000)
	private String estimateFileUrl;

	@Column(name = "is_selected", nullable = false)
	private boolean selected;

	@Column(name = "selection_reason", length = 1000)
	private String selectionReason;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected VendorComparison() {
	}

	public VendorComparison(String comparisonId, String projectId, String executionItemId, String vendorName, BigDecimal quotedAmount, String vatType, String constructionPeriod, String scopeAndNotes, String paymentTerms, String estimateFileUrl, boolean selected, String selectionReason) {
		this.comparisonId = comparisonId;
		this.projectId = projectId;
		this.executionItemId = executionItemId;
		this.vendorName = vendorName;
		this.quotedAmount = quotedAmount == null ? BigDecimal.ZERO : quotedAmount;
		this.vatType = vatType == null ? "별도" : vatType;
		this.constructionPeriod = constructionPeriod;
		this.scopeAndNotes = scopeAndNotes;
		this.paymentTerms = paymentTerms;
		this.estimateFileUrl = estimateFileUrl;
		this.selected = selected;
		this.selectionReason = selectionReason;
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

	public String getComparisonId() { return comparisonId; }
	public String getProjectId() { return projectId; }
	public String getExecutionItemId() { return executionItemId; }
	public String getVendorName() { return vendorName; }
	public BigDecimal getQuotedAmount() { return quotedAmount; }
	public String getVatType() { return vatType; }
	public String getConstructionPeriod() { return constructionPeriod; }
	public String getScopeAndNotes() { return scopeAndNotes; }
	public String getPaymentTerms() { return paymentTerms; }
	public String getEstimateFileUrl() { return estimateFileUrl; }
	public boolean isSelected() { return selected; }
	public String getSelectionReason() { return selectionReason; }
}
