package com.construction.management.project.service;

import com.construction.management.project.api.CustomerContractRequest;
import com.construction.management.project.api.ExecutionBudgetDetailRequest;
import com.construction.management.project.api.ApprovalRequest;
import com.construction.management.project.api.ApprovalResponse;
import com.construction.management.project.api.ProjectRevisionCreateRequest;
import com.construction.management.project.api.ProjectRevisionResponse;
import com.construction.management.project.api.ProjectRevisionProcurementResponse;
import com.construction.management.project.api.ProjectRevisionBudgetView;
import com.construction.management.project.api.ProjectRevisionHistoryResponse;
import com.construction.management.project.api.ExecutionBudgetDetailResponse.Detail;
import com.construction.management.project.api.ProjectRevisionResponse.BudgetDetail;
import com.construction.management.project.api.ProjectRevisionResponse.ContractItem;
import com.construction.management.project.domain.CustomerContractItem;
import com.construction.management.project.domain.ContractChange;
import com.construction.management.project.domain.BudgetChange;
import com.construction.management.project.domain.Approval;
import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.Project;
import com.construction.management.project.domain.ProjectExecutionBudgetDetail;
import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import com.construction.management.project.domain.ProjectRevision;
import com.construction.management.project.domain.VendorComparison;
import com.construction.management.project.domain.PurchaseOrder;
import com.construction.management.project.domain.ProgressClaim;
import com.construction.management.project.domain.ActualPayment;
import com.construction.management.project.repository.CustomerContractItemRepository;
import com.construction.management.project.repository.ContractChangeRepository;
import com.construction.management.project.repository.BudgetChangeRepository;
import com.construction.management.project.repository.ApprovalRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetDetailRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetItemRepository;
import com.construction.management.project.repository.ProjectRepository;
import com.construction.management.project.repository.ProjectRevisionRepository;
import com.construction.management.project.repository.VendorComparisonRepository;
import com.construction.management.project.repository.PurchaseOrderRepository;
import com.construction.management.project.repository.ProgressClaimRepository;
import com.construction.management.project.repository.ActualPaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectRevisionService {
	private final ProjectRepository projectRepository;
	private final CustomerContractItemRepository contractItemRepository;
	private final ProjectExecutionBudgetDetailRepository budgetDetailRepository;
	private final ProjectExecutionBudgetItemRepository budgetItemRepository;
	private final ProjectRevisionRepository revisionRepository;
	private final ApprovalRepository approvalRepository;
	private final ContractChangeRepository contractChangeRepository;
	private final BudgetChangeRepository budgetChangeRepository;
	private final ProjectSettlementService projectSettlementService;
	private final VendorComparisonRepository vendorComparisonRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final ProgressClaimRepository progressClaimRepository;
	private final ActualPaymentRepository actualPaymentRepository;
	private final ObjectMapper objectMapper;

	public ProjectRevisionService(
			ProjectRepository projectRepository,
			CustomerContractItemRepository contractItemRepository,
			ProjectExecutionBudgetDetailRepository budgetDetailRepository,
			ProjectExecutionBudgetItemRepository budgetItemRepository,
			ProjectRevisionRepository revisionRepository,
			ApprovalRepository approvalRepository,
			ContractChangeRepository contractChangeRepository,
			BudgetChangeRepository budgetChangeRepository,
			ProjectSettlementService projectSettlementService,
			VendorComparisonRepository vendorComparisonRepository,
			PurchaseOrderRepository purchaseOrderRepository,
			ProgressClaimRepository progressClaimRepository,
			ActualPaymentRepository actualPaymentRepository,
			ObjectMapper objectMapper
	) {
		this.projectRepository = projectRepository;
		this.contractItemRepository = contractItemRepository;
		this.budgetDetailRepository = budgetDetailRepository;
		this.budgetItemRepository = budgetItemRepository;
		this.revisionRepository = revisionRepository;
		this.approvalRepository = approvalRepository;
		this.contractChangeRepository = contractChangeRepository;
		this.budgetChangeRepository = budgetChangeRepository;
		this.projectSettlementService = projectSettlementService;
		this.vendorComparisonRepository = vendorComparisonRepository;
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.progressClaimRepository = progressClaimRepository;
		this.actualPaymentRepository = actualPaymentRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public ProjectRevisionResponse create(String projectId, ProjectRevisionCreateRequest request) {
		projectSettlementService.assertNotSettled(projectId);
		Project project = project(projectId);
		List<ProjectRevision> revisions = revisionRepository.findByProjectIdOrderByRevisionNumberDesc(projectId);
		ProjectRevision previous = revisions.isEmpty() ? null : revisions.get(0);
		String contractSnapshot = previous == null ? json(currentContract(project)) : previous.getContractSnapshot();
		String baseBudgetSnapshot = previous == null ? json(currentBudget(projectId)) : previous.getBudgetSnapshot();
		String budgetSnapshot = baseBudgetSnapshot;
		int revisionNumber = revisions.stream().mapToInt(ProjectRevision::getRevisionNumber).max().orElse(1) + 1;
		ProjectRevision revision = revisionRepository.save(new ProjectRevision(
				"rev_" + UUID.randomUUID().toString().replace("-", ""),
				projectId,
				revisionNumber,
				request.title(),
				request.reason(),
				contractSnapshot,
				contractSnapshot,
				baseBudgetSnapshot,
				budgetSnapshot
		));
		return response(revision);
	}

	@Transactional(readOnly = true)
	public List<ProjectRevisionResponse> list(String projectId) {
		project(projectId);
		return revisionRepository.findByProjectIdOrderByRevisionNumberDesc(projectId).stream().map(this::response).toList();
	}

	@Transactional(readOnly = true)
	public ProjectRevisionResponse get(String projectId, String revisionId) {
		return response(revision(projectId, revisionId));
	}

	@Transactional(readOnly = true)
	public ProjectRevisionHistoryResponse history(String projectId, String revisionId) {
		ProjectRevision revision = revision(projectId, revisionId);
		List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findByProjectIdAndRevisionIdOrderByPoIdDesc(projectId, revisionId);
		List<String> poIds = purchaseOrders.stream().map(PurchaseOrder::getPoId).toList();
		Map<String, List<ProgressClaim>> claimsByPo = poIds.isEmpty() ? Map.of() : progressClaimRepository.findByPoIdIn(poIds).stream()
				.collect(java.util.stream.Collectors.groupingBy(ProgressClaim::getPoId));
		List<String> claimIds = claimsByPo.values().stream().flatMap(List::stream).map(ProgressClaim::getClaimId).toList();
		Map<String, List<ActualPayment>> paymentsByClaim = claimIds.isEmpty() ? Map.of() : actualPaymentRepository.findByClaimIdIn(claimIds).stream()
				.collect(java.util.stream.Collectors.groupingBy(ActualPayment::getClaimId));
		return new ProjectRevisionHistoryResponse(
				revision.getRevisionNumber(),
				vendorComparisonRepository.findByProjectIdAndRevisionIdOrderByExecutionItemIdAscComparisonIdAsc(projectId, revisionId).stream()
						.map(comparison -> new ProjectRevisionHistoryResponse.Comparison(comparison.getExecutionItemId(), comparison.getVendorName(), comparison.getQuotedAmount(), comparison.isSelected(), comparison.getSelectionReason(), comparison.getConstructionPeriod()))
						.toList(),
				purchaseOrders.stream().map(po -> new ProjectRevisionHistoryResponse.PurchaseOrder(
						po.getPoId(), po.getBudgetId(), po.getVendorName(), po.getInitialPoAmount(), po.getApprovedBudgetAmount(),
						claimsByPo.getOrDefault(po.getPoId(), List.of()).stream().map(claim -> new ProjectRevisionHistoryResponse.Claim(
								claim.getClaimId(), claim.getDegree(), claim.getClaimAmount(), claim.getApprovalStatus().name(),
								paymentsByClaim.getOrDefault(claim.getClaimId(), List.of()).stream().map(payment -> new ProjectRevisionHistoryResponse.Payment(payment.getPaymentId(), payment.getPaidAmount(), payment.getPaidDate(), payment.getPaymentStatus())).toList()
						)).toList()
				)).toList()
		);
	}

	@Transactional
	public ProjectRevisionResponse saveContract(String projectId, String revisionId, CustomerContractRequest request) {
		ProjectRevision revision = revision(projectId, revisionId);
		ensureContractEditable(revision);
		List<ContractItem> items = request.items().stream()
				.filter(item -> StringUtils.hasText(item.workCategory()))
				.map(item -> new ContractItem(item.workCategory(), zero(item.quotedAmount()), zero(item.contractAmount()), item.vatAmount() == null ? zero(item.contractAmount()).multiply(BigDecimal.valueOf(0.1)) : item.vatAmount(), item.note()))
				.toList();
		revision.updateContractSnapshot(json(new ContractSnapshot(zero(request.targetProfitRate()), items)));
		return response(revisionRepository.save(revision));
	}

	@Transactional
	public ProjectRevisionResponse saveBudget(String projectId, String revisionId, ExecutionBudgetDetailRequest request) {
		ProjectRevision revision = revision(projectId, revisionId);
		ensureContractWritten(revision);
		List<BudgetDetail> details = request.details().stream()
				.filter(detail -> StringUtils.hasText(detail.budgetItemId()) && StringUtils.hasText(detail.categoryName()))
				.map(detail -> new BudgetDetail(
						detail.budgetItemId(), detail.categoryName(), empty(detail.itemName()), empty(detail.costType()),
						empty(detail.vendorDescription()), empty(detail.unit()), zero(detail.quantity()), zero(detail.unitPrice()),
						empty(detail.evidenceLink()), empty(detail.note())
				))
				.toList();
		revision.updateBudgetSnapshot(json(new BudgetSnapshot(details)));
		return response(revisionRepository.save(revision));
	}

	@Transactional
	public ApprovalResponse requestApproval(String projectId, String revisionId, ApprovalRequest request) {
		ProjectRevision revision = revision(projectId, revisionId);
		if (!"CONTRACT_WRITTEN".equals(revision.getStatus()) && !"BUDGET_WRITTEN".equals(revision.getStatus())) {
			throw new IllegalStateException("계약 변경 내용을 저장한 차수만 결재 요청할 수 있습니다.");
		}
		if (approvalRepository.existsByProjectIdAndApprovalTypeAndTargetIdAndStatus(projectId, ApprovalService.TYPE_PROJECT_REVISION, revisionId, ApprovalStatus.REQUESTED)) {
			throw new IllegalStateException("이미 결재 대기 중인 변경 차수입니다.");
		}
		ProjectRevisionResponse detail = response(revision);
		Approval approval = approvalRepository.save(new Approval(
				"APR-REV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
				projectId,
				ApprovalService.TYPE_PROJECT_REVISION,
				revisionId,
				revision.getRevisionNumber() + "차 계약 변경 승인 요청",
				detail.contractTotal(),
				detail.baseContractTotal(),
				detail.contractTotal().subtract(detail.baseContractTotal()),
				request != null && StringUtils.hasText(request.requestUser()) ? request.requestUser() : "system",
				request != null && StringUtils.hasText(request.requestReason()) ? request.requestReason() : revision.getReason()
		));
		revision.markApprovalRequested();
		revisionRepository.save(revision);
		return ApprovalResponse.from(approval, project(projectId));
	}

	@Transactional
	public void approveRevision(String projectId, String revisionId, String approvalId) {
		ProjectRevision revision = revision(projectId, revisionId);
		if (!"REQUESTED".equals(revision.getStatus())) throw new IllegalStateException("결재 대기 중인 변경 차수만 승인할 수 있습니다.");
		boolean contractOnlyRevision = budget(revision.getBaseBudgetSnapshot()).details().equals(budget(revision.getBudgetSnapshot()).details());
		Project project = project(projectId);
		ProjectRevisionResponse changed = response(revision);
		project.updateTargetProfitRate(changed.targetProfitRate());
		projectRepository.save(project);
		BigDecimal contractDelta = changed.contractTotal().subtract(currentContractTotal(projectId));
		if (contractDelta.signum() != 0) {
			contractChangeRepository.save(new ContractChange(
					"rev-contract-" + UUID.randomUUID().toString().replace("-", ""), projectId, revisionId,
					contractDelta, revision.getReason(), ApprovalStatus.APPROVED));
		}

		BudgetSnapshot baseBudget = budget(revision.getBaseBudgetSnapshot());
		BudgetSnapshot changedBudget = budget(revision.getBudgetSnapshot());
		Map<String, BigDecimal> baseAmounts = budgetAmounts(baseBudget.details());
		Map<String, BigDecimal> changedAmounts = budgetAmounts(changedBudget.details());
		Map<String, BudgetDetail> changedDetails = firstBudgetDetailByItem(changedBudget.details());
		Map<String, ProjectExecutionBudgetItem> existingItems = new LinkedHashMap<>();
		for (ProjectExecutionBudgetItem item : budgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId)) {
			existingItems.put(ProjectExecutionBudgetItem.budgetItemId(projectId, item.getItemId()), item);
		}
		Set<String> itemIds = new LinkedHashSet<>();
		itemIds.addAll(baseAmounts.keySet());
		itemIds.addAll(changedAmounts.keySet());
		for (String budgetItemId : itemIds) {
			BigDecimal baseAmount = baseAmounts.getOrDefault(budgetItemId, BigDecimal.ZERO);
			BigDecimal targetAmount = changedAmounts.getOrDefault(budgetItemId, BigDecimal.ZERO);
			if (targetAmount.compareTo(baseAmount) == 0) continue;
			ProjectExecutionBudgetItem item = existingItems.get(budgetItemId);
			BudgetDetail detail = changedDetails.get(budgetItemId);
			if (item == null) {
				if (detail == null || targetAmount.signum() <= 0) continue;
				item = new ProjectExecutionBudgetItem(ProjectExecutionBudgetItem.storageId(projectId, budgetItemId), projectId, detail.categoryName(), detail.itemName());
				item.updateBudgetAmount(BigDecimal.ZERO);
				item.approve(approvalId);
				item = budgetItemRepository.save(item);
				existingItems.put(budgetItemId, item);
			} else if (detail != null) {
				item.updateInfo(detail.categoryName(), detail.itemName());
				budgetItemRepository.save(item);
			}
			BigDecimal delta = targetAmount.subtract(currentBudgetAmount(item));
			if (delta.signum() != 0) {
				budgetChangeRepository.save(new BudgetChange(
						"rev-budget-" + UUID.randomUUID().toString().replace("-", ""), item.getItemId(), revisionId, delta, ApprovalStatus.APPROVED));
			}
		}
		if (contractOnlyRevision) {
			revision.enableExecutionBudgetRewrite();
		}
		revision.approve();
		revisionRepository.save(revision);
	}

	@Transactional(readOnly = true)
	public boolean isExecutionBudgetRewriteAvailable(String projectId) {
		return revisionRepository.findByProjectIdOrderByRevisionNumberDesc(projectId).stream()
				.findFirst()
				.map(this::isExecutionBudgetRewriteCandidate)
				.orElse(false);
	}

	@Transactional
	public void updateApprovedRevisionBudgetSnapshot(String projectId, List<ProjectExecutionBudgetDetail> details) {
		ProjectRevision revision = revisionRepository.findByProjectIdOrderByRevisionNumberDesc(projectId).stream()
				.filter(this::isExecutionBudgetRewriteCandidate)
				.findFirst()
				.orElse(null);
		if (revision == null) {
			return;
		}
		List<BudgetDetail> snapshotDetails = details.stream()
				.map(detail -> new BudgetDetail(
						detail.getBudgetItemId(), detail.getCategoryName(), detail.getItemName(), detail.getCostType(),
						empty(detail.getVendorDescription()), empty(detail.getUnit()), zero(detail.getQuantity()), zero(detail.getUnitPrice()),
						empty(detail.getEvidenceLink()), empty(detail.getNote())
				))
				.toList();
		revision.enableExecutionBudgetRewrite();
		revision.replaceBudgetSnapshot(json(new BudgetSnapshot(snapshotDetails)));
		revisionRepository.save(revision);
	}

	@Transactional
	public void consumeExecutionBudgetRewrite(String projectId) {
		revisionRepository.findByProjectIdOrderByRevisionNumberDesc(projectId).stream()
				.filter(this::isExecutionBudgetRewriteCandidate)
				.findFirst()
				.ifPresent(revision -> {
					revision.consumeExecutionBudgetRewrite();
					revisionRepository.save(revision);
				});
	}

	@Transactional
	public void rejectRevision(String projectId, String revisionId) {
		ProjectRevision revision = revision(projectId, revisionId);
		if (!"REQUESTED".equals(revision.getStatus())) throw new IllegalStateException("결재 대기 중인 변경 차수만 반려할 수 있습니다.");
		revision.reject();
		revisionRepository.save(revision);
	}

	@Transactional(readOnly = true)
	public ProjectRevisionProcurementResponse procurementItemIds(String projectId) {
		project(projectId);
		return revisionRepository.findByProjectIdOrderByRevisionNumberDesc(projectId).stream()
				.filter(revision -> "APPROVED".equals(revision.getStatus()))
				.findFirst()
				.map(revision -> new ProjectRevisionProcurementResponse(true, revision.getRevisionId(), budget(revision.getBudgetSnapshot()).details().stream()
						.map(BudgetDetail::budgetItemId).distinct().toList()))
				.orElse(new ProjectRevisionProcurementResponse(false, null, List.of()));
	}

	@Transactional(readOnly = true)
	public Optional<ProjectRevisionBudgetView> latestApprovedBudget(String projectId) {
		project(projectId);
		return revisionRepository.findByProjectIdOrderByRevisionNumberDesc(projectId).stream()
				.filter(revision -> "APPROVED".equals(revision.getStatus()))
				.findFirst()
				.map(revision -> {
					List<BudgetDetail> details = budget(revision.getBudgetSnapshot()).details();
					List<Detail> responseDetails = new java.util.ArrayList<>();
					for (int index = 0; index < details.size(); index++) {
						BudgetDetail detail = details.get(index);
						responseDetails.add(new Detail(
								"revision-" + revision.getRevisionId() + "-" + index,
								detail.budgetItemId(), detail.categoryName(), detail.itemName(), detail.costType(),
								detail.vendorDescription(), detail.unit(), zero(detail.quantity()), zero(detail.unitPrice()),
								zero(detail.quantity()).multiply(zero(detail.unitPrice())), detail.evidenceLink(), detail.note()
						));
					}
					return new ProjectRevisionBudgetView(revision.getRevisionNumber(), responseDetails);
				});
	}

	@Transactional(readOnly = true)
	public Optional<BigDecimal> latestApprovedBudgetItemAmount(String projectId, String budgetItemId) {
		return latestApprovedBudget(projectId).flatMap(view -> {
			List<Detail> matchingDetails = view.details().stream()
					.filter(detail -> budgetItemId.equals(detail.budgetItemId()))
					.toList();
			if (matchingDetails.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(matchingDetails.stream()
					.map(Detail::amount)
					.map(ProjectRevisionService::zero)
					.reduce(BigDecimal.ZERO, BigDecimal::add));
		});
	}

	@Transactional(readOnly = true)
	public Optional<ProjectRevisionResponse> latestApprovedRevision(String projectId) {
		project(projectId);
		return revisionRepository.findTopByProjectIdAndStatusOrderByRevisionNumberDesc(projectId, "APPROVED")
				.map(this::response);
	}

	private ProjectRevisionResponse response(ProjectRevision revision) {
		ContractSnapshot baseContract = contract(revision.getBaseContractSnapshot());
		ContractSnapshot changedContract = contract(revision.getContractSnapshot());
		BudgetSnapshot baseBudget = budget(revision.getBaseBudgetSnapshot());
		BudgetSnapshot changedBudget = budget(revision.getBudgetSnapshot());
		return new ProjectRevisionResponse(
				revision.getRevisionId(), revision.getProjectId(), revision.getRevisionNumber(), revision.getTitle(), revision.getReason(), revision.getStatus(), revision.getCreatedAt(),
				sumContract(baseContract.items()), sumContract(changedContract.items()), changedContract.targetProfitRate(), sumBudget(baseBudget.details()), sumBudget(changedBudget.details()),
				baseContract.items(), changedContract.items(), baseBudget.details(), changedBudget.details()
		);
	}

	private ContractSnapshot currentContract(Project project) {
		List<ContractItem> items = contractItemRepository.findByProjectIdOrderByWorkCategoryAsc(project.getProjectId()).stream()
				.map(item -> new ContractItem(item.getWorkCategory(), zero(item.getQuotedAmount()), zero(item.getContractAmount()), zero(item.getVatAmount()), item.getNote()))
				.toList();
		return new ContractSnapshot(zero(project.getTargetProfitRate()), items);
	}

	private BudgetSnapshot currentBudget(String projectId) {
		List<BudgetDetail> details = budgetDetailRepository.findByProjectIdOrderByBudgetItemIdAscDetailIdAsc(projectId).stream()
				.map(detail -> new BudgetDetail(detail.getBudgetItemId(), detail.getCategoryName(), detail.getItemName(), detail.getCostType(), detail.getVendorDescription(), detail.getUnit(), zero(detail.getQuantity()), zero(detail.getUnitPrice()), detail.getEvidenceLink(), detail.getNote()))
				.toList();
		return new BudgetSnapshot(details);
	}

	private Project project(String projectId) {
		return projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));
	}

	private ProjectRevision revision(String projectId, String revisionId) {
		ProjectRevision revision = revisionRepository.findById(revisionId).orElseThrow(() -> new EntityNotFoundException("Project revision not found: " + revisionId));
		if (!projectId.equals(revision.getProjectId())) throw new IllegalArgumentException("The revision belongs to another project.");
		return revision;
	}

	private static void ensureContractEditable(ProjectRevision revision) {
		if (!"DRAFT".equals(revision.getStatus()) && !"CONTRACT_WRITTEN".equals(revision.getStatus()) && !"BUDGET_WRITTEN".equals(revision.getStatus())) {
			throw new IllegalStateException("Only a draft revision can be edited.");
		}
	}

	private static void ensureContractWritten(ProjectRevision revision) {
		if (!"CONTRACT_WRITTEN".equals(revision.getStatus()) && !"BUDGET_WRITTEN".equals(revision.getStatus())) {
			throw new IllegalStateException("Save the contract revision before writing the execution budget.");
		}
	}

	private ContractSnapshot contract(String value) {
		try {
			return objectMapper.readValue(value, ContractSnapshot.class);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Unable to read the contract revision snapshot.", ex);
		}
	}

	private BudgetSnapshot budget(String value) {
		try {
			return objectMapper.readValue(value, BudgetSnapshot.class);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Unable to read the execution-budget revision snapshot.", ex);
		}
	}

	private String json(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Unable to save the revision snapshot.", ex);
		}
	}

	private static BigDecimal sumContract(List<ContractItem> items) {
		return items.stream().map(ContractItem::contractAmount).map(ProjectRevisionService::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static BigDecimal sumBudget(List<BudgetDetail> details) {
		return details.stream().map(detail -> zero(detail.quantity()).multiply(zero(detail.unitPrice()))).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal currentContractTotal(String projectId) {
		BigDecimal original = contractItemRepository.findByProjectIdOrderByWorkCategoryAsc(projectId).stream()
				.map(CustomerContractItem::getContractAmount).map(ProjectRevisionService::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal changes = contractChangeRepository.findByProjectIdAndApprovalStatus(projectId, ApprovalStatus.APPROVED).stream()
				.map(ContractChange::getChangeAmount).map(ProjectRevisionService::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
		return original.add(changes);
	}

	private BigDecimal currentBudgetAmount(ProjectExecutionBudgetItem item) {
		BigDecimal original = item.getApprovedBudgetAmount() == null ? zero(item.getBudgetAmount()) : zero(item.getApprovedBudgetAmount());
		BigDecimal changes = budgetChangeRepository.findByBudgetIdInAndApprovalStatus(List.of(item.getItemId()), ApprovalStatus.APPROVED).stream()
				.map(BudgetChange::getChangeAmount).map(ProjectRevisionService::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
		return original.add(changes);
	}

	private static Map<String, BigDecimal> budgetAmounts(List<BudgetDetail> details) {
		Map<String, BigDecimal> amounts = new LinkedHashMap<>();
		for (BudgetDetail detail : details) {
			amounts.merge(detail.budgetItemId(), zero(detail.quantity()).multiply(zero(detail.unitPrice())), BigDecimal::add);
		}
		return amounts;
	}

	private static Map<String, BudgetDetail> firstBudgetDetailByItem(List<BudgetDetail> details) {
		Map<String, BudgetDetail> items = new LinkedHashMap<>();
		for (BudgetDetail detail : details) items.putIfAbsent(detail.budgetItemId(), detail);
		return items;
	}

	private Set<String> changedProcurementItemIds(ProjectRevision revision) {
		Map<String, BigDecimal> base = budgetAmounts(budget(revision.getBaseBudgetSnapshot()).details());
		Map<String, BigDecimal> changed = budgetAmounts(budget(revision.getBudgetSnapshot()).details());
		Set<String> result = new LinkedHashSet<>();
		Set<String> itemIds = new LinkedHashSet<>();
		itemIds.addAll(base.keySet());
		itemIds.addAll(changed.keySet());
		for (String itemId : itemIds) {
			if (changed.getOrDefault(itemId, BigDecimal.ZERO).compareTo(base.getOrDefault(itemId, BigDecimal.ZERO)) != 0
					&& changed.getOrDefault(itemId, BigDecimal.ZERO).signum() > 0) result.add(itemId);
		}
		return result;
	}

	private static BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
	private static String empty(String value) { return value == null ? "" : value; }

	private boolean isExecutionBudgetRewriteCandidate(ProjectRevision revision) {
		return "APPROVED".equals(revision.getStatus())
				&& (revision.isExecutionBudgetRewriteAvailable() || budget(revision.getBudgetSnapshot()).details().isEmpty());
	}

	private record ContractSnapshot(BigDecimal targetProfitRate, List<ContractItem> items) { }
	private record BudgetSnapshot(List<BudgetDetail> details) { }
}
