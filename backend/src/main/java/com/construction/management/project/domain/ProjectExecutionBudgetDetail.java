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
@Table(name = "project_execution_budget_details")
public class ProjectExecutionBudgetDetail {

	@Id
	@Column(name = "detail_id", nullable = false, length = 64)
	private String detailId;

	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "budget_item_id", nullable = false, length = 64)
	private String budgetItemId;

	@Column(name = "category_name", nullable = false)
	private String categoryName;

	@Column(name = "item_name", nullable = false)
	private String itemName;

	@Column(name = "cost_type", nullable = false)
	private String costType;

	@Column(name = "vendor_description")
	private String vendorDescription;

	@Column(name = "unit")
	private String unit;

	@Column(name = "quantity", nullable = false, precision = 18, scale = 4)
	private BigDecimal quantity = BigDecimal.ZERO;

	@Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
	private BigDecimal unitPrice = BigDecimal.ZERO;

	@Column(name = "amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal amount = BigDecimal.ZERO;

	@Column(name = "evidence_link", length = 1000)
	private String evidenceLink;

	@Column(name = "note", length = 1000)
	private String note;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected ProjectExecutionBudgetDetail() {
	}

	public ProjectExecutionBudgetDetail(
			String detailId,
			String projectId,
			String budgetItemId,
			String categoryName,
			String itemName,
			String costType,
			String vendorDescription,
			String unit,
			BigDecimal quantity,
			BigDecimal unitPrice,
			String evidenceLink,
			String note
	) {
		this.detailId = detailId;
		this.projectId = projectId;
		this.budgetItemId = budgetItemId;
		this.categoryName = categoryName;
		this.itemName = itemName;
		this.costType = costType;
		this.vendorDescription = vendorDescription;
		this.unit = unit;
		this.quantity = defaultZero(quantity);
		this.unitPrice = defaultZero(unitPrice);
		this.amount = this.quantity.multiply(this.unitPrice);
		this.evidenceLink = evidenceLink;
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

	public String getDetailId() {
		return detailId;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getBudgetItemId() {
		return budgetItemId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public String getItemName() {
		return itemName;
	}

	public String getCostType() {
		return costType;
	}

	public String getVendorDescription() {
		return vendorDescription;
	}

	public String getUnit() {
		return unit;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getEvidenceLink() {
		return evidenceLink;
	}

	public String getNote() {
		return note;
	}

	private static BigDecimal defaultZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
