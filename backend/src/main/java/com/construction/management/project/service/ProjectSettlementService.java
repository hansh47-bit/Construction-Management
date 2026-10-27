package com.construction.management.project.service;

import com.construction.management.project.api.ProjectSettlementCompleteRequest;
import com.construction.management.project.api.ProjectSettlementResponse;
import com.construction.management.project.api.ProjectSettlementResponse.AmountSummary;
import com.construction.management.project.api.ProjectSettlementResponse.PaymentSummary;
import com.construction.management.project.api.ProjectSettlementResponse.Profitability;
import com.construction.management.project.api.ProjectRevisionResponse.BudgetDetail;
import com.construction.management.project.api.ProjectRevisionResponse.ContractItem;
import com.construction.management.project.domain.ActualPayment;
import com.construction.management.project.domain.Approval;
import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.BudgetChange;
import com.construction.management.project.domain.ContractChange;
import com.construction.management.project.domain.CustomerContractItem;
import com.construction.management.project.domain.ExecutionBudget;
import com.construction.management.project.domain.PoChange;
import com.construction.management.project.domain.ProgressClaim;
import com.construction.management.project.domain.Project;
import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import com.construction.management.project.domain.ProjectExecutionBudgetDetail;
import com.construction.management.project.domain.ProjectRevision;
import com.construction.management.project.domain.ProjectSettlement;
import com.construction.management.project.domain.ProjectSettlement.ProjectSettlementSnapshot;
import com.construction.management.project.domain.ProjectStatus;
import com.construction.management.project.domain.PurchaseOrder;
import com.construction.management.project.repository.ActualPaymentRepository;
import com.construction.management.project.repository.ApprovalRepository;
import com.construction.management.project.repository.BudgetChangeRepository;
import com.construction.management.project.repository.ContractChangeRepository;
import com.construction.management.project.repository.CustomerContractItemRepository;
import com.construction.management.project.repository.ExecutionBudgetRepository;
import com.construction.management.project.repository.PoChangeRepository;
import com.construction.management.project.repository.ProgressClaimRepository;
import com.construction.management.project.repository.ProjectContractRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetItemRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetDetailRepository;
import com.construction.management.project.repository.ProjectRepository;
import com.construction.management.project.repository.ProjectRevisionRepository;
import com.construction.management.project.repository.ProjectSettlementRepository;
import com.construction.management.project.repository.PurchaseOrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectSettlementService {
	private final ProjectRepository projectRepository;
	private final ProjectRevisionRepository revisionRepository;
	private final ProjectContractRepository projectContractRepository;
	private final ContractChangeRepository contractChangeRepository;
	private final CustomerContractItemRepository customerContractItemRepository;
	private final ProjectExecutionBudgetItemRepository executionBudgetItemRepository;
	private final ProjectExecutionBudgetDetailRepository executionBudgetDetailRepository;
	private final ExecutionBudgetRepository executionBudgetRepository;
	private final BudgetChangeRepository budgetChangeRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final PoChangeRepository poChangeRepository;
	private final ProgressClaimRepository progressClaimRepository;
	private final ActualPaymentRepository actualPaymentRepository;
	private final ApprovalRepository approvalRepository;
	private final ProjectSettlementRepository settlementRepository;
	private final ObjectMapper objectMapper;

	public ProjectSettlementService(
			ProjectRepository projectRepository,
			ProjectRevisionRepository revisionRepository,
			ProjectContractRepository projectContractRepository,
			ContractChangeRepository contractChangeRepository,
			CustomerContractItemRepository customerContractItemRepository,
			ProjectExecutionBudgetItemRepository executionBudgetItemRepository,
			ProjectExecutionBudgetDetailRepository executionBudgetDetailRepository,
			ExecutionBudgetRepository executionBudgetRepository,
			BudgetChangeRepository budgetChangeRepository,
			PurchaseOrderRepository purchaseOrderRepository,
			PoChangeRepository poChangeRepository,
			ProgressClaimRepository progressClaimRepository,
			ActualPaymentRepository actualPaymentRepository,
			ApprovalRepository approvalRepository,
			ProjectSettlementRepository settlementRepository,
			ObjectMapper objectMapper
	) {
		this.projectRepository = projectRepository;
		this.revisionRepository = revisionRepository;
		this.projectContractRepository = projectContractRepository;
		this.contractChangeRepository = contractChangeRepository;
		this.customerContractItemRepository = customerContractItemRepository;
		this.executionBudgetItemRepository = executionBudgetItemRepository;
		this.executionBudgetDetailRepository = executionBudgetDetailRepository;
		this.executionBudgetRepository = executionBudgetRepository;
		this.budgetChangeRepository = budgetChangeRepository;
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.poChangeRepository = poChangeRepository;
		this.progressClaimRepository = progressClaimRepository;
		this.actualPaymentRepository = actualPaymentRepository;
		this.approvalRepository = approvalRepository;
		this.settlementRepository = settlementRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public ProjectSettlementResponse getSettlement(String projectId) {
		Project project = getProject(projectId);
		ProjectRevision revision = currentRevision(projectId);
		ProjectSettlement settlement = settlementRepository.findByProjectIdAndRevisionId(projectId, revision == null ? null : revision.getRevisionId()).orElse(null);
		BigDecimal approvedSiteExpense = approvedSiteExpense(projectId, revision);
		BigDecimal settlementExpense = settlement == null ? approvedSiteExpense : settlement.getSettlementExpense();
		BigDecimal settlementTax = settlement == null ? BigDecimal.ZERO : settlement.getSettlementTax();
		ProjectSettlementSnapshot snapshot = settlement == null
				? calculate(projectId, revision, BigDecimal.ZERO, BigDecimal.ZERO)
				: settlement.snapshot();
		return toResponse(project, revision, snapshot, settlement, settlement == null ? BigDecimal.ZERO : settlement.getUncollectedReceivable(), settlementExpense, settlementTax, approvedSiteExpense(projectId, revision));
	}

	@Transactional
	public ProjectSettlementResponse requestSettlementApproval(String projectId, ProjectSettlementCompleteRequest request) {
		Project project = getProject(projectId);
		ProjectRevision revision = currentRevision(projectId);
		if (project.getStatus() == ProjectStatus.정산완료) {
			throw new IllegalStateException("이미 최종 승인되어 정산완료된 프로젝트입니다.");
		}
		if (approvalRepository.existsByProjectIdAndApprovalTypeAndStatus(projectId, "FINAL_SETTLEMENT", ApprovalStatus.REQUESTED)) {
			throw new IllegalStateException("이미 결재 대기 중인 준공정산 요청이 있습니다.");
		}
		BigDecimal uncollectedReceivable = defaultZero(request.uncollectedReceivable());
		BigDecimal settlementExpense = defaultZero(request.settlementExpense());
		BigDecimal settlementTax = defaultZero(request.settlementTax());
		if (settlementExpense.signum() < 0 || settlementTax.signum() < 0) {
			throw new IllegalArgumentException("준공정산 경비와 세금은 0원 이상이어야 합니다.");
		}
		ProjectSettlementSnapshot snapshot = calculate(projectId, revision, settlementExpense, settlementTax);

		if (snapshot.unpaidBalance().signum() > 0) {
			if (!Boolean.TRUE.equals(request.forceCompleteIfUnpaid())) {
				throw new IllegalArgumentException("미지급 잔액이 남아있습니다. 강제 완료 여부와 미지급 처리 사유를 확인해 주세요.");
			}
			if (!StringUtils.hasText(request.unpaidHandlingReason())) {
				throw new IllegalArgumentException("미지급 잔액이 있는 경우 미지급 처리 사유가 필요합니다.");
			}
		}

		ProjectSettlement settlement = settlementRepository.findByProjectIdAndRevisionId(projectId, revision == null ? null : revision.getRevisionId())
				.orElseGet(() -> new ProjectSettlement(nextSettlementId(), projectId, revision == null ? null : revision.getRevisionId()));
		settlement.requestApproval(
				snapshot,
				uncollectedReceivable,
				settlementExpense,
				settlementTax,
				request.settlementNotes(),
				request.unpaidHandlingReason()
		);
		settlementRepository.save(settlement);
		approvalRepository.save(new Approval(
				nextSettlementApprovalId(), projectId, "FINAL_SETTLEMENT", settlement.getSettlementId(), "준공정산 최종 승인 요청",
				snapshot.finalCost(), snapshot.finalContractAmount(), snapshot.finalProfit(),
				StringUtils.hasText(request.settledBy()) ? request.settledBy() : "system", request.settlementNotes()
		));
		project.changeStatus(ProjectStatus.정산중);
		projectRepository.save(project);
		return toResponse(project, revision, snapshot, settlement, uncollectedReceivable, settlementExpense, settlementTax, approvedSiteExpense(projectId, revision));
	}

	@Transactional
	public void approveRequestedSettlement(String projectId, String settlementId, String approver) {
		ProjectSettlement settlement = settlementRepository.findById(settlementId)
				.orElseThrow(() -> new EntityNotFoundException("준공정산 요청을 찾을 수 없습니다: " + settlementId));
		if (!projectId.equals(settlement.getProjectId()) || !"REQUESTED".equals(settlement.getSettlementStatus())) {
			throw new IllegalStateException("결재 대기 중인 준공정산 요청만 승인할 수 있습니다.");
		}
		settlement.approve(approver);
		settlementRepository.save(settlement);
		Project project = getProject(projectId);
		project.changeStatus(ProjectStatus.정산완료);
		projectRepository.save(project);
	}

	@Transactional
	public void rejectRequestedSettlement(String projectId, String settlementId) {
		ProjectSettlement settlement = settlementRepository.findById(settlementId)
				.orElseThrow(() -> new EntityNotFoundException("준공정산 요청을 찾을 수 없습니다: " + settlementId));
		if (!projectId.equals(settlement.getProjectId()) || !"REQUESTED".equals(settlement.getSettlementStatus())) {
			throw new IllegalStateException("결재 대기 중인 준공정산 요청만 반려할 수 있습니다.");
		}
		settlement.reject();
		settlementRepository.save(settlement);
	}

	public void assertNotSettled(String projectId) {
		Project project = getProject(projectId);
		ProjectSettlement settlement = settlementRepository.findByProjectId(projectId).orElse(null);
		if (project.getStatus() == ProjectStatus.정산완료 || (settlement != null && settlement.isSettled())) {
			throw new IllegalStateException("정산 완료된 프로젝트는 추가 등록 또는 변경할 수 없습니다.");
		}
		if (settlement != null && "REQUESTED".equals(settlement.getSettlementStatus())) {
			throw new IllegalStateException("준공정산 최종 결재 대기 중에는 내용을 변경할 수 없습니다.");
		}
	}

	private ProjectSettlementSnapshot calculate(String projectId, ProjectRevision revision, BigDecimal settlementExpense, BigDecimal settlementTax) {
		AmountBreakdown contractAmounts = revision == null ? legacyContractAmounts(projectId) : revisionContractAmounts(revision);
		AmountBreakdown budgetAmounts = revision == null ? legacyBudgetAmounts(projectId) : revisionBudgetAmounts(revision);
		BigDecimal initialContractAmount = contractAmounts.initialAmount();
		BigDecimal changeContractAmount = contractAmounts.changeAmount();
		BigDecimal finalContractAmount = contractAmounts.finalAmount();
		BigDecimal initialBudgetAmount = budgetAmounts.initialAmount();
		BigDecimal changeBudgetAmount = budgetAmounts.changeAmount();
		BigDecimal finalBudgetAmount = budgetAmounts.finalAmount();

		List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findByProjectIdAndRevisionIdOrderByPoIdDesc(
				projectId, revision == null ? null : revision.getRevisionId());
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
		BigDecimal finalCost = (finalPoAmount.signum() > 0 ? finalPoAmount : actualPaidTotal)
				.add(defaultZero(settlementExpense))
				.add(defaultZero(settlementTax));
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
				defaultZero(settlementExpense),
				defaultZero(settlementTax),
				finalCost,
				finalProfit,
				profitMarginRate
		);
	}

	private ProjectSettlementResponse toResponse(Project project, ProjectRevision revision, ProjectSettlementSnapshot snapshot, ProjectSettlement settlement, BigDecimal uncollectedReceivable, BigDecimal settlementExpense, BigDecimal settlementTax, BigDecimal approvedSiteExpense) {
		return new ProjectSettlementResponse(
				project.getProjectId(),
				project.getProjectCode(),
				project.getProjectName(),
				revision == null ? null : revision.getRevisionNumber(),
				new AmountSummary(snapshot.initialContractAmount(), snapshot.changeContractAmount(), snapshot.finalContractAmount()),
				new AmountSummary(snapshot.initialBudgetAmount(), snapshot.changeBudgetAmount(), snapshot.finalBudgetAmount()),
				new AmountSummary(snapshot.initialPoAmount(), snapshot.changePoAmount(), snapshot.finalPoAmount()),
				settlementExpense,
				approvedSiteExpense,
				settlementTax,
				new PaymentSummary(snapshot.approvedClaimsTotal(), snapshot.actualPaidTotal(), snapshot.unpaidBalance(), defaultZero(uncollectedReceivable)),
				new Profitability(snapshot.finalCost(), snapshot.finalProfit(), snapshot.profitMarginRate()),
				settlement != null && settlement.isSettled(),
				settlement == null ? null : settlement.getSettlementId(),
				settlement == null ? "DRAFT" : settlement.getSettlementStatus(),
				settlement == null ? null : settlement.getSettlementNotes(),
				settlement == null ? null : settlement.getUnpaidHandlingReason(),
				settlement == null ? null : settlement.getSettledBy(),
				settlement == null ? null : settlement.getSettledAt()
		);
	}

	private ProjectRevision currentRevision(String projectId) {
		return revisionRepository.findTopByProjectIdAndStatusOrderByRevisionNumberDesc(projectId, "APPROVED").orElse(null);
	}

	private AmountBreakdown revisionContractAmounts(ProjectRevision revision) {
		BigDecimal initialAmount = contract(revision.getBaseContractSnapshot()).items().stream()
				.map(ContractItem::contractAmount).map(ProjectSettlementService::defaultZero).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal finalAmount = contract(revision.getContractSnapshot()).items().stream()
				.map(ContractItem::contractAmount).map(ProjectSettlementService::defaultZero).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new AmountBreakdown(initialAmount, finalAmount.subtract(initialAmount), finalAmount);
	}

	private AmountBreakdown revisionBudgetAmounts(ProjectRevision revision) {
		BigDecimal initialAmount = budget(revision.getBaseBudgetSnapshot()).details().stream()
				.map(this::budgetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal finalAmount = budget(revision.getBudgetSnapshot()).details().stream()
				.map(this::budgetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new AmountBreakdown(initialAmount, finalAmount.subtract(initialAmount), finalAmount);
	}

	private AmountBreakdown legacyContractAmounts(String projectId) {
		List<CustomerContractItem> contractItems = customerContractItemRepository.findByProjectIdOrderByWorkCategoryAsc(projectId);
		BigDecimal initialAmount = contractItems.isEmpty()
				? projectContractRepository.findByProjectId(projectId).stream().map(contract -> defaultZero(contract.getInitialAmount())).reduce(BigDecimal.ZERO, BigDecimal::add)
				: contractItems.stream().map(CustomerContractItem::getContractAmount).map(ProjectSettlementService::defaultZero).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal changeAmount = contractChangeRepository.findByProjectIdAndApprovalStatus(projectId, ApprovalStatus.APPROVED).stream()
				.map(ContractChange::getChangeAmount).map(ProjectSettlementService::defaultZero).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new AmountBreakdown(initialAmount, changeAmount, initialAmount.add(changeAmount));
	}

	private AmountBreakdown legacyBudgetAmounts(String projectId) {
		List<ProjectExecutionBudgetItem> rolledUpItems = executionBudgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId);
		BigDecimal initialAmount = rolledUpItems.isEmpty()
				? executionBudgetRepository.findByProjectId(projectId).stream().map(ExecutionBudget::getInitialBudget).map(ProjectSettlementService::defaultZero).reduce(BigDecimal.ZERO, BigDecimal::add)
				: rolledUpItems.stream().map(ProjectExecutionBudgetItem::getBudgetAmount).map(ProjectSettlementService::defaultZero).reduce(BigDecimal.ZERO, BigDecimal::add);
		List<String> budgetIds = executionBudgetRepository.findByProjectId(projectId).stream().map(ExecutionBudget::getBudgetId).toList();
		BigDecimal changeAmount = budgetIds.isEmpty() ? BigDecimal.ZERO
				: budgetChangeRepository.findByBudgetIdInAndApprovalStatus(budgetIds, ApprovalStatus.APPROVED).stream()
						.map(BudgetChange::getChangeAmount).map(ProjectSettlementService::defaultZero).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new AmountBreakdown(initialAmount, changeAmount, initialAmount.add(changeAmount));
	}

	private BigDecimal approvedSiteExpense(String projectId, ProjectRevision revision) {
		if (revision != null) {
			return budget(revision.getBudgetSnapshot()).details().stream()
					.filter(detail -> "현장경비".equals(detail.costType()))
					.map(this::budgetAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
		}
		Map<String, Boolean> approvedByBudgetItem = executionBudgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId).stream()
				.collect(Collectors.toMap(
						item -> ProjectExecutionBudgetItem.budgetItemId(projectId, item.getItemId()),
						ProjectExecutionBudgetItem::isApproved,
						(left, right) -> left
				));
		return executionBudgetDetailRepository.findByProjectIdOrderByBudgetItemIdAscDetailIdAsc(projectId).stream()
				.filter(detail -> "현장경비".equals(detail.getCostType()))
				.filter(detail -> approvedByBudgetItem.getOrDefault(detail.getBudgetItemId(), false))
				.map(ProjectExecutionBudgetDetail::getAmount)
				.map(ProjectSettlementService::defaultZero)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private ContractSnapshot contract(String snapshot) {
		try {
			return objectMapper.readValue(snapshot, ContractSnapshot.class);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Unable to read the contract revision snapshot.", ex);
		}
	}

	private BudgetSnapshot budget(String snapshot) {
		try {
			return objectMapper.readValue(snapshot, BudgetSnapshot.class);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Unable to read the execution-budget revision snapshot.", ex);
		}
	}

	private BigDecimal budgetAmount(BudgetDetail detail) {
		return defaultZero(detail.quantity()).multiply(defaultZero(detail.unitPrice()));
	}

	private Project getProject(String projectId) {
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
	}

	private String nextSettlementId() {
		return "STL-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}

	private String nextSettlementApprovalId() {
		return "APR-STL-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}

	private static BigDecimal defaultZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private record AmountBreakdown(BigDecimal initialAmount, BigDecimal changeAmount, BigDecimal finalAmount) { }
	private record ContractSnapshot(BigDecimal targetProfitRate, List<ContractItem> items) { }
	private record BudgetSnapshot(List<BudgetDetail> details) { }
}
