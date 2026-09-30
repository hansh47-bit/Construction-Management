package com.construction.management.project.service;

import com.construction.management.project.api.PurchaseOrderDraftResponse;
import com.construction.management.project.api.VendorComparisonRequest;
import com.construction.management.project.api.VendorComparisonResponse;
import com.construction.management.project.domain.PoRequisition;
import com.construction.management.project.domain.Project;
import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import com.construction.management.project.domain.VendorComparison;
import com.construction.management.project.repository.PoRequisitionRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetItemRepository;
import com.construction.management.project.repository.ProjectRepository;
import com.construction.management.project.repository.VendorComparisonRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class VendorComparisonService {
	private final ProjectRepository projectRepository;
	private final ProjectExecutionBudgetItemRepository budgetItemRepository;
	private final VendorComparisonRepository comparisonRepository;
	private final PoRequisitionRepository requisitionRepository;
	private final ProjectSettlementService projectSettlementService;
	private final ApprovalService approvalService;

	public VendorComparisonService(ProjectRepository projectRepository, ProjectExecutionBudgetItemRepository budgetItemRepository, VendorComparisonRepository comparisonRepository, PoRequisitionRepository requisitionRepository, ProjectSettlementService projectSettlementService, ApprovalService approvalService) {
		this.projectRepository = projectRepository;
		this.budgetItemRepository = budgetItemRepository;
		this.comparisonRepository = comparisonRepository;
		this.requisitionRepository = requisitionRepository;
		this.projectSettlementService = projectSettlementService;
		this.approvalService = approvalService;
	}

	@Transactional(readOnly = true)
	public VendorComparisonResponse getComparisons(String projectId, String itemId) {
		Project project = getProject(projectId);
		ProjectExecutionBudgetItem item = getItem(itemId);
		return toResponse(project, item);
	}

	@Transactional
	public VendorComparisonResponse saveComparisons(String projectId, String itemId, VendorComparisonRequest request) {
		projectSettlementService.assertNotSettled(projectId);
		Project project = getProject(projectId);
		ProjectExecutionBudgetItem item = getItem(itemId);
		if (!item.isApproved()) {
			throw new IllegalStateException("실행예산 승인 후 발주품의를 요청할 수 있습니다.");
		}
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

		comparisonRepository.deleteByProjectIdAndExecutionItemId(projectId, itemId);
		List<VendorComparison> comparisons = request.comparisons().stream()
				.filter(comparison -> StringUtils.hasText(comparison.vendorName()))
				.map(comparison -> new VendorComparison(
						StringUtils.hasText(comparison.comparisonCode()) ? comparison.comparisonCode() : nextComparisonCode(itemId),
						projectId,
						itemId,
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

	@Transactional(readOnly = true)
	public PurchaseOrderDraftResponse getPurchaseOrderDraft(String projectId, String itemId) {
		Project project = getProject(projectId);
		ProjectExecutionBudgetItem item = getItem(itemId);
		VendorComparison selected = comparisonRepository.findByProjectIdAndExecutionItemIdAndSelectedTrue(projectId, itemId)
				.orElseThrow(() -> new EntityNotFoundException("선정된 비교견적이 없습니다."));
		return toDraft(project, item, selected, "DRAFT");
	}

	@Transactional
	public PurchaseOrderDraftResponse requestPurchaseOrder(String projectId, String itemId) {
		projectSettlementService.assertNotSettled(projectId);
		Project project = getProject(projectId);
		ProjectExecutionBudgetItem item = getItem(itemId);
		if (!item.isApproved()) {
			throw new IllegalStateException("실행예산 승인 후 발주품의를 요청할 수 있습니다.");
		}
		VendorComparison selected = comparisonRepository.findByProjectIdAndExecutionItemIdAndSelectedTrue(projectId, itemId)
				.orElseThrow(() -> new EntityNotFoundException("선정된 비교견적이 없습니다."));
		if (!StringUtils.hasText(selected.getSelectionReason())) {
			throw new IllegalArgumentException("발주품의 요청 전 선정 사유가 필요합니다.");
		}
		if (!requisitionRepository.findByProjectIdAndExecutionItemIdAndApprovalStatus(projectId, itemId, "REQUESTED").isEmpty()) {
			throw new IllegalArgumentException("이미 결재 대기 중인 발주품의가 있습니다.");
		}
		if (!requisitionRepository.findByProjectIdAndExecutionItemIdAndApprovalStatus(projectId, itemId, "APPROVED").isEmpty()) {
			throw new IllegalArgumentException("이미 승인된 발주품의가 있습니다.");
		}
		BigDecimal approvedBudget = item.getApprovedBudgetAmount() == null ? item.getBudgetAmount() : item.getApprovedBudgetAmount();
		BigDecimal variance = selected.getQuotedAmount().subtract(approvedBudget);
		PoRequisition requisition = new PoRequisition(
				"REQ-PO-" + UUID.randomUUID().toString().substring(0, 8),
				projectId,
				itemId,
				selected.getComparisonId(),
				approvedBudget,
				selected.getQuotedAmount(),
				variance,
				selected.getSelectionReason(),
				"system"
		);
		requisitionRepository.save(requisition);
		approvalService.createPurchaseOrderApproval(project, item, requisition, selected.getSelectionReason());
		return toDraft(project, item, selected, "REQUESTED");
	}

	private VendorComparisonResponse toResponse(Project project, ProjectExecutionBudgetItem item) {
		BigDecimal budgetAmount = item.getApprovedBudgetAmount() == null ? item.getBudgetAmount() : item.getApprovedBudgetAmount();
		return new VendorComparisonResponse(
				project.getProjectId(),
				project.getProjectCode(),
				item.getItemId(),
				item.getCategoryName(),
				item.getItemName(),
				budgetAmount,
				comparisonRepository.findByProjectIdAndExecutionItemIdOrderByComparisonIdAsc(project.getProjectId(), item.getItemId()).stream()
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
								comparison.isSelected(),
								comparison.getSelectionReason(),
								comparison.getQuotedAmount().subtract(budgetAmount)
						))
						.toList()
		);
	}

	private PurchaseOrderDraftResponse toDraft(Project project, ProjectExecutionBudgetItem item, VendorComparison selected, String status) {
		BigDecimal budgetAmount = item.getApprovedBudgetAmount() == null ? item.getBudgetAmount() : item.getApprovedBudgetAmount();
		BigDecimal variance = selected.getQuotedAmount().subtract(budgetAmount);
		String varianceType = variance.signum() > 0 ? "OVER" : variance.signum() < 0 ? "SAVING" : "EVEN";
		String color = variance.signum() > 0 ? "RED" : variance.signum() < 0 ? "BLUE" : "DARK";
		return new PurchaseOrderDraftResponse(project.getProjectCode(), item.getItemId(), item.getCategoryName(), item.getItemName(), budgetAmount, selected.getVendorName(), selected.getQuotedAmount(), variance, varianceType, color, selected.getSelectionReason(), status);
	}

	private Project getProject(String projectId) {
		return projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
	}

	private ProjectExecutionBudgetItem getItem(String itemId) {
		return budgetItemRepository.findById(itemId).orElseThrow(() -> new EntityNotFoundException("실행항목을 찾을 수 없습니다: " + itemId));
	}

	private String nextComparisonCode(String itemId) {
		return "CMP-" + itemId.replace("EX-", "") + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
	}
}
