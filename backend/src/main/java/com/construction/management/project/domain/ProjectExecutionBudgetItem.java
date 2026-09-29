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
@Table(name = "project_execution_budget_items")
public class ProjectExecutionBudgetItem {

	@Id
	@Column(name = "item_id", nullable = false, length = 64)
	private String itemId;

	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "category_name", nullable = false)
	private String categoryName;

	@Column(name = "item_name", nullable = false)
	private String itemName;

	@Column(name = "budget_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal budgetAmount = BigDecimal.ZERO;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected ProjectExecutionBudgetItem() {
	}

	public ProjectExecutionBudgetItem(String itemId, String projectId, String categoryName, String itemName) {
		this.itemId = itemId;
		this.projectId = projectId;
		this.categoryName = categoryName;
		this.itemName = itemName;
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

	public String getCategoryName() {
		return categoryName;
	}

	public String getItemName() {
		return itemName;
	}

	public BigDecimal getBudgetAmount() {
		return budgetAmount;
	}

	public void updateInfo(String categoryName, String itemName) {
		this.categoryName = categoryName;
		this.itemName = itemName;
	}

	public void updateBudgetAmount(BigDecimal budgetAmount) {
		this.budgetAmount = budgetAmount == null ? BigDecimal.ZERO : budgetAmount;
	}
}
