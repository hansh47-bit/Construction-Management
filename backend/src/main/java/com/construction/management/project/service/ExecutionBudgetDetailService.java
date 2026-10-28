package com.construction.management.project.service;

import com.construction.management.project.api.ExecutionBudgetDetailRequest;
import com.construction.management.project.api.ExecutionBudgetDetailResponse;
import com.construction.management.project.api.ExecutionBudgetDetailResponse.Detail;
import com.construction.management.project.api.ExecutionBudgetDetailResponse.SummaryItem;
import com.construction.management.project.api.ProjectRevisionBudgetView;
import com.construction.management.project.api.ProjectRevisionResponse;
import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.BudgetChange;
import com.construction.management.project.domain.ContractChange;
import com.construction.management.project.domain.CustomerContractItem;
import com.construction.management.project.domain.Project;
import com.construction.management.project.domain.ProjectExecutionBudgetDetail;
import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import com.construction.management.project.domain.PoRequisition;
import com.construction.management.project.repository.CustomerContractItemRepository;
import com.construction.management.project.repository.BudgetChangeRepository;
import com.construction.management.project.repository.ContractChangeRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetDetailRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetItemRepository;
import com.construction.management.project.repository.ProjectRepository;
import com.construction.management.project.repository.PoRequisitionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ExecutionBudgetDetailService {

	private final ProjectRepository projectRepository;
	private final CustomerContractItemRepository customerContractItemRepository;
	private final ProjectExecutionBudgetItemRepository itemRepository;
	private final ProjectExecutionBudgetDetailRepository detailRepository;
	private final ProjectSettlementService projectSettlementService;
	private final BudgetChangeRepository budgetChangeRepository;
	private final ContractChangeRepository contractChangeRepository;
	private final PoRequisitionRepository poRequisitionRepository;
	private final ProjectRevisionService projectRevisionService;

	public ExecutionBudgetDetailService(
			ProjectRepository projectRepository,
			CustomerContractItemRepository customerContractItemRepository,
			ProjectExecutionBudgetItemRepository itemRepository,
			ProjectExecutionBudgetDetailRepository detailRepository,
			ProjectSettlementService projectSettlementService,
			BudgetChangeRepository budgetChangeRepository,
			ContractChangeRepository contractChangeRepository,
			PoRequisitionRepository poRequisitionRepository,
			ProjectRevisionService projectRevisionService
	) {
		this.projectRepository = projectRepository;
		this.customerContractItemRepository = customerContractItemRepository;
		this.itemRepository = itemRepository;
		this.detailRepository = detailRepository;
		this.projectSettlementService = projectSettlementService;
		this.budgetChangeRepository = budgetChangeRepository;
		this.contractChangeRepository = contractChangeRepository;
		this.poRequisitionRepository = poRequisitionRepository;
		this.projectRevisionService = projectRevisionService;
	}

	@Transactional(readOnly = true)
	public ExecutionBudgetDetailResponse getDetails(String projectId) {
		Project project = getProject(projectId);
		return toResponse(project, projectRevisionService.latestOpenBudget(projectId)
				.or(() -> projectRevisionService.latestApprovedBudget(projectId))
				.orElse(null));
	}

	@Transactional
	public ExecutionBudgetDetailResponse saveDetails(String projectId, ExecutionBudgetDetailRequest request) {
		projectSettlementService.assertNotSettled(projectId);
		Optional<ProjectRevisionBudgetView> writableRevision = projectRevisionService.latestWritableBudget(projectId);
		if (writableRevision.isPresent()) {
			projectRevisionService.saveBudget(projectId, writableRevision.get().revisionId(), request);
			return getDetails(projectId);
		}
		Project project = getProject(projectId);
		boolean locked = itemRepository.findByProjectIdOrderByItemIdAsc(projectId).stream()
				.anyMatch(item -> ApprovalStatus.REQUESTED.name().equals(item.getApprovalStatus()) || ApprovalStatus.APPROVED.name().equals(item.getApprovalStatus()));
		boolean rewriteAvailable = projectRevisionService.isExecutionBudgetRewriteAvailable(projectId);
		if (locked && !rewriteAvailable) {
			throw new IllegalStateException("결재 대기 또는 승인 완료 상태의 실행예산은 수정할 수 없습니다.");
		}
		if (locked) {
			itemRepository.findByProjectIdOrderByItemIdAsc(projectId).forEach(item -> {
				item.resetForBudgetRewrite();
				itemRepository.save(item);
			});
		}

		detailRepository.deleteByProjectId(projectId);
		List<ProjectExecutionBudgetDetail> details = request.details().stream()
				.filter(detail -> StringUtils.hasText(detail.budgetItemId()) && StringUtils.hasText(detail.categoryName()))
				.map(detail -> new ProjectExecutionBudgetDetail(
						"dtl_" + UUID.randomUUID().toString().replace("-", ""),
						projectId,
						detail.budgetItemId(),
						detail.categoryName(),
						detail.itemName(),
						StringUtils.hasText(detail.costType()) ? detail.costType() : "외주비",
						detail.vendorDescription(),
						detail.unit(),
						detail.quantity(),
						detail.unitPrice(),
						detail.evidenceLink(),
						detail.note()
				))
				.toList();
		detailRepository.saveAll(details);
		rollup(projectId, details);
		if (rewriteAvailable) {
			projectRevisionService.updateApprovedRevisionBudgetSnapshot(projectId, details);
		}

		return toResponse(project, null);
	}

	private void rollup(String projectId, List<ProjectExecutionBudgetDetail> details) {
		Map<String, List<ProjectExecutionBudgetDetail>> byItem = details.stream()
				.collect(Collectors.groupingBy(ProjectExecutionBudgetDetail::getBudgetItemId, LinkedHashMap::new, Collectors.toList()));
		Map<String, ProjectExecutionBudgetItem> itemsByBudgetItemId = itemRepository.findByProjectIdOrderByItemIdAsc(projectId).stream()
				.collect(Collectors.toMap(
						item -> ProjectExecutionBudgetItem.budgetItemId(projectId, item.getItemId()),
						item -> item,
						(left, right) -> left,
						LinkedHashMap::new
				));

		for (Map.Entry<String, List<ProjectExecutionBudgetDetail>> entry : byItem.entrySet()) {
			ProjectExecutionBudgetDetail first = entry.getValue().get(0);
			ProjectExecutionBudgetItem item = itemsByBudgetItemId.get(entry.getKey());
			if (item == null) {
				item = new ProjectExecutionBudgetItem(
						ProjectExecutionBudgetItem.storageId(projectId, entry.getKey()),
						projectId,
						first.getCategoryName(),
						first.getItemName()
				);
			}
			item.updateInfo(first.getCategoryName(), first.getItemName());
			item.updateBudgetAmount(entry.getValue().stream()
					.map(ProjectExecutionBudgetDetail::getAmount)
					.map(ExecutionBudgetDetailService::defaultZero)
					.reduce(BigDecimal.ZERO, BigDecimal::add));
			itemRepository.save(item);
		}

		itemRepository.findByProjectIdOrderByItemIdAsc(projectId).stream()
				.filter(item -> !byItem.containsKey(ProjectExecutionBudgetItem.budgetItemId(projectId, item.getItemId())))
				.forEach(item -> {
					item.updateBudgetAmount(BigDecimal.ZERO);
					itemRepository.save(item);
				});
	}

	private ExecutionBudgetDetailResponse toResponse(Project project, ProjectRevisionBudgetView appliedRevision) {
		List<Detail> details = appliedRevision == null
				? detailRepository.findByProjectIdOrderByBudgetItemIdAscDetailIdAsc(project.getProjectId()).stream()
						.map(detail -> new Detail(detail.getDetailId(), detail.getBudgetItemId(), detail.getCategoryName(), detail.getItemName(), detail.getCostType(), detail.getVendorDescription(), detail.getUnit(), detail.getQuantity(), detail.getUnitPrice(), detail.getAmount(), detail.getEvidenceLink(), detail.getNote()))
						.toList()
				: appliedRevision.details();
		List<ProjectExecutionBudgetItem> items = itemRepository.findByProjectIdOrderByItemIdAsc(project.getProjectId());
		Map<String, List<Detail>> detailsByItem = details.stream()
				.collect(Collectors.groupingBy(Detail::budgetItemId));
		String approvalStatus = appliedRevision == null ? executionBudgetApprovalStatus(items) : appliedRevision.status();
		String approvalId = appliedRevision == null ? items.stream().map(ProjectExecutionBudgetItem::getApprovalId).filter(StringUtils::hasText).findFirst().orElse(null) : null;
		String approvalComment = appliedRevision == null ? items.stream().map(ProjectExecutionBudgetItem::getApprovalComment).filter(StringUtils::hasText).findFirst().orElse(null) : null;
		boolean rewriteAvailable = appliedRevision == null && projectRevisionService.isExecutionBudgetRewriteAvailable(project.getProjectId());
		List<String> itemIds = items.stream().map(ProjectExecutionBudgetItem::getItemId).toList();
		Map<String, BigDecimal> changesByItem = itemIds.isEmpty() ? Map.of() : budgetChangeRepository.findByBudgetIdInAndApprovalStatus(itemIds, ApprovalStatus.APPROVED).stream()
				.collect(Collectors.groupingBy(BudgetChange::getBudgetId, Collectors.mapping(change -> defaultZero(change.getChangeAmount()), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
		Map<String, String> purchaseOrderStatusByItem = poRequisitionRepository.findByProjectIdOrderByCreatedAtDesc(project.getProjectId()).stream()
				.collect(Collectors.toMap(PoRequisition::getExecutionItemId, PoRequisition::getApprovalStatus, (latest, ignored) -> latest));
		BigDecimal totalDetailAmount = details.stream()
				.map(Detail::amount)
				.map(ExecutionBudgetDetailService::defaultZero)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal approvedAmount = appliedRevision == null
				? items.stream()
						.filter(ProjectExecutionBudgetItem::isApproved)
						.map(item -> defaultZero(item.getApprovedBudgetAmount()).add(changesByItem.getOrDefault(item.getItemId(), BigDecimal.ZERO)))
						.reduce(BigDecimal.ZERO, BigDecimal::add)
				: totalDetailAmount;
		BigDecimal storedContractSupplyAmount = customerContractItemRepository.findByProjectIdOrderByWorkCategoryAsc(project.getProjectId()).stream()
				.map(CustomerContractItem::getContractAmount)
				.map(ExecutionBudgetDetailService::defaultZero)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.add(contractChangeRepository.findByProjectIdAndApprovalStatus(project.getProjectId(), ApprovalStatus.APPROVED).stream()
						.map(ContractChange::getChangeAmount).map(ExecutionBudgetDetailService::defaultZero).reduce(BigDecimal.ZERO, BigDecimal::add));
		ProjectRevisionResponse approvedRevision = projectRevisionService.latestApprovedRevision(project.getProjectId()).orElse(null);
		BigDecimal contractSupplyAmount = approvedRevision == null ? storedContractSupplyAmount : approvedRevision.contractTotal();
		BigDecimal targetProfitRate = approvedRevision == null ? project.getTargetProfitRate() : approvedRevision.targetProfitRate();
		BigDecimal targetCostLimit = contractSupplyAmount
				.multiply(BigDecimal.ONE.subtract(defaultZero(targetProfitRate).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
				.setScale(0, RoundingMode.HALF_UP);

		return new ExecutionBudgetDetailResponse(
				project.getProjectId(),
				project.getProjectCode(),
				project.getProjectName(),
				contractSupplyAmount,
				targetCostLimit,
			approvalStatus,
			approvalId,
			approvalComment,
			rewriteAvailable,
			appliedRevision == null ? null : appliedRevision.revisionId(),
			appliedRevision == null ? null : appliedRevision.revisionNumber(),
			appliedRevision == null ? null : appliedRevision.status(),
			approvedAmount,
				totalDetailAmount,
				targetCostLimit.compareTo(BigDecimal.ZERO) > 0 && totalDetailAmount.compareTo(targetCostLimit) > 0,
				items.stream()
						.sorted(Comparator.comparing(ProjectExecutionBudgetItem::getItemId))
						.filter(item -> appliedRevision == null || details.stream().anyMatch(detail -> detail.budgetItemId().equals(ProjectExecutionBudgetItem.budgetItemId(project.getProjectId(), item.getItemId()))))
						.map(item -> new SummaryItem(ProjectExecutionBudgetItem.budgetItemId(project.getProjectId(), item.getItemId()), item.getCategoryName(), item.getItemName(), item.getApprovalStatus(),
						purchaseOrderStatusByItem.getOrDefault(ProjectExecutionBudgetItem.budgetItemId(project.getProjectId(), item.getItemId()), "NOT_REQUESTED"),
						requiresProcurement(detailsByItem.get(ProjectExecutionBudgetItem.budgetItemId(project.getProjectId(), item.getItemId()))),
						appliedRevision == null
								? (item.isApproved() ? defaultZero(item.getApprovedBudgetAmount()).add(changesByItem.getOrDefault(item.getItemId(), BigDecimal.ZERO)) : item.getBudgetAmount())
								: detailsByItem.getOrDefault(ProjectExecutionBudgetItem.budgetItemId(project.getProjectId(), item.getItemId()), List.of()).stream()
										.map(Detail::amount).map(ExecutionBudgetDetailService::defaultZero).reduce(BigDecimal.ZERO, BigDecimal::add)))
						.toList(),
			details
		);
	}

	private static String executionBudgetApprovalStatus(List<ProjectExecutionBudgetItem> items) {
		if (items.isEmpty()) {
			return ApprovalStatus.DRAFT.name();
		}
		if (items.stream().anyMatch(item -> ApprovalStatus.REQUESTED.name().equals(item.getApprovalStatus()))) {
			return ApprovalStatus.REQUESTED.name();
		}
		if (items.stream().allMatch(ProjectExecutionBudgetItem::isApproved)) {
			return ApprovalStatus.APPROVED.name();
		}
		if (items.stream().anyMatch(item -> ApprovalStatus.REJECTED.name().equals(item.getApprovalStatus()))) {
			return ApprovalStatus.REJECTED.name();
		}
		return ApprovalStatus.DRAFT.name();
	}

	private Project getProject(String projectId) {
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
	}

	private static BigDecimal defaultZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private static boolean requiresProcurement(List<Detail> details) {
		return details == null || details.isEmpty()
				|| details.stream().anyMatch(detail -> !ProjectExecutionBudgetDetail.isDirectSettlementCostType(detail.costType()));
	}
}
