package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_settlements")
public class ProjectSettlement {

	@Id
	@Column(name = "settlement_id", nullable = false, length = 64)
	private String settlementId;

	@Column(name = "project_id", nullable = false, unique = true, length = 64)
	private String projectId;

	/** The approved execution-budget revision being settled. Null is the original budget. */
	@Column(name = "revision_id", length = 64)
	private String revisionId;

	@Column(name = "initial_contract_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal initialContractAmount;

	@Column(name = "change_contract_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal changeContractAmount;

	@Column(name = "final_contract_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal finalContractAmount;

	@Column(name = "initial_budget_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal initialBudgetAmount;

	@Column(name = "change_budget_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal changeBudgetAmount;

	@Column(name = "final_budget_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal finalBudgetAmount;

	@Column(name = "initial_po_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal initialPoAmount;

	@Column(name = "change_po_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal changePoAmount;

	@Column(name = "final_po_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal finalPoAmount;

	@Column(name = "approved_claims_total", nullable = false, precision = 18, scale = 2)
	private BigDecimal approvedClaimsTotal;

	@Column(name = "actual_paid_total", nullable = false, precision = 18, scale = 2)
	private BigDecimal actualPaidTotal;

	@Column(name = "unpaid_balance", nullable = false, precision = 18, scale = 2)
	private BigDecimal unpaidBalance;

	@Column(name = "uncollected_receivable", nullable = false, precision = 18, scale = 2)
	private BigDecimal uncollectedReceivable;

	@Column(name = "settlement_expense", nullable = false, precision = 18, scale = 2)
	private BigDecimal settlementExpense = BigDecimal.ZERO;

	@Column(name = "settlement_site_expense", nullable = false, precision = 18, scale = 2, columnDefinition = "decimal(18,2) default 0")
	private BigDecimal settlementSiteExpense = BigDecimal.ZERO;

	@Column(name = "settlement_labor_expense", nullable = false, precision = 18, scale = 2, columnDefinition = "decimal(18,2) default 0")
	private BigDecimal settlementLaborExpense = BigDecimal.ZERO;

	@Column(name = "settlement_tax", nullable = false, precision = 18, scale = 2)
	private BigDecimal settlementTax = BigDecimal.ZERO;

	@Column(name = "final_cost", nullable = false, precision = 18, scale = 2)
	private BigDecimal finalCost;

	@Column(name = "final_profit", nullable = false, precision = 18, scale = 2)
	private BigDecimal finalProfit;

	@Column(name = "profit_margin_rate", nullable = false, precision = 7, scale = 2)
	private BigDecimal profitMarginRate;

	@Column(name = "settlement_status", nullable = false, length = 32)
	private String settlementStatus = "DRAFT";

	@Column(name = "settlement_notes", length = 1000)
	private String settlementNotes;

	@Column(name = "unpaid_handling_reason", length = 1000)
	private String unpaidHandlingReason;

	@Column(name = "settled_by", length = 64)
	private String settledBy;

	@Column(name = "settled_at")
	private LocalDateTime settledAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected ProjectSettlement() {
	}

	public ProjectSettlement(String settlementId, String projectId) {
		this(settlementId, projectId, null);
	}

	public ProjectSettlement(String settlementId, String projectId, String revisionId) {
		this.settlementId = settlementId;
		this.projectId = projectId;
		this.revisionId = revisionId;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	public ProjectSettlement requestApproval(
			ProjectSettlementSnapshot snapshot,
			BigDecimal uncollectedReceivable,
			BigDecimal settlementSiteExpense,
			BigDecimal settlementLaborExpense,
			BigDecimal settlementTax,
			String settlementNotes,
			String unpaidHandlingReason
	) {
		this.initialContractAmount = snapshot.initialContractAmount();
		this.changeContractAmount = snapshot.changeContractAmount();
		this.finalContractAmount = snapshot.finalContractAmount();
		this.initialBudgetAmount = snapshot.initialBudgetAmount();
		this.changeBudgetAmount = snapshot.changeBudgetAmount();
		this.finalBudgetAmount = snapshot.finalBudgetAmount();
		this.initialPoAmount = snapshot.initialPoAmount();
		this.changePoAmount = snapshot.changePoAmount();
		this.finalPoAmount = snapshot.finalPoAmount();
		this.approvedClaimsTotal = snapshot.approvedClaimsTotal();
		this.actualPaidTotal = snapshot.actualPaidTotal();
		this.unpaidBalance = snapshot.unpaidBalance();
		this.uncollectedReceivable = uncollectedReceivable == null ? BigDecimal.ZERO : uncollectedReceivable;
		this.settlementSiteExpense = settlementSiteExpense == null ? BigDecimal.ZERO : settlementSiteExpense;
		this.settlementLaborExpense = settlementLaborExpense == null ? BigDecimal.ZERO : settlementLaborExpense;
		this.settlementExpense = this.settlementSiteExpense.add(this.settlementLaborExpense);
		this.settlementTax = settlementTax == null ? BigDecimal.ZERO : settlementTax;
		this.finalCost = snapshot.finalCost();
		this.finalProfit = snapshot.finalProfit();
		this.profitMarginRate = snapshot.profitMarginRate();
		this.settlementStatus = "REQUESTED";
		this.settlementNotes = settlementNotes;
		this.unpaidHandlingReason = unpaidHandlingReason;
		this.settledBy = null;
		this.settledAt = null;
		return this;
	}

	public void approve(String approver) {
		this.settlementStatus = "SETTLED";
		this.settledBy = approver;
		this.settledAt = LocalDateTime.now();
	}

	public void reject() {
		this.settlementStatus = "REJECTED";
		this.settledBy = null;
		this.settledAt = null;
	}

	public boolean isSettled() {
		return "SETTLED".equals(settlementStatus);
	}

	public ProjectSettlementSnapshot snapshot() {
		return new ProjectSettlementSnapshot(
				initialContractAmount, changeContractAmount, finalContractAmount,
				initialBudgetAmount, changeBudgetAmount, finalBudgetAmount,
				initialPoAmount, changePoAmount, finalPoAmount,
				approvedClaimsTotal, actualPaidTotal, unpaidBalance,
				settlementExpense, settlementTax, finalCost, finalProfit, profitMarginRate
		);
	}

	public String getSettlementId() {
		return settlementId;
	}

	public String getProjectId() { return projectId; }
	public String getRevisionId() { return revisionId; }

	public BigDecimal getUncollectedReceivable() {
		return uncollectedReceivable;
	}

	public BigDecimal getSettlementExpense() { return settlementExpense; }
	public BigDecimal getSettlementSiteExpense() { return settlementSiteExpense == null ? BigDecimal.ZERO : settlementSiteExpense; }
	public BigDecimal getSettlementLaborExpense() { return settlementLaborExpense == null ? BigDecimal.ZERO : settlementLaborExpense; }
	public BigDecimal getSettlementTax() { return settlementTax; }

	public String getSettlementStatus() {
		return settlementStatus;
	}

	public String getSettlementNotes() {
		return settlementNotes;
	}

	public String getUnpaidHandlingReason() {
		return unpaidHandlingReason;
	}

	public String getSettledBy() {
		return settledBy;
	}

	public LocalDateTime getSettledAt() {
		return settledAt;
	}

	public record ProjectSettlementSnapshot(
			BigDecimal initialContractAmount,
			BigDecimal changeContractAmount,
			BigDecimal finalContractAmount,
			BigDecimal initialBudgetAmount,
			BigDecimal changeBudgetAmount,
			BigDecimal finalBudgetAmount,
			BigDecimal initialPoAmount,
			BigDecimal changePoAmount,
			BigDecimal finalPoAmount,
			BigDecimal approvedClaimsTotal,
			BigDecimal actualPaidTotal,
			BigDecimal unpaidBalance,
			BigDecimal settlementExpense,
			BigDecimal settlementTax,
			BigDecimal finalCost,
			BigDecimal finalProfit,
			BigDecimal profitMarginRate
	) {
	}
}
