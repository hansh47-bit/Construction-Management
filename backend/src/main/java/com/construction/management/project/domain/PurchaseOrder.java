package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

	@Id
	@Column(name = "po_id", nullable = false, length = 64)
	private String poId;

	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "budget_id", length = 64)
	private String budgetId;

	@Column(name = "vendor_name", nullable = false)
	private String vendorName;

	@Column(name = "initial_po_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal initialPoAmount = BigDecimal.ZERO;

	protected PurchaseOrder() {
	}

	public PurchaseOrder(String poId, String projectId, String budgetId, String vendorName, BigDecimal initialPoAmount) {
		this.poId = poId;
		this.projectId = projectId;
		this.budgetId = budgetId;
		this.vendorName = vendorName;
		this.initialPoAmount = initialPoAmount == null ? BigDecimal.ZERO : initialPoAmount;
	}

	public String getPoId() {
		return poId;
	}

	public String getBudgetId() {
		return budgetId;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getVendorName() {
		return vendorName;
	}

	public BigDecimal getInitialPoAmount() {
		return initialPoAmount;
	}
}
