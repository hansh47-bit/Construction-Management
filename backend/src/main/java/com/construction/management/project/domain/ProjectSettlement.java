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

	@Column(name = "final_cost", nullable = false, precision = 18, scale = 2)
	private BigDecimal finalCost;

	@Column(name = "final_profit", nullable = false, precision = 18, scale = 2)
	private BigDecimal finalProfit;

	@Column(name = "profit_margin_rate", nullable = false, precision = 7, scale = 2)
	private BigDecimal profitMarginRate;

	@Column(name = "settlement_status", nullable = false, length = 32)
	private String settlementStatus = "SETTLED";

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
		this.settlementId = settlementId;
		this.projectId = projectId;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	public ProjectSettlement settle(
			ProjectSettlementSnapshot snapshot,
			BigDecimal uncollectedReceivable,
			String settlementNotes,
			String unpaidHandlingReason,
			String settledBy
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
		this.finalCost = snapshot.finalCost();
		this.finalProfit = snapshot.finalProfit();
		this.profitMarginRate = snapshot.profitMarginRate();
		this.settlementStatus = "SETTLED";
		this.settlementNotes = settlementNotes;
		this.unpaidHandlingReason = unpaidHandlingReason;
		this.settledBy = settledBy;
		this.settledAt = LocalDateTime.now();
		return this;
	}

	public String getSettlementId() {
		return settlementId;
	}

	public BigDecimal getUncollectedReceivable() {
		return uncollectedReceivable;
	}

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
			BigDecimal finalCost,
			BigDecimal finalProfit,
			BigDecimal profitMarginRate
	) {
	}
}
