package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_revisions")
public class ProjectRevision extends com.construction.management.auth.AuthoredEntity {

	@Id
	@Column(name = "revision_id", nullable = false, length = 64)
	private String revisionId;

	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "revision_number", nullable = false)
	private int revisionNumber;

	@Column(name = "title", nullable = false, length = 255)
	private String title;

	@Column(name = "reason", nullable = false, length = 1000)
	private String reason;

	@Column(name = "status", nullable = false, length = 32)
	private String status = "DRAFT";

	@Column(name = "execution_budget_rewrite_available", nullable = false)
	private boolean executionBudgetRewriteAvailable;

	@Lob
	@Column(name = "base_contract_snapshot", nullable = false)
	private String baseContractSnapshot;

	@Lob
	@Column(name = "contract_snapshot", nullable = false)
	private String contractSnapshot;

	@Lob
	@Column(name = "base_budget_snapshot", nullable = false)
	private String baseBudgetSnapshot;

	@Lob
	@Column(name = "budget_snapshot", nullable = false)
	private String budgetSnapshot;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected ProjectRevision() { }

	public ProjectRevision(String revisionId, String projectId, int revisionNumber, String title, String reason, String contractSnapshot, String budgetSnapshot) {
		this(revisionId, projectId, revisionNumber, title, reason, contractSnapshot, contractSnapshot, budgetSnapshot, budgetSnapshot);
	}

	public ProjectRevision(
			String revisionId,
			String projectId,
			int revisionNumber,
			String title,
			String reason,
			String baseContractSnapshot,
			String contractSnapshot,
			String baseBudgetSnapshot,
			String budgetSnapshot
	) {
		this.revisionId = revisionId;
		this.projectId = projectId;
		this.revisionNumber = revisionNumber;
		this.title = title;
		this.reason = reason;
		this.baseContractSnapshot = baseContractSnapshot;
		this.contractSnapshot = contractSnapshot;
		this.baseBudgetSnapshot = baseBudgetSnapshot;
		this.budgetSnapshot = budgetSnapshot;
	}

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@jakarta.persistence.PreUpdate
	void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public String getRevisionId() { return revisionId; }
	public String getProjectId() { return projectId; }
	public int getRevisionNumber() { return revisionNumber; }
	public String getTitle() { return title; }
	public String getReason() { return reason; }
	public String getStatus() { return status; }
	public boolean isExecutionBudgetRewriteAvailable() { return executionBudgetRewriteAvailable; }
	public String getBaseContractSnapshot() { return baseContractSnapshot; }
	public String getContractSnapshot() { return contractSnapshot; }
	public String getBaseBudgetSnapshot() { return baseBudgetSnapshot; }
	public String getBudgetSnapshot() { return budgetSnapshot; }
	public LocalDateTime getCreatedAt() { return createdAt; }

	public void updateContractSnapshot(String contractSnapshot) {
		this.contractSnapshot = contractSnapshot;
		this.status = "CONTRACT_WRITTEN";
	}

	public void updateBudgetSnapshot(String budgetSnapshot) {
		this.budgetSnapshot = budgetSnapshot;
		this.status = "BUDGET_WRITTEN";
	}

	public void markApprovalRequested() {
		this.status = "REQUESTED";
	}

	public void approve() {
		this.status = "APPROVED";
	}

	public void enableExecutionBudgetRewrite() {
		this.executionBudgetRewriteAvailable = true;
	}

	public void consumeExecutionBudgetRewrite() {
		this.executionBudgetRewriteAvailable = false;
	}

	public void replaceBudgetSnapshot(String budgetSnapshot) {
		this.budgetSnapshot = budgetSnapshot;
	}

	public void reject() {
		this.status = "REJECTED";
	}
}
