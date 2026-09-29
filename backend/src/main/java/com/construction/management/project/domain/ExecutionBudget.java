package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "execution_budgets")
public class ExecutionBudget {

	@Id
	@Column(name = "budget_id", nullable = false, length = 64)
	private String budgetId;

	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "work_category", nullable = false)
	private String workCategory;

	@Column(name = "initial_budget", nullable = false, precision = 18, scale = 2)
	private BigDecimal initialBudget = BigDecimal.ZERO;

	protected ExecutionBudget() {
	}

	public ExecutionBudget(String budgetId, String projectId, String workCategory, BigDecimal initialBudget) {
		this.budgetId = budgetId;
		this.projectId = projectId;
		this.workCategory = workCategory;
		this.initialBudget = initialBudget == null ? BigDecimal.ZERO : initialBudget;
	}

	public String getBudgetId() {
		return budgetId;
	}

	public String getWorkCategory() {
		return workCategory;
	}

	public BigDecimal getInitialBudget() {
		return initialBudget;
	}
}
