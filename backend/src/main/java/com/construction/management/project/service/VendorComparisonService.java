package com.construction.management.project.service;

import com.construction.management.project.api.PurchaseOrderDraftResponse;
import com.construction.management.project.api.VendorComparisonRequest;
import com.construction.management.project.api.VendorComparisonResponse;
import com.construction.management.project.domain.PoRequisition;
import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.BudgetChange;
import com.construction.management.project.domain.Project;
import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import com.construction.management.project.domain.ProjectExecutionBudgetDetail;
import com.construction.management.project.domain.ProjectRevision;
import com.construction.management.project.domain.PurchaseOrder;
import com.construction.management.project.domain.VendorComparison;
import com.construction.management.project.repository.PoRequisitionRepository;
import com.construction.management.project.repository.BudgetChangeRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetItemRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetDetailRepository;
import com.construction.management.project.repository.ProjectRepository;
import com.construction.management.project.repository.ProjectRevisionRepository;
import com.construction.management.project.repository.PurchaseOrderRepository;
import com.construction.management.project.repository.VendorComparisonRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROJECT_READ')")
public class VendorComparisonService {
	@org.springframework.beans.factory.annotation.Autowired private com.construction.management.auth.BusinessPolicy businessPolicy;

	private final ProjectRepository projectRepository;
	private final ProjectRevisionRepository revisionRepository;
	private final ProjectExecutionBudgetItemRepository budgetItemRepository;
	private final ProjectExecutionBudgetDetailRepository budgetDetailRepository;
	private final VendorComparisonRepository comparisonRepository;
	private final PoRequisitionRepository requisitionRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final ProjectSettlementService projectSettlementService;
	private final ApprovalService approvalService;
	private final BudgetChangeRepository budgetChangeRepository;
	private final VendorComparisonAttachmentService attachmentService;
	private final ProjectRevisionService projectRevisionService;

	public VendorComparisonService(ProjectRepository projectRepository, ProjectRevisionRepository revisionRepository, ProjectExecutionBudgetItemRepository budgetItemRepository, ProjectExecutionBudgetDetailRepository budgetDetailRepository, VendorComparisonRepository comparisonRepository, PoRequisitionRepository requisitionRepository, PurchaseOrderRepository purchaseOrderRepository, ProjectSettlementService projectSettlementService, ApprovalService approvalService, BudgetChangeRepository budgetChangeRepository, VendorComparisonAttachmentService attachmentService, ProjectRevisionService projectRevisionService) {
		this.projectRepository = projectRepository;
		this.revisionRepository = revisionRepository;
		this.budgetItemRepository = budgetItemRepository;
		this.budgetDetailRepository = budgetDetailRepository;
		this.comparisonRepository = comparisonRepository;
		this.requisitionRepository = requisitionRepository;
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.projectSettlementService = projectSettlementService;
		this.approvalService = approvalService;
		this.budgetChangeRepository = budgetChangeRepository;
		this.attachmentService = attachmentService;
		this.projectRevisionService = projectRevisionService;
	}

	@Transactional(readOnly = true)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROCUREMENT_READ')")
	public VendorComparisonResponse getComparisons(String projectId, String itemId) {
		Project project = getProject(projectId);
		ProjectExecutionBudgetItem item = getItem(projectId, itemId);
		return toResponse(project, item);
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROCUREMENT_WRITE')")
	@com.construction.management.auth.BusinessAction
	public VendorComparisonResponse saveComparisons(String projectId, String itemId, VendorComparisonRequest request) {
		businessPolicy.draft("PROCUREMENT", projectId, itemId);
		projectSettlementService.assertNotSettled(projectId);
		Project project = getProject(projectId);
		ProjectExecutionBudgetItem item = getItem(projectId, itemId);
		String revisionId = currentProcurementRevisionId(projectId);
		requisitionRepository.findTopByProjectIdAndExecutionItemIdAndRevisionIdOrderByCreatedAtDesc(projectId, itemId, revisionId)
				.filter(requisition -> ApprovalStatus.REQUESTED.name().equals(requisition.getApprovalStatus()) || ApprovalStatus.APPROVED.name().equals(requisition.getApprovalStatus()))
				.ifPresent(requisition -> {
					throw new IllegalStateException("결재 대기 또는 승인 완료 발주품의는 수정할 수 없습니다.");
				});
		if (!item.isApproved()) {
			throw new IllegalStateException("실행예산 승인 후 발주품의를 요청할 수 있습니다.");
		}
		assertProcurementRequired(projectId, itemId);
		long selectedCount = request.comparisons().stream().filter(VendorComparisonRequest.Item::selected).count();
		if (selectedCount > 1) {
			throw new IllegalArgumentException("동일 실행항목ID에는 하나의 업체만 선정할 수 있습니다.");
		}
		request.comparisons().stream()
				.filter(VendorComparisonRequest.Item::selected)
				.filter(comparison -> !StringUtils.hasText(comparison.selectionReason()))
				.findAny()
				.ifPresent(comparison -> {
					throw new IllegalArgumentException("선정 업체는 선정 사유가 필요합니다.");
				});

		comparisonRepository.deleteByProjectIdAndExecutionItemIdAndRevisionId(projectId, itemId, revisionId);
		List<VendorComparison> comparisons = request.comparisons().stream()
				.filter(comparison -> StringUtils.hasText(comparison.vendorName()))
				.map(comparison -> new VendorComparison(
						StringUtils.hasText(comparison.comparisonCode()) ? comparison.comparisonCode() : nextComparisonCode(itemId),
						projectId,
						itemId,
						revisionId,
						comparison.vendorName(),
						comparison.quotedAmount(),
						comparison.vatType(),
						comparison.constructionPeriod(),
						comparison.scopeAndNotes(),
						comparison.paymentTerms(),
						comparison.estimateFileUrl(),
						comparison.selected(),
						comparison.selectionReason()
				))
				.toList();
		comparisonRepository.saveAll(comparisons);
		return toResponse(project, item);
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROCUREMENT_WRITE')")
	@com.construction.management.auth.BusinessAction
	public com.construction.management.project.api.ApprovalResponse.Attachment uploadAttachment(
			String projectId, String itemId, String comparisonId, org.springframework.web.multipart.MultipartFile file
	) {
		businessPolicy.draft("PROCUREMENT", projectId, itemId);
		projectSettlementService.assertNotSettled(projectId);
		getItem(projectId, itemId);
		String revisionId = currentProcurementRevisionId(projectId);
		requisitionRepository.findTopByProjectIdAndExecutionItemIdAndRevisionIdOrderByCreatedAtDesc(projectId, itemId, revisionId)
				.filter(requisition -> ApprovalStatus.REQUESTED.name().equals(requisition.getApprovalStatus()) || ApprovalStatus.APPROVED.name().equals(requisition.getApprovalStatus()))
				.ifPresent(requisition -> { throw new IllegalStateException("결재 대기 또는 승인된 발주품의의 비교견적에는 파일을 추가할 수 없습니다."); });
		VendorComparison comparison = comparisonRepository.findById(comparisonId)
				.orElseThrow(() -> new EntityNotFoundException("비교견적을 먼저 저장한 후 파일을 올려 주세요."));
		if (!projectId.equals(comparison.getProjectId()) || !itemId.equals(comparison.getExecutionItemId()) || !java.util.Objects.equals(revisionId, comparison.getRevisionId())) {
			throw new EntityNotFoundException("현재 실행항목에 속하지 않는 비교견적입니다.");
		}
		return attachmentService.save(projectId, itemId, comparisonId, file);
	}

	@Transactional(readOnly = true)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROCUREMENT_READ')")
	public PurchaseOrderDraftResponse getPurchaseOrderDraft(String projectId, String itemId) {
		Project project = getProject(projectId);
		ProjectExecutionBudgetItem item = getItem(projectId, itemId);
		String revisionId = currentProcurementRevisionId(projectId);
		VendorComparison selected = comparisonRepository.findByProjectIdAndExecutionItemIdAndRevisionIdAndSelectedTrue(projectId, itemId, revisionId)
				.orElseThrow(() -> new EntityNotFoundException("선정된 비교견적이 없습니다."));
		PoRequisition requisition = requisitionRepository.findTopByProjectIdAndExecutionItemIdAndRevisionIdOrderByCreatedAtDesc(projectId, itemId, revisionId).orElse(null);
		return toDraft(project, item, selected, requisition);
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROCUREMENT_REQUEST')")
	@com.construction.management.auth.BusinessAction
	public PurchaseOrderDraftResponse requestPurchaseOrder(String projectId, String itemId) {
		businessPolicy.draft("PROCUREMENT", projectId, itemId);
		projectSettlementService.assertNotSettled(projectId);
		Project project = getProject(projectId);
		ProjectExecutionBudgetItem item = getItem(projectId, itemId);
		String revisionId = currentProcurementRevisionId(projectId);
		if (!item.isApproved()) {
			throw new IllegalStateException("실행예산 승인 후 발주품의를 요청할 수 있습니다.");
		}
		assertProcurementRequired(projectId, itemId);
		VendorComparison selected = comparisonRepository.findByProjectIdAndExecutionItemIdAndRevisionIdAndSelectedTrue(projectId, itemId, revisionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "선정 업체를 저장한 후 발주품의를 요청할 수 있습니다."));
		if (!StringUtils.hasText(selected.getSelectionReason())) {
			throw new IllegalArgumentException("발주품의 요청 전 선정 사유가 필요합니다.");
		}
		if (!requisitionRepository.findByProjectIdAndExecutionItemIdAndRevisionIdAndApprovalStatus(projectId, itemId, revisionId, "REQUESTED").isEmpty()) {
			throw new IllegalArgumentException("이미 결재 대기 중인 발주품의가 있습니다.");
		}
		if (!requisitionRepository.findByProjectIdAndExecutionItemIdAndRevisionIdAndApprovalStatus(projectId, itemId, revisionId, "APPROVED").isEmpty()) {
			throw new IllegalArgumentException("이미 승인된 발주품의가 있습니다.");
		}
		BigDecimal approvedBudget = currentBudgetAmount(projectId, item);
		BigDecimal variance = selected.getQuotedAmount().subtract(approvedBudget);
		PoRequisition requisition = new PoRequisition(
				"REQ-PO-" + UUID.randomUUID().toString().substring(0, 8),
				projectId,
				itemId,
				revisionId,
				selected.getComparisonId(),
				approvedBudget,
				selected.getQuotedAmount(),
				variance,
				selected.getSelectionReason(),
				com.construction.management.auth.CurrentActor.name()
		);
		requisitionRepository.save(requisition);
		approvalService.createPurchaseOrderApproval(project, item, requisition, selected.getSelectionReason());
		return toDraft(project, item, selected, requisition);
	}

	private VendorComparisonResponse toResponse(Project project, ProjectExecutionBudgetItem item) {
		String budgetItemId = ProjectExecutionBudgetItem.budgetItemId(project.getProjectId(), item.getItemId());
		BigDecimal budgetAmount = currentBudgetAmount(project.getProjectId(), item);
		return new VendorComparisonResponse(
				project.getProjectId(),
				project.getProjectCode(),
				budgetItemId,
				item.getCategoryName(),
				item.getItemName(),
				budgetAmount,
				comparisonRepository.findByProjectIdAndExecutionItemIdAndRevisionIdOrderByComparisonIdAsc(
						project.getProjectId(), budgetItemId, currentProcurementRevisionId(project.getProjectId())
				).stream()
						.map(comparison -> new VendorComparisonResponse.Item(
								comparison.getComparisonId(),
								comparison.getComparisonId(),
								comparison.getVendorName(),
								comparison.getQuotedAmount(),
								comparison.getVatType(),
								comparison.getConstructionPeriod(),
								comparison.getScopeAndNotes(),
							comparison.getPaymentTerms(),
							comparison.getEstimateFileUrl(),
							attachmentService.getResponses(project.getProjectId(), budgetItemId, comparison.getComparisonId()),
							comparison.isSelected(),
								comparison.getSelectionReason(),
								comparison.getQuotedAmount().subtract(budgetAmount)
						))
						.toList()
		);
	}

	private PurchaseOrderDraftResponse toDraft(Project project, ProjectExecutionBudgetItem item, VendorComparison selected, PoRequisition requisition) {
		String budgetItemId = ProjectExecutionBudgetItem.budgetItemId(project.getProjectId(), item.getItemId());
		BigDecimal budgetAmount = currentBudgetAmount(project.getProjectId(), item);
		BigDecimal variance = selected.getQuotedAmount().subtract(budgetAmount);
		String varianceType = variance.signum() > 0 ? "OVER" : variance.signum() < 0 ? "SAVING" : "EVEN";
		String color = variance.signum() > 0 ? "RED" : variance.signum() < 0 ? "BLUE" : "DARK";
		PurchaseOrder purchaseOrder = requisition == null ? null : purchaseOrderRepository.findByRequisitionId(requisition.getRequisitionId()).orElse(null);
		return new PurchaseOrderDraftResponse(
				project.getProjectCode(), budgetItemId, item.getCategoryName(), item.getItemName(), budgetAmount,
				selected.getVendorName(), selected.getQuotedAmount(), variance, varianceType, color, selected.getSelectionReason(),
				requisition == null ? "DRAFT" : requisition.getApprovalStatus(),
				purchaseOrder == null ? null : purchaseOrder.getPoId(),
				purchaseOrder == null ? null : purchaseOrder.getApprovedAt(),
				purchaseOrder == null ? null : purchaseOrder.getApprover()
		);
	}

	private Project getProject(String projectId) {
		return projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
	}

	private ProjectExecutionBudgetItem getItem(String projectId, String itemId) {
		return budgetItemRepository.findByProjectIdAndBudgetItemId(projectId, itemId)
				.orElseThrow(() -> new EntityNotFoundException("Execution budget item not found: " + itemId));
	}

	private ProjectExecutionBudgetItem getItem(String itemId) {
		return budgetItemRepository.findById(itemId).orElseThrow(() -> new EntityNotFoundException("실행항목을 찾을 수 없습니다: " + itemId));
	}

	private String nextComparisonCode(String itemId) {
		return "CMP-" + itemId.replace("EX-", "") + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
	}

	private String currentProcurementRevisionId(String projectId) {
		return revisionRepository.findTopByProjectIdAndStatusOrderByRevisionNumberDesc(projectId, "APPROVED")
				.map(ProjectRevision::getRevisionId)
				.orElse(null);
	}

	private BigDecimal currentBudgetAmount(String projectId, ProjectExecutionBudgetItem item) {
		String budgetItemId = ProjectExecutionBudgetItem.budgetItemId(projectId, item.getItemId());
		var revisionAmount = projectRevisionService.latestApprovedBudgetItemAmount(projectId, budgetItemId);
		if (revisionAmount.isPresent()) {
			return revisionAmount.get();
		}
		BigDecimal original = item.getApprovedBudgetAmount() == null ? item.getBudgetAmount() : item.getApprovedBudgetAmount();
		BigDecimal changes = budgetChangeRepository.findByBudgetIdInAndApprovalStatus(List.of(item.getItemId()), ApprovalStatus.APPROVED).stream()
				.map(BudgetChange::getChangeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		return original.add(changes);
	}

	private void assertProcurementRequired(String projectId, String itemId) {
		List<ProjectExecutionBudgetDetail> details = budgetDetailRepository.findByProjectIdAndBudgetItemIdOrderByDetailIdAsc(projectId, itemId);
		boolean directSettlementOnly = !details.isEmpty() && details.stream().allMatch(detail -> ProjectExecutionBudgetDetail.isDirectSettlementCostType(detail.getCostType()));
		if (directSettlementOnly) {
			throw new IllegalStateException("현장경비·인건비 실행항목은 업체 비교견적 및 발주 대상이 아닙니다. 준공정산의 직접 정산비에서 처리해 주세요.");
		}
	}
}
