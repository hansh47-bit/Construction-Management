package com.construction.management.project.service;

import com.construction.management.project.api.IntegratedSummaryResponse;
import com.construction.management.project.api.IntegratedSummaryResponse.CostBreakdown;
import com.construction.management.project.api.IntegratedSummaryResponse.FlowSummary;
import com.construction.management.project.api.IntegratedSummaryResponse.ProfitAndBalance;
import com.construction.management.project.api.IntegratedSummaryResponse.ProjectInfo;
import com.construction.management.project.api.ProjectCreateRequest;
import com.construction.management.project.api.ProjectResponse;
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
import com.construction.management.project.repository.PurchaseOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final ProjectContractRepository projectContractRepository;
	private final ContractChangeRepository contractChangeRepository;
	private final CustomerContractItemRepository customerContractItemRepository;
	private final ProjectExecutionBudgetItemRepository projectExecutionBudgetItemRepository;
	private final ExecutionBudgetRepository executionBudgetRepository;
	private final BudgetChangeRepository budgetChangeRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final PoChangeRepository poChangeRepository;
	private final ProgressClaimRepository progressClaimRepository;
	private final ActualPaymentRepository actualPaymentRepository;

	public ProjectService(
			ProjectRepository projectRepository,
			ProjectContractRepository projectContractRepository,
			ContractChangeRepository contractChangeRepository,
			CustomerContractItemRepository customerContractItemRepository,
			ProjectExecutionBudgetItemRepository projectExecutionBudgetItemRepository,
			ExecutionBudgetRepository executionBudgetRepository,
			BudgetChangeRepository budgetChangeRepository,
			PurchaseOrderRepository purchaseOrderRepository,
			PoChangeRepository poChangeRepository,
			ProgressClaimRepository progressClaimRepository,
			ActualPaymentRepository actualPaymentRepository
	) {
		this.projectRepository = projectRepository;
		this.projectContractRepository = projectContractRepository;
		this.contractChangeRepository = contractChangeRepository;
		this.customerContractItemRepository = customerContractItemRepository;
		this.projectExecutionBudgetItemRepository = projectExecutionBudgetItemRepository;
		this.executionBudgetRepository = executionBudgetRepository;
		this.budgetChangeRepository = budgetChangeRepository;
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.poChangeRepository = poChangeRepository;
		this.progressClaimRepository = progressClaimRepository;
		this.actualPaymentRepository = actualPaymentRepository;
	}

	@Transactional
	public ProjectResponse create(ProjectCreateRequest request) {
		String projectCode = StringUtils.hasText(request.projectCode()) ? request.projectCode() : nextProjectCode();
		if (projectRepository.existsByProjectCode(projectCode)) {
			throw new IllegalArgumentException("이미 등록된 프로젝트번호입니다: " + projectCode);
		}

		Project project = new Project(toProjectId(projectCode), projectCode, request.projectName())
				.updateDetails(
						request.clientName(),
						request.siteAddress(),
						request.contractDate(),
						request.startDate(),
						request.endDate(),
						request.teamLeaderId(),
						request.managerId(),
						request.status(),
						request.targetProfitRate(),
						request.note()
				);

		return ProjectResponse.from(projectRepository.save(project));
	}

	@Transactional(readOnly = true)
	public List<ProjectResponse> search(String keyword) {
		List<Project> projects = StringUtils.hasText(keyword)
				? projectRepository.findTop20ByProjectNameContainingIgnoreCaseOrProjectCodeContainingIgnoreCaseOrderByCreatedAtDesc(keyword, keyword)
				: projectRepository.findTop20ByOrderByCreatedAtDesc();

		return projects.stream().map(project -> {
			List<CustomerContractItem> items = customerContractItemRepository.findByProjectIdOrderByWorkCategoryAsc(project.getProjectId());
			BigDecimal supplyAmount = items.stream()
					.map(CustomerContractItem::getContractAmount)
					.map(ProjectService::defaultZero)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal totalAmount = items.stream()
					.map(CustomerContractItem::getTotalAmount)
					.map(ProjectService::defaultZero)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			return ProjectResponse.from(project, supplyAmount, totalAmount);
		}).toList();
	}

	@Transactional(readOnly = true)
	public IntegratedSummaryResponse getIntegratedSummary(String projectId) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));

		List<CustomerContractItem> customerContractItems = customerContractItemRepository.findByProjectIdOrderByWorkCategoryAsc(projectId);
		BigDecimal currentContractAmount = customerContractItems.isEmpty()
				? projectContractRepository.findByProjectId(projectId).stream()
						.map(contract -> defaultZero(contract.getInitialAmount()))
						.reduce(BigDecimal.ZERO, BigDecimal::add)
						.add(contractChangeRepository.findByProjectIdAndApprovalStatus(projectId, ApprovalStatus.APPROVED).stream()
								.map(ContractChange::getChangeAmount)
								.map(ProjectService::defaultZero)
								.reduce(BigDecimal.ZERO, BigDecimal::add))
				: customerContractItems.stream()
						.map(CustomerContractItem::getContractAmount)
						.map(ProjectService::defaultZero)
						.reduce(BigDecimal.ZERO, BigDecimal::add);

		List<ExecutionBudget> budgets = executionBudgetRepository.findByProjectId(projectId);
		List<String> budgetIds = budgets.stream().map(ExecutionBudget::getBudgetId).toList();
		Map<String, BigDecimal> budgetChangesByBudget = budgetIds.isEmpty()
				? Map.of()
				: budgetChangeRepository.findByBudgetIdInAndApprovalStatus(budgetIds, ApprovalStatus.APPROVED).stream()
						.collect(Collectors.groupingBy(
								BudgetChange::getBudgetId,
								Collectors.mapping(change -> defaultZero(change.getChangeAmount()), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
						));

		List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findByProjectId(projectId);
		List<String> poIds = purchaseOrders.stream().map(PurchaseOrder::getPoId).toList();
		Map<String, BigDecimal> poChangesByPo = poIds.isEmpty()
				? Map.of()
				: poChangeRepository.findByPoIdInAndApprovalStatus(poIds, ApprovalStatus.APPROVED).stream()
						.collect(Collectors.groupingBy(
								PoChange::getPoId,
								Collectors.mapping(change -> defaultZero(change.getChangeAmount()), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
						));

		Map<String, BigDecimal> purchaseByBudget = purchaseOrders.stream()
				.collect(Collectors.groupingBy(
						PurchaseOrder::getBudgetId,
						Collectors.mapping(po -> currentPoAmount(po, poChangesByPo), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
				));

		List<ProgressClaim> claims = poIds.isEmpty()
				? List.of()
				: progressClaimRepository.findByPoIdInAndApprovalStatus(poIds, ApprovalStatus.APPROVED);
		Map<String, BigDecimal> claimsByPo = claims.stream()
				.collect(Collectors.groupingBy(
						ProgressClaim::getPoId,
						Collectors.mapping(claim -> defaultZero(claim.getClaimAmount()), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
				));

		List<String> claimIds = claims.stream().map(ProgressClaim::getClaimId).toList();
		List<ActualPayment> payments = claimIds.isEmpty() ? List.of() : actualPaymentRepository.findByClaimIdIn(claimIds);
		Map<String, ProgressClaim> claimsById = claims.stream()
				.collect(Collectors.toMap(ProgressClaim::getClaimId, Function.identity()));
		Map<String, BigDecimal> paidByPo = payments.stream()
				.collect(Collectors.groupingBy(
						payment -> claimsById.get(payment.getClaimId()).getPoId(),
						Collectors.mapping(payment -> defaultZero(payment.getPaidAmount()), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
				));

		List<ProjectExecutionBudgetItem> rolledUpExecutionBudgetItems = projectExecutionBudgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId);
		BigDecimal currentExecutionBudget = rolledUpExecutionBudgetItems.isEmpty()
				? budgets.stream()
						.map(budget -> currentBudgetAmount(budget, budgetChangesByBudget))
						.reduce(BigDecimal.ZERO, BigDecimal::add)
				: rolledUpExecutionBudgetItems.stream()
						.filter(ProjectExecutionBudgetItem::isApproved)
						.map(ProjectExecutionBudgetItem::getApprovedBudgetAmount)
						.map(ProjectService::defaultZero)
						.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal currentPurchaseAmount = purchaseOrders.stream()
				.map(po -> currentPoAmount(po, poChangesByPo))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal accumulatedCompletedAmount = claims.stream()
				.map(ProgressClaim::getClaimAmount)
				.map(ProjectService::defaultZero)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal actualPaidAmount = payments.stream()
				.map(ActualPayment::getPaidAmount)
				.map(ProjectService::defaultZero)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		Set<String> purchasedBudgetIds = purchaseOrders.stream()
				.map(PurchaseOrder::getBudgetId)
				.filter(StringUtils::hasText)
				.collect(Collectors.toSet());
		BigDecimal unpurchasedBudgetAmount = rolledUpExecutionBudgetItems.isEmpty()
				? budgets.stream()
						.filter(budget -> !purchasedBudgetIds.contains(budget.getBudgetId()))
						.map(budget -> currentBudgetAmount(budget, budgetChangesByBudget))
						.reduce(BigDecimal.ZERO, BigDecimal::add)
				: rolledUpExecutionBudgetItems.stream()
						.filter(ProjectExecutionBudgetItem::isApproved)
						.filter(item -> !purchasedBudgetIds.contains(item.getItemId()))
						.map(ProjectExecutionBudgetItem::getApprovedBudgetAmount)
						.map(ProjectService::defaultZero)
						.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal expectedFinalCost = currentPurchaseAmount.add(unpurchasedBudgetAmount);
		BigDecimal expectedProfit = currentContractAmount.subtract(expectedFinalCost);
		BigDecimal expectedProfitRate = currentContractAmount.compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO
				: expectedProfit.multiply(BigDecimal.valueOf(100)).divide(currentContractAmount, 1, RoundingMode.HALF_UP);
		BigDecimal targetCostLimit = currentContractAmount
				.multiply(BigDecimal.ONE.subtract(defaultZero(project.getTargetProfitRate()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
				.setScale(0, RoundingMode.HALF_UP);
		BigDecimal unbilledAmount = currentPurchaseAmount.subtract(accumulatedCompletedAmount);
		BigDecimal approvedUnpaidAmount = accumulatedCompletedAmount.subtract(actualPaidAmount);
		BigDecimal totalUnpaidBalance = unbilledAmount.add(approvedUnpaidAmount);

		List<CostBreakdown> costBreakdown = budgets.stream()
				.map(budget -> {
					BigDecimal currentBudget = currentBudgetAmount(budget, budgetChangesByBudget);
					BigDecimal purchaseAmount = purchaseByBudget.getOrDefault(budget.getBudgetId(), BigDecimal.ZERO);
					BigDecimal completedAmount = purchaseOrders.stream()
							.filter(po -> budget.getBudgetId().equals(po.getBudgetId()))
							.map(po -> claimsByPo.getOrDefault(po.getPoId(), BigDecimal.ZERO))
							.reduce(BigDecimal.ZERO, BigDecimal::add);
					BigDecimal paidAmount = purchaseOrders.stream()
							.filter(po -> budget.getBudgetId().equals(po.getBudgetId()))
							.map(po -> paidByPo.getOrDefault(po.getPoId(), BigDecimal.ZERO))
							.reduce(BigDecimal.ZERO, BigDecimal::add);
					return new CostBreakdown(
							budget.getBudgetId(),
							budget.getWorkCategory(),
							defaultZero(budget.getInitialBudget()),
							currentBudget,
							purchaseAmount,
							completedAmount,
							paidAmount,
							purchaseAmount.subtract(currentBudget)
					);
				})
				.toList();

		return new IntegratedSummaryResponse(
				new ProjectInfo(
						project.getProjectId(),
						project.getProjectCode(),
						project.getProjectName(),
						project.getClientName(),
						project.getSiteAddress(),
						project.getStatus().name(),
						project.getTeamLeaderId(),
						project.getManagerId(),
						project.getTargetProfitRate()
				),
				new FlowSummary(
						currentContractAmount,
						currentExecutionBudget,
						currentPurchaseAmount,
						accumulatedCompletedAmount,
						actualPaidAmount
				),
				new ProfitAndBalance(
						targetCostLimit,
						unpurchasedBudgetAmount,
						expectedFinalCost,
						expectedProfit,
						expectedProfitRate,
						unbilledAmount,
						approvedUnpaidAmount,
						totalUnpaidBalance
				),
				costBreakdown
		);
	}

	private String nextProjectCode() {
		return "P-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
	}

	private String toProjectId(String projectCode) {
		String normalized = Normalizer.normalize(projectCode, Normalizer.Form.NFKD)
				.replaceAll("[^A-Za-z0-9]+", "_")
				.replaceAll("^_+|_+$", "")
				.toLowerCase(Locale.ROOT);
		return "prj_" + normalized;
	}

	private static BigDecimal currentBudgetAmount(ExecutionBudget budget, Map<String, BigDecimal> changesByBudget) {
		return defaultZero(budget.getInitialBudget()).add(changesByBudget.getOrDefault(budget.getBudgetId(), BigDecimal.ZERO));
	}

	private static BigDecimal currentPoAmount(PurchaseOrder po, Map<String, BigDecimal> changesByPo) {
		return defaultZero(po.getInitialPoAmount()).add(changesByPo.getOrDefault(po.getPoId(), BigDecimal.ZERO));
	}

	private static BigDecimal defaultZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
