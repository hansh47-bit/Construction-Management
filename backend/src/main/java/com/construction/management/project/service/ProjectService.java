package com.construction.management.project.service;

import com.construction.management.project.api.IntegratedSummaryResponse;
import com.construction.management.project.api.IntegratedSummaryResponse.CostBreakdown;
import com.construction.management.project.api.IntegratedSummaryResponse.FlowSummary;
import com.construction.management.project.api.IntegratedSummaryResponse.ProfitAndBalance;
import com.construction.management.project.api.IntegratedSummaryResponse.ProjectInfo;
import com.construction.management.project.api.ProjectCreateRequest;
import com.construction.management.project.api.ProjectResponse;
import com.construction.management.project.api.ProjectRevisionResponse;
import com.construction.management.project.domain.ActualPayment;
import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.BudgetChange;
import com.construction.management.project.domain.ContractChange;
import com.construction.management.project.domain.CustomerContractItem;
import com.construction.management.project.domain.ExecutionBudget;
import com.construction.management.project.domain.PoChange;
import com.construction.management.project.domain.ProgressClaim;
import com.construction.management.project.domain.Project;
import com.construction.management.project.domain.ProjectExecutionBudgetDetail;
import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import com.construction.management.project.domain.ProjectRevision;
import com.construction.management.project.domain.ProjectStatus;
import com.construction.management.project.domain.PurchaseOrder;
import com.construction.management.project.repository.ActualPaymentRepository;
import com.construction.management.project.repository.ApprovalRepository;
import com.construction.management.project.repository.BudgetChangeRepository;
import com.construction.management.project.repository.ChangeOrderRepository;
import com.construction.management.project.repository.ContractChangeRepository;
import com.construction.management.project.repository.CustomerContractItemRepository;
import com.construction.management.project.repository.ExecutionBudgetRepository;
import com.construction.management.project.repository.PaymentRequestRepository;
import com.construction.management.project.repository.PoChangeRepository;
import com.construction.management.project.repository.PoRequisitionRepository;
import com.construction.management.project.repository.ProgressClaimRepository;
import com.construction.management.project.repository.ProjectContractRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetDetailRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetItemRepository;
import com.construction.management.project.repository.ProjectRepository;
import com.construction.management.project.repository.ProjectRevisionRepository;
import com.construction.management.project.repository.ProjectSettlementRepository;
import com.construction.management.project.repository.PurchaseOrderRepository;
import com.construction.management.project.repository.VendorComparisonRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter PROJECT_CODE_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

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
	private final ApprovalRepository approvalRepository;
	private final ChangeOrderRepository changeOrderRepository;
	private final CustomerContractService customerContractService;
	private final ExecutionBudgetApprovalAttachmentService executionBudgetApprovalAttachmentService;
	private final VendorComparisonAttachmentService vendorComparisonAttachmentService;
	private final PaymentRequestRepository paymentRequestRepository;
	private final PoRequisitionRepository poRequisitionRepository;
	private final ProjectExecutionBudgetDetailRepository projectExecutionBudgetDetailRepository;
	private final ProjectSettlementRepository projectSettlementRepository;
	private final VendorComparisonRepository vendorComparisonRepository;
	private final ProjectRevisionRepository projectRevisionRepository;
	private final ProjectRevisionService projectRevisionService;
	private final DailyLogService dailyLogService;
	private final MeetingMinutesService meetingMinutesService;

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
			ActualPaymentRepository actualPaymentRepository,
			ApprovalRepository approvalRepository,
			ChangeOrderRepository changeOrderRepository,
			CustomerContractService customerContractService,
			ExecutionBudgetApprovalAttachmentService executionBudgetApprovalAttachmentService,
			VendorComparisonAttachmentService vendorComparisonAttachmentService,
			PaymentRequestRepository paymentRequestRepository,
			PoRequisitionRepository poRequisitionRepository,
			ProjectExecutionBudgetDetailRepository projectExecutionBudgetDetailRepository,
			ProjectSettlementRepository projectSettlementRepository,
			VendorComparisonRepository vendorComparisonRepository,
			ProjectRevisionRepository projectRevisionRepository,
			ProjectRevisionService projectRevisionService,
			DailyLogService dailyLogService,
			MeetingMinutesService meetingMinutesService
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
		this.approvalRepository = approvalRepository;
		this.changeOrderRepository = changeOrderRepository;
		this.customerContractService = customerContractService;
		this.executionBudgetApprovalAttachmentService = executionBudgetApprovalAttachmentService;
		this.vendorComparisonAttachmentService = vendorComparisonAttachmentService;
		this.paymentRequestRepository = paymentRequestRepository;
		this.poRequisitionRepository = poRequisitionRepository;
		this.projectExecutionBudgetDetailRepository = projectExecutionBudgetDetailRepository;
		this.projectSettlementRepository = projectSettlementRepository;
		this.vendorComparisonRepository = vendorComparisonRepository;
		this.projectRevisionRepository = projectRevisionRepository;
		this.projectRevisionService = projectRevisionService;
		this.dailyLogService = dailyLogService;
		this.meetingMinutesService = meetingMinutesService;
	}

	@Transactional
	public ProjectResponse create(ProjectCreateRequest request) {
		String projectCode = nextProjectCode();

		Project project = new Project(toProjectId(projectCode), projectCode, request.projectName())
				.updateDetails(
						request.clientName(),
						request.siteAddress(),
						request.contractDate(),
						request.startDate(),
						request.endDate(),
						request.teamLeaderId(),
						request.managerId(),
						ProjectStatus.견적,
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

	@Transactional
	public void delete(String projectId) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));

		List<String> purchaseOrderIds = purchaseOrderRepository.findByProjectId(projectId).stream()
				.map(PurchaseOrder::getPoId)
				.toList();
		List<String> claimIds = purchaseOrderIds.isEmpty()
				? List.of()
				: progressClaimRepository.findByPoIdIn(purchaseOrderIds).stream()
						.map(ProgressClaim::getClaimId)
						.toList();
		List<String> budgetIds = new ArrayList<>();
		budgetIds.addAll(executionBudgetRepository.findByProjectId(projectId).stream()
				.map(ExecutionBudget::getBudgetId)
				.toList());
		budgetIds.addAll(projectExecutionBudgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId).stream()
				.map(ProjectExecutionBudgetItem::getItemId)
				.toList());

		if (!claimIds.isEmpty()) {
			actualPaymentRepository.deleteByClaimIdIn(claimIds);
		}
		if (!purchaseOrderIds.isEmpty()) {
			progressClaimRepository.deleteByPoIdIn(purchaseOrderIds);
			poChangeRepository.deleteByPoIdIn(purchaseOrderIds);
		}
		if (!budgetIds.isEmpty()) {
			budgetChangeRepository.deleteByBudgetIdIn(budgetIds);
		}

		customerContractService.deleteFilesForProject(projectId);
		executionBudgetApprovalAttachmentService.deleteAllForProject(projectId);
		vendorComparisonAttachmentService.deleteAllForProject(projectId);
		dailyLogService.deleteAllForProject(projectId);
		meetingMinutesService.deleteAllForProject(projectId);
		approvalRepository.deleteByProjectId(projectId);
		paymentRequestRepository.deleteByProjectId(projectId);
		poRequisitionRepository.deleteByProjectId(projectId);
		vendorComparisonRepository.deleteByProjectId(projectId);
		purchaseOrderRepository.deleteByProjectId(projectId);
		projectExecutionBudgetDetailRepository.deleteByProjectId(projectId);
		projectExecutionBudgetItemRepository.deleteByProjectId(projectId);
		executionBudgetRepository.deleteByProjectId(projectId);
		contractChangeRepository.deleteByProjectId(projectId);
		changeOrderRepository.deleteByProjectId(projectId);
		projectRevisionRepository.deleteByProjectId(projectId);
		customerContractItemRepository.deleteByProjectId(projectId);
		projectContractRepository.deleteByProjectId(projectId);
		projectSettlementRepository.deleteByProjectId(projectId);
		projectRepository.delete(project);
	}

	@Transactional
	public ProjectResponse startConstruction(String projectId) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
		if (project.getStatus() != ProjectStatus.공사준비) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"실행예산 승인 후 공사준비 상태의 프로젝트만 공사를 시작할 수 있습니다."
			);
		}
		project.changeStatus(ProjectStatus.공사진행);
		return ProjectResponse.from(projectRepository.save(project));
	}

	@Transactional
	public ProjectResponse synchronizePreparationStatus(String projectId) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
		if (project.getStatus() != ProjectStatus.계약) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"계약 상태의 프로젝트만 실행예산 승인 상태를 보정할 수 있습니다."
			);
		}
		boolean hasApprovedExecutionBudget = approvalRepository.existsByProjectIdAndApprovalTypeAndStatus(
				projectId,
				ApprovalService.TYPE_EXECUTION_BUDGET,
				ApprovalStatus.APPROVED
		);
		if (!hasApprovedExecutionBudget) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"승인된 실행예산이 있어야 공사준비 상태로 전환할 수 있습니다."
			);
		}
		project.changeStatus(ProjectStatus.공사준비);
		return ProjectResponse.from(projectRepository.save(project));
	}

	@Transactional(readOnly = true)
	public IntegratedSummaryResponse getIntegratedSummary(String projectId) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));

		List<CustomerContractItem> customerContractItems = customerContractItemRepository.findByProjectIdOrderByWorkCategoryAsc(projectId);
		BigDecimal approvedContractChangeAmount = contractChangeRepository.findByProjectIdAndApprovalStatus(projectId, ApprovalStatus.APPROVED).stream()
				.map(ContractChange::getChangeAmount)
				.map(ProjectService::defaultZero)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal legacyCurrentContractAmount = customerContractItems.isEmpty()
				? projectContractRepository.findByProjectId(projectId).stream()
						.map(contract -> defaultZero(contract.getInitialAmount()))
						.reduce(BigDecimal.ZERO, BigDecimal::add)
						.add(approvedContractChangeAmount)
				: customerContractItems.stream()
						.map(CustomerContractItem::getContractAmount)
				.map(ProjectService::defaultZero)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.add(approvedContractChangeAmount);
		ProjectRevisionResponse approvedRevision = projectRevisionService.latestApprovedRevision(projectId).orElse(null);
		BigDecimal currentContractAmount = approvedRevision == null ? legacyCurrentContractAmount : approvedRevision.contractTotal();
		BigDecimal currentTargetProfitRate = approvedRevision == null ? project.getTargetProfitRate() : approvedRevision.targetProfitRate();

		List<ExecutionBudget> budgets = executionBudgetRepository.findByProjectId(projectId);
		List<String> budgetIds = budgets.stream().map(ExecutionBudget::getBudgetId).toList();
		Map<String, BigDecimal> budgetChangesByBudget = budgetIds.isEmpty()
				? Map.of()
				: budgetChangeRepository.findByBudgetIdInAndApprovalStatus(budgetIds, ApprovalStatus.APPROVED).stream()
						.collect(Collectors.groupingBy(
								BudgetChange::getBudgetId,
								Collectors.mapping(change -> defaultZero(change.getChangeAmount()), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
						));

		String currentRevisionId = projectRevisionRepository.findTopByProjectIdAndStatusOrderByRevisionNumberDesc(projectId, "APPROVED")
				.map(ProjectRevision::getRevisionId)
				.orElse(null);
		List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findByProjectIdAndRevisionIdOrderByPoIdDesc(projectId, currentRevisionId);
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
		List<String> rolledUpBudgetIds = rolledUpExecutionBudgetItems.stream()
				.flatMap(item -> java.util.stream.Stream.of(
						item.getItemId(),
						ProjectExecutionBudgetItem.budgetItemId(projectId, item.getItemId())
				))
				.distinct()
				.toList();
		Map<String, BigDecimal> rolledUpBudgetChanges = rolledUpBudgetIds.isEmpty() ? Map.of() : budgetChangeRepository.findByBudgetIdInAndApprovalStatus(rolledUpBudgetIds, ApprovalStatus.APPROVED).stream()
				.collect(Collectors.groupingBy(BudgetChange::getBudgetId, Collectors.mapping(change -> defaultZero(change.getChangeAmount()), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
		BigDecimal currentExecutionBudget = rolledUpExecutionBudgetItems.isEmpty()
				? budgets.stream()
						.map(budget -> currentBudgetAmount(budget, budgetChangesByBudget))
						.reduce(BigDecimal.ZERO, BigDecimal::add)
				: rolledUpExecutionBudgetItems.stream()
						.map(item -> currentRolledUpExecutionBudgetAmount(projectId, item, rolledUpBudgetChanges))
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
		Map<String, List<ProjectExecutionBudgetDetail>> executionDetailsByBudgetItem = projectExecutionBudgetDetailRepository
				.findByProjectIdOrderByBudgetItemIdAscDetailIdAsc(projectId).stream()
				.collect(Collectors.groupingBy(ProjectExecutionBudgetDetail::getBudgetItemId));
		Set<String> directSettlementOnlyBudgetItemIds = executionDetailsByBudgetItem.entrySet().stream()
				.filter(entry -> !entry.getValue().isEmpty())
				.filter(entry -> entry.getValue().stream().allMatch(detail -> ProjectExecutionBudgetDetail.isDirectSettlementCostType(detail.getCostType())))
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		BigDecimal unpurchasedBudgetAmount = rolledUpExecutionBudgetItems.isEmpty()
				? budgets.stream()
						.filter(budget -> !purchasedBudgetIds.contains(budget.getBudgetId()))
						.map(budget -> currentBudgetAmount(budget, budgetChangesByBudget))
						.reduce(BigDecimal.ZERO, BigDecimal::add)
				: rolledUpExecutionBudgetItems.stream()
						.filter(item -> !purchasedBudgetIds.contains(ProjectExecutionBudgetItem.budgetItemId(projectId, item.getItemId())))
						.map(item -> currentRolledUpExecutionBudgetAmount(projectId, item, rolledUpBudgetChanges))
						.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal unexecutedDirectSettlementAmount = rolledUpExecutionBudgetItems.isEmpty()
				? BigDecimal.ZERO
				: rolledUpExecutionBudgetItems.stream()
						.filter(item -> !purchasedBudgetIds.contains(ProjectExecutionBudgetItem.budgetItemId(projectId, item.getItemId())))
						.filter(item -> directSettlementOnlyBudgetItemIds.contains(ProjectExecutionBudgetItem.budgetItemId(projectId, item.getItemId())))
						.map(item -> currentRolledUpExecutionBudgetAmount(projectId, item, rolledUpBudgetChanges))
						.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal unpurchasedProcurementBudgetAmount = unpurchasedBudgetAmount.subtract(unexecutedDirectSettlementAmount);

		BigDecimal expectedFinalCost = currentPurchaseAmount.add(unpurchasedBudgetAmount);
		BigDecimal expectedProfit = currentContractAmount.subtract(expectedFinalCost);
		BigDecimal expectedProfitRate = currentContractAmount.compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO
				: expectedProfit.multiply(BigDecimal.valueOf(100)).divide(currentContractAmount, 1, RoundingMode.HALF_UP);
		BigDecimal targetCostLimit = currentContractAmount
				.multiply(BigDecimal.ONE.subtract(defaultZero(currentTargetProfitRate).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
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
						unpurchasedProcurementBudgetAmount,
						unexecutedDirectSettlementAmount,
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
		String prefix = "P-" + LocalDate.now(KOREA_ZONE_ID).format(PROJECT_CODE_DATE_FORMAT) + "-";
		int nextSequence = projectRepository.findByProjectCodeStartingWithOrderByProjectCodeDesc(prefix).stream()
				.map(Project::getProjectCode)
				.map(code -> code.substring(prefix.length()))
				.mapToInt(sequence -> {
					try {
						return Integer.parseInt(sequence);
					} catch (NumberFormatException ignored) {
						return 0;
					}
				})
				.max()
				.orElse(0) + 1;
		return "%s%03d".formatted(prefix, nextSequence);
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

	private static BigDecimal currentRolledUpExecutionBudgetAmount(
			String projectId,
			ProjectExecutionBudgetItem item,
			Map<String, BigDecimal> changesByBudget
	) {
		BigDecimal baseAmount = item.isApproved()
				? defaultZero(item.getApprovedBudgetAmount())
				: defaultZero(item.getBudgetAmount());
		String sourceBudgetItemId = ProjectExecutionBudgetItem.budgetItemId(projectId, item.getItemId());
		BigDecimal changes = defaultZero(changesByBudget.get(item.getItemId()));
		if (!sourceBudgetItemId.equals(item.getItemId())) {
			changes = changes.add(defaultZero(changesByBudget.get(sourceBudgetItemId)));
		}
		return baseAmount.add(changes);
	}

	private static BigDecimal currentPoAmount(PurchaseOrder po, Map<String, BigDecimal> changesByPo) {
		return defaultZero(po.getInitialPoAmount()).add(changesByPo.getOrDefault(po.getPoId(), BigDecimal.ZERO));
	}

	private static BigDecimal defaultZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
