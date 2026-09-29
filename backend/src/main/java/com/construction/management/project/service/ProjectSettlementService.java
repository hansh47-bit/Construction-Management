package com.construction.management.project.service;

import com.construction.management.project.api.ProjectSettlementCompleteRequest;
import com.construction.management.project.api.ProjectSettlementResponse;
import com.construction.management.project.api.ProjectSettlementResponse.AmountSummary;
import com.construction.management.project.api.ProjectSettlementResponse.PaymentSummary;
import com.construction.management.project.api.ProjectSettlementResponse.Profitability;
import com.construction.management.project.domain.ActualPayment;
import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.BudgetChange;
import com.construction.management.project.domain.ContractChange;
import com.construction.management.project.domain.CustomerContractItem;
import com.construction.management.project.domain.ExecutionBudget;
import com.construction.management.project.domain.PoChange;
import com.construction.management.project.domain.ProgressClaim;
import com.construction.management.project.domain.Project;
import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import com.construction.management.project.domain.ProjectSettlement;
import com.construction.management.project.domain.ProjectSettlement.ProjectSettlementSnapshot;
import com.construction.management.project.domain.ProjectStatus;
import com.construction.management.project.domain.PurchaseOrder;
import com.construction.management.project.repository.ActualPaymentRepository;
import com.construction.management.project.repository.BudgetChangeRepository;
import com.construction.management.project.repository.ContractChangeRepository;
import com.construction.management.project.repository.CustomerContractItemRepository;
import com.construction.management.project.repository.ExecutionBudgetRepository;
import com.construction.management.project.repository.PoChangeRepository;
import com.construction.management.project.repository.ProgressClaimRepository;
import com.construction.management.project.repository.ProjectContractRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetItemRepository;
import com.construction.management.project.repository.ProjectRepository;
import com.construction.management.project.repository.ProjectSettlementRepository;
import com.construction.management.project.repository.PurchaseOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectSettlementService {
	private final ProjectRepository projectRepository;
	private final ProjectContractRepository projectContractRepository;
	private final ContractChangeRepository contractChangeRepository;
	private final CustomerContractItemRepository customerContractItemRepository;
	private final ProjectExecutionBudgetItemRepository executionBudgetItemRepository;
	private final ExecutionBudgetRepository executionBudgetRepository;
	private final BudgetChangeRepository budgetChangeRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final PoChangeRepository poChangeRepository;
	private final ProgressClaimRepository progressClaimRepository;
	private final ActualPaymentRepository actualPaymentRepository;
	private final ProjectSettlementRepository settlementRepository;

	public ProjectSettlementService(
			ProjectRepository projectRepository,
			ProjectContractRepository projectContractRepository,
			ContractChangeRepository contractChangeRepository,
			CustomerContractItemRepository customerContractItemRepository,
			ProjectExecutionBudgetItemRepository executionBudgetItemRepository,
			ExecutionBudgetRepository executionBudgetRepository,
			BudgetChangeRepository budgetChangeRepository,
			PurchaseOrderRepository purchaseOrderRepository,
			PoChangeRepository poChangeRepository,
			ProgressClaimRepository progressClaimRepository,
			ActualPaymentRepository actualPaymentRepository,
			ProjectSettlementRepository settlementRepository
	) {
		this.projectRepository = projectRepository;
		this.projectContractRepository = projectContractRepository;
		this.contractChangeRepository = contractChangeRepository;
		this.customerContractItemRepository = customerContractItemRepository;
		this.executionBudgetItemRepository = executionBudgetItemRepository;
		this.executionBudgetRepository = executionBudgetRepository;
		this.budgetChangeRepository = budgetChangeRepository;
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.poChangeRepository = poChangeRepository;
		this.progressClaimRepository = progressClaimRepository;
		this.actualPaymentRepository = actualPaymentRepository;
		this.settlementRepository = settlementRepository;
	}

	@Transactional(readOnly = true)
	public ProjectSettlementResponse getSettlement(String projectId) {
		Project project = getProject(projectId);
		ProjectSettlementSnapshot snapshot = calculate(projectId);
		ProjectSettlement settlement = settlementRepository.findByProjectId(projectId).orElse(null);
		return toResponse(project, snapshot, settlement, settlement == null ? BigDecimal.ZERO : settlement.getUncollectedReceivable());
	}

	@Transactional
	public ProjectSettlementResponse completeSettlement(String projectId, ProjectSettlementCompleteRequest request) {
		Project project = getProject(projectId);
		ProjectSettlementSnapshot snapshot = calculate(projectId);
		BigDecimal uncollectedReceivable = defaultZero(request.uncollectedReceivable());

		if (snapshot.unpaidBalance().signum() > 0) {
			if (!Boolean.TRUE.equals(request.forceCompleteIfUnpaid())) {
				throw new IllegalArgumentException("미지급 잔액이 남아있습니다. 강제 완료 여부와 미지급 처리 사유를 확인해 주세요.");
			}
			if (!StringUtils.hasText(request.unpaidHandlingReason())) {
				throw new IllegalArgumentException("미지급 잔액이 있는 경우 미지급 처리 사유가 필요합니다.");
			}
		}

		ProjectSettlement settlement = settlementRepository.findByProjectId(projectId)
				.orElseGet(() -> new ProjectSettlement(nextSettlementId(), projectId));
		settlement.settle(
				snapshot,
				uncollectedReceivable,
				request.settlementNotes(),
				request.unpaidHandlingReason(),
				StringUtils.hasText(request.settledBy()) ? request.settledBy() : "system"
		);
		settlementRepository.save(settlement);
		project.changeStatus(ProjectStatus.정산완료);
		projectRepository.save(project);
		return toResponse(project, snapshot, settlement, uncollectedReceivable);
	}

	public void assertNotSettled(String projectId) {
		Project project = getProject(projectId);
		if (project.getStatus() == ProjectStatus.정산완료 || settlementRepository.findByProjectId(projectId).isPresent()) {
			throw new IllegalStateException("정산 완료된 프로젝트는 추가 등록 또는 변경할 수 없습니다.");
		}
	}

	private ProjectSettlementSnapshot calculate(String projectId) {
		List<CustomerContractItem> contractItems = customerContractItemRepository.findByProjectIdOrderByWorkCategoryAsc(projectId);
		BigDecimal initialContractAmount = contractItems.isEmpty()
				? projectContractRepository.findByProjectId(projectId).stream()
						.map(contract -> defaultZero(contract.getInitialAmount()))
						.reduce(BigDecimal.ZERO, BigDecimal::add)
				: contractItems.stream()
						.map(CustomerContractItem::getContractAmount)
						.map(ProjectSettlementService::defaultZero)
						.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal changeContractAmount = contractChangeRepository.findByProjectIdAndApprovalStatus(projectId, ApprovalStatus.APPROVED).stream()
				.map(ContractChange::getChangeAmount)
				.map(ProjectSettlementService::defaultZero)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal finalContractAmount = initialContractAmount.add(changeContractAmount);

		List<ProjectExecutionBudgetItem> rolledUpItems = executionBudgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId);
		BigDecimal initialBudgetAmount = rolledUpItems.isEmpty()
				? executionBudgetRepository.findByProjectId(projectId).stream()
						.map(ExecutionBudget::getInitialBudget)
						.map(ProjectSettlementService::defaultZero)
						.reduce(BigDecimal.ZERO, BigDecimal::add)
				: rolledUpItems.stream()
						.map(ProjectExecutionBudgetItem::getBudgetAmount)
						.map(ProjectSettlementService::defaultZero)
						.reduce(BigDecimal.ZERO, BigDecimal::add);
		List<String> budgetIds = executionBudgetRepository.findByProjectId(projectId).stream().map(ExecutionBudget::getBudgetId).toList();
		BigDecimal changeBudgetAmount = budgetIds.isEmpty()
				? BigDecimal.ZERO
				: budgetChangeRepository.findByBudgetIdInAndApprovalStatus(budgetIds, ApprovalStatus.APPROVED).stream()
						.map(BudgetChange::getChangeAmount)
						.map(ProjectSettlementService::defaultZero)
						.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal finalBudgetAmount = initialBudgetAmount.add(changeBudgetAmount);

		List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findByProjectId(projectId);
		BigDecimal initialPoAmount = purchaseOrders.stream()
				.map(PurchaseOrder::getInitialPoAmount)
				.map(ProjectSettlementService::defaultZero)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		List<String> poIds = purchaseOrders.stream().map(PurchaseOrder::getPoId).toList();
		BigDecimal changePoAmount = poIds.isEmpty()
				? BigDecimal.ZERO
				: poChangeRepository.findByPoIdInAndApprovalStatus(poIds, ApprovalStatus.APPROVED).stream()
						.map(PoChange::getChangeAmount)
						.map(ProjectSettlementService::defaultZero)
						.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal finalPoAmount = initialPoAmount.add(changePoAmount);

		List<ProgressClaim> approvedClaims = poIds.isEmpty()
				? List.of()
				: progressClaimRepository.findByPoIdInAndApprovalStatus(poIds, ApprovalStatus.APPROVED);
		BigDecimal approvedClaimsTotal = approvedClaims.stream()
				.map(ProgressClaim::getClaimAmount)
				.map(ProjectSettlementService::defaultZero)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		List<String> claimIds = approvedClaims.stream().map(ProgressClaim::getClaimId).toList();
		BigDecimal actualPaidTotal = claimIds.isEmpty()
				? BigDecimal.ZERO
				: actualPaymentRepository.findByClaimIdIn(claimIds).stream()
						.map(ActualPayment::getPaidAmount)
						.map(ProjectSettlementService::defaultZero)
						.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal unpaidBalance = finalPoAmount.subtract(actualPaidTotal).max(BigDecimal.ZERO);
		BigDecimal finalCost = finalPoAmount.signum() > 0 ? finalPoAmount : actualPaidTotal;
		BigDecimal finalProfit = finalContractAmount.subtract(finalCost);
		BigDecimal profitMarginRate = finalContractAmount.signum() == 0
				? BigDecimal.ZERO
				: finalProfit.multiply(BigDecimal.valueOf(100)).divide(finalContractAmount, 2, RoundingMode.HALF_UP);

		return new ProjectSettlementSnapshot(
				initialContractAmount,
				changeContractAmount,
				finalContractAmount,
				initialBudgetAmount,
				changeBudgetAmount,
				finalBudgetAmount,
				initialPoAmount,
				changePoAmount,
				finalPoAmount,
				approvedClaimsTotal,
				actualPaidTotal,
				unpaidBalance,
				finalCost,
				finalProfit,
				profitMarginRate
		);
	}

	private ProjectSettlementResponse toResponse(Project project, ProjectSettlementSnapshot snapshot, ProjectSettlement settlement, BigDecimal uncollectedReceivable) {
		return new ProjectSettlementResponse(
				project.getProjectId(),
				project.getProjectCode(),
				project.getProjectName(),
				new AmountSummary(snapshot.initialContractAmount(), snapshot.changeContractAmount(), snapshot.finalContractAmount()),
				new AmountSummary(snapshot.initialBudgetAmount(), snapshot.changeBudgetAmount(), snapshot.finalBudgetAmount()),
				new AmountSummary(snapshot.initialPoAmount(), snapshot.changePoAmount(), snapshot.finalPoAmount()),
				new PaymentSummary(snapshot.approvedClaimsTotal(), snapshot.actualPaidTotal(), snapshot.unpaidBalance(), defaultZero(uncollectedReceivable)),
				new Profitability(snapshot.finalCost(), snapshot.finalProfit(), snapshot.profitMarginRate()),
				settlement != null,
				settlement == null ? null : settlement.getSettlementId(),
				settlement == null ? "DRAFT" : settlement.getSettlementStatus(),
				settlement == null ? null : settlement.getSettlementNotes(),
				settlement == null ? null : settlement.getUnpaidHandlingReason(),
				settlement == null ? null : settlement.getSettledBy(),
				settlement == null ? null : settlement.getSettledAt()
		);
	}

	private Project getProject(String projectId) {
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
	}

	private String nextSettlementId() {
		return "STL-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}

	private static BigDecimal defaultZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
