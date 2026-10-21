package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

	/** The approved execution-budget revision this order belongs to. Null is the original budget. */
	@Column(name = "revision_id", length = 64)
	private String revisionId;

	@Column(name = "vendor_name", nullable = false)
	private String vendorName;

	@Column(name = "initial_po_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal initialPoAmount = BigDecimal.ZERO;

	@Column(name = "requisition_id", length = 64)
	private String requisitionId;

	@Column(name = "approval_id", length = 64)
	private String approvalId;

	@Column(name = "selected_comparison_id", length = 64)
	private String selectedComparisonId;

	@Column(name = "approved_budget_amount", precision = 18, scale = 2)
	private BigDecimal approvedBudgetAmount = BigDecimal.ZERO;

	@Column(name = "budget_variance", precision = 18, scale = 2)
	private BigDecimal budgetVariance = BigDecimal.ZERO;

	@Column(name = "selection_reason", length = 1000)
	private String selectionReason;

	@Column(name = "construction_period")
	private String constructionPeriod;

	@Column(name = "scope_and_notes", length = 1000)
	private String scopeAndNotes;

	@Column(name = "payment_terms")
	private String paymentTerms;

	@Column(name = "estimate_file_url", length = 1000)
	private String estimateFileUrl;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@Column(name = "approver")
	private String approver;

	@Column(name = "approval_comment", length = 1000)
	private String approvalComment;

	protected PurchaseOrder() {
	}

	public PurchaseOrder(String poId, String projectId, String budgetId, String vendorName, BigDecimal initialPoAmount) {
		this.poId = poId;
		this.projectId = projectId;
		this.budgetId = budgetId;
		this.vendorName = vendorName;
		this.initialPoAmount = initialPoAmount == null ? BigDecimal.ZERO : initialPoAmount;
	}

	public PurchaseOrder(String poId, String projectId, String budgetId, PoRequisition requisition, VendorComparison selectedComparison, String approver, String approvalComment) {
		this(poId, projectId, budgetId, selectedComparison.getVendorName(), requisition.getPoAmount());
		this.revisionId = requisition.getRevisionId();
		this.requisitionId = requisition.getRequisitionId();
		this.approvalId = requisition.getApprovalId();
		this.selectedComparisonId = selectedComparison.getComparisonId();
		this.approvedBudgetAmount = defaultZero(requisition.getApprovedBudgetAmount());
		this.budgetVariance = defaultZero(requisition.getBudgetVariance());
		this.selectionReason = requisition.getSelectionReason();
		this.constructionPeriod = selectedComparison.getConstructionPeriod();
		this.scopeAndNotes = selectedComparison.getScopeAndNotes();
		this.paymentTerms = selectedComparison.getPaymentTerms();
		this.estimateFileUrl = selectedComparison.getEstimateFileUrl();
		this.approvedAt = LocalDateTime.now();
		this.approver = approver;
		this.approvalComment = approvalComment;
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

	public String getRevisionId() { return revisionId; }

	public String getRequisitionId() { return requisitionId; }
	public String getApprovalId() { return approvalId; }
	public String getSelectedComparisonId() { return selectedComparisonId; }
	public BigDecimal getApprovedBudgetAmount() { return approvedBudgetAmount; }
	public BigDecimal getBudgetVariance() { return budgetVariance; }
	public String getSelectionReason() { return selectionReason; }
	public String getConstructionPeriod() { return constructionPeriod; }
	public String getScopeAndNotes() { return scopeAndNotes; }
	public String getPaymentTerms() { return paymentTerms; }
	public String getEstimateFileUrl() { return estimateFileUrl; }
	public LocalDateTime getApprovedAt() { return approvedAt; }
	public String getApprover() { return approver; }
	public String getApprovalComment() { return approvalComment; }

	private static BigDecimal defaultZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
