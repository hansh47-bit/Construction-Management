package com.construction.management.project.service;

import com.construction.management.project.api.ApprovalDecisionRequest;
import com.construction.management.project.api.ApprovalRequest;
import com.construction.management.project.api.ApprovalResponse;
import com.construction.management.project.api.ExecutionBudgetDetailResponse;
import com.construction.management.project.api.VendorComparisonResponse;
import com.construction.management.project.domain.Approval;
import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.PoRequisition;
import com.construction.management.project.domain.Project;
import com.construction.management.project.domain.ProjectExecutionBudgetDetail;
import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import com.construction.management.project.domain.PurchaseOrder;
import com.construction.management.project.domain.VendorComparison;
import com.construction.management.project.repository.ApprovalRepository;
import com.construction.management.project.repository.PoRequisitionRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetDetailRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetItemRepository;
import com.construction.management.project.repository.ProjectRepository;
import com.construction.management.project.repository.PurchaseOrderRepository;
import com.construction.management.project.repository.VendorComparisonRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ApprovalService {
	public static final String TYPE_EXECUTION_BUDGET = "EXECUTION_BUDGET";
	public static final String TYPE_PURCHASE_ORDER = "PURCHASE_ORDER";

	private final ApprovalRepository approvalRepository;
	private final ProjectRepository projectRepository;
	private final ProjectExecutionBudgetItemRepository budgetItemRepository;
	private final ProjectExecutionBudgetDetailRepository budgetDetailRepository;
	private final PoRequisitionRepository requisitionRepository;
	private final VendorComparisonRepository comparisonRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final ExecutionBudgetDetailService executionBudgetDetailService;
	private final ProjectSettlementService projectSettlementService;

	public ApprovalService(
			ApprovalRepository approvalRepository,
			ProjectRepository projectRepository,
			ProjectExecutionBudgetItemRepository budgetItemRepository,
			ProjectExecutionBudgetDetailRepository budgetDetailRepository,
			PoRequisitionRepository requisitionRepository,
			VendorComparisonRepository comparisonRepository,
			PurchaseOrderRepository purchaseOrderRepository,
			ExecutionBudgetDetailService executionBudgetDetailService,
			ProjectSettlementService projectSettlementService
	) {
		this.approvalRepository = approvalRepository;
		this.projectRepository = projectRepository;
		this.budgetItemRepository = budgetItemRepository;
		this.budgetDetailRepository = budgetDetailRepository;
		this.requisitionRepository = requisitionRepository;
		this.comparisonRepository = comparisonRepository;
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.executionBudgetDetailService = executionBudgetDetailService;
		this.projectSettlementService = projectSettlementService;
	}

	@Transactional(readOnly = true)
	public List<ApprovalResponse> search(String status, String type, String projectId) {
		ApprovalStatus approvalStatus = parseStatus(status);
		List<Approval> approvals;
		if (approvalStatus != null && StringUtils.hasText(type) && StringUtils.hasText(projectId)) {
			approvals = approvalRepository.findByStatusAndApprovalTypeAndProjectIdOrderByRequestedAtDesc(approvalStatus, type, projectId);
		} else if (approvalStatus != null && StringUtils.hasText(type)) {
			approvals = approvalRepository.findByStatusAndApprovalTypeOrderByRequestedAtDesc(approvalStatus, type);
		} else if (approvalStatus != null && StringUtils.hasText(projectId)) {
			approvals = approvalRepository.findByStatusAndProjectIdOrderByRequestedAtDesc(approvalStatus, projectId);
		} else if (StringUtils.hasText(type) && StringUtils.hasText(projectId)) {
			approvals = approvalRepository.findByApprovalTypeAndProjectIdOrderByRequestedAtDesc(type, projectId);
		} else if (approvalStatus != null) {
			approvals = approvalRepository.findByStatusOrderByRequestedAtDesc(approvalStatus);
		} else if (StringUtils.hasText(type)) {
			approvals = approvalRepository.findByApprovalTypeOrderByRequestedAtDesc(type);
		} else if (StringUtils.hasText(projectId)) {
			approvals = approvalRepository.findByProjectIdOrderByRequestedAtDesc(projectId);
		} else {
			approvals = approvalRepository.findByOrderByRequestedAtDesc();
		}

		return approvals.stream()
				.map(approval -> ApprovalResponse.from(approval, getProject(approval.getProjectId())))
				.toList();
	}

	@Transactional(readOnly = true)
	public ApprovalResponse getApproval(String approvalId) {
		Approval approval = getApprovalEntity(approvalId);
		return ApprovalResponse.from(approval, getProject(approval.getProjectId()), detailFor(approval));
	}

	@Transactional
	public ApprovalResponse requestExecutionBudget(String projectId, ApprovalRequest request) {
		projectSettlementService.assertNotSettled(projectId);
		Project project = getProject(projectId);
		List<ProjectExecutionBudgetDetail> details = budgetDetailRepository.findByProjectIdOrderByBudgetItemIdAscDetailIdAsc(projectId);
		if (details.isEmpty()) {
			throw new IllegalArgumentException("세부 실행예산 입력 항목이 있어야 결재요청할 수 있습니다.");
		}
		BigDecimal total = details.stream()
				.map(ProjectExecutionBudgetDetail::getAmount)
				.map(ApprovalService::defaultZero)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		if (total.signum() <= 0) {
			throw new IllegalArgumentException("실행예산 총액이 0원보다 커야 결재요청할 수 있습니다.");
		}
		if (approvalRepository.existsByProjectIdAndApprovalTypeAndStatus(projectId, TYPE_EXECUTION_BUDGET, ApprovalStatus.REQUESTED)) {
			throw new IllegalArgumentException("이미 결재 대기 중인 실행예산 요청이 있습니다.");
		}
		if (approvalRepository.existsByProjectIdAndApprovalTypeAndStatus(projectId, TYPE_EXECUTION_BUDGET, ApprovalStatus.APPROVED)) {
			throw new IllegalArgumentException("이미 승인된 최초 실행예산이 있습니다. 변경은 변경관리에서 처리해야 합니다.");
		}

		Approval approval = new Approval(
				nextApprovalId(),
				projectId,
				TYPE_EXECUTION_BUDGET,
				projectId,
				"실행예산 승인 요청",
				total,
				BigDecimal.ZERO,
				total,
				requestUser(request),
				request == null ? null : request.requestReason()
		);
		approvalRepository.save(approval);
		budgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId).forEach(item -> {
			item.markApprovalRequested(approval.getApprovalId());
			budgetItemRepository.save(item);
		});
		return ApprovalResponse.from(approval, project, detailFor(approval));
	}

	@Transactional
	public Approval createPurchaseOrderApproval(Project project, ProjectExecutionBudgetItem item, PoRequisition requisition, String requestReason) {
		Approval approval = new Approval(
				nextApprovalId(),
				project.getProjectId(),
				TYPE_PURCHASE_ORDER,
				requisition.getRequisitionId(),
				"발주품의 승인 요청",
				requisition.getPoAmount(),
				requisition.getApprovedBudgetAmount(),
				requisition.getBudgetVariance(),
				"system",
				requestReason
		);
		approvalRepository.save(approval);
		requisition.linkApproval(approval.getApprovalId());
		requisitionRepository.save(requisition);
		return approval;
	}

	@Transactional
	public ApprovalResponse approve(String approvalId, ApprovalDecisionRequest request) {
		Approval approval = getRequestedApproval(approvalId);
		if (TYPE_EXECUTION_BUDGET.equals(approval.getApprovalType())) {
			approveExecutionBudget(approval);
		} else if (TYPE_PURCHASE_ORDER.equals(approval.getApprovalType())) {
			approvePurchaseOrder(approval, request);
		} else {
			throw new IllegalArgumentException("지원하지 않는 결재 유형입니다: " + approval.getApprovalType());
		}
		approval.approve(decisionUser(request), request == null ? null : request.comment());
		approvalRepository.save(approval);
		return ApprovalResponse.from(approval, getProject(approval.getProjectId()), detailFor(approval));
	}

	@Transactional
	public ApprovalResponse reject(String approvalId, ApprovalDecisionRequest request) {
		Approval approval = getRequestedApproval(approvalId);
		if (request == null || !StringUtils.hasText(request.comment())) {
			throw new IllegalArgumentException("반려 사유를 입력해야 합니다.");
		}
		if (TYPE_EXECUTION_BUDGET.equals(approval.getApprovalType())) {
			budgetItemRepository.findByProjectIdOrderByItemIdAsc(approval.getProjectId()).forEach(item -> {
				item.reject(approval.getApprovalId(), request.comment());
				budgetItemRepository.save(item);
			});
		} else if (TYPE_PURCHASE_ORDER.equals(approval.getApprovalType())) {
			PoRequisition requisition = getRequisition(approval);
			requisition.reject(decisionUser(request), request.comment());
			requisitionRepository.save(requisition);
		}
		approval.reject(decisionUser(request), request.comment());
		approvalRepository.save(approval);
		return ApprovalResponse.from(approval, getProject(approval.getProjectId()), detailFor(approval));
	}

	private void approveExecutionBudget(Approval approval) {
		List<ProjectExecutionBudgetItem> items = budgetItemRepository.findByProjectIdOrderByItemIdAsc(approval.getProjectId());
		if (items.isEmpty()) {
			throw new IllegalArgumentException("승인할 실행예산 항목이 없습니다.");
		}
		items.forEach(item -> {
			item.approve(approval.getApprovalId());
			budgetItemRepository.save(item);
		});
	}

	private void approvePurchaseOrder(Approval approval, ApprovalDecisionRequest request) {
		PoRequisition requisition = getRequisition(approval);
		if (!ApprovalStatus.REQUESTED.name().equals(requisition.getApprovalStatus())) {
			throw new IllegalStateException("결재 대기 상태의 발주품의만 승인할 수 있습니다.");
		}
		ProjectExecutionBudgetItem item = budgetItemRepository.findById(requisition.getExecutionItemId())
				.orElseThrow(() -> new EntityNotFoundException("실행항목을 찾을 수 없습니다: " + requisition.getExecutionItemId()));
		if (!item.isApproved()) {
			throw new IllegalStateException("실행예산 승인 후 발주를 승인할 수 있습니다.");
		}
		if (purchaseOrderRepository.existsByProjectIdAndBudgetId(approval.getProjectId(), requisition.getExecutionItemId())) {
			throw new IllegalStateException("이미 승인된 발주가 있는 실행항목입니다.");
		}
		VendorComparison selected = comparisonRepository.findById(requisition.getSelectedComparisonId())
				.orElseThrow(() -> new EntityNotFoundException("선정 비교견적을 찾을 수 없습니다: " + requisition.getSelectedComparisonId()));
		purchaseOrderRepository.save(new PurchaseOrder(nextPurchaseOrderId(), approval.getProjectId(), requisition.getExecutionItemId(), selected.getVendorName(), requisition.getPoAmount()));
		requisition.approve(decisionUser(request), request == null ? null : request.comment());
		requisitionRepository.save(requisition);
	}

	private Object detailFor(Approval approval) {
		if (TYPE_EXECUTION_BUDGET.equals(approval.getApprovalType())) {
			ExecutionBudgetDetailResponse response = executionBudgetDetailService.getDetails(approval.getProjectId());
			return new ApprovalResponse.ExecutionBudgetDetail(
					response.contractSupplyAmount(),
					response.targetCostLimit(),
					response.totalDetailAmount(),
					response.overTargetLimit(),
					response.summaryByItem(),
					response.details()
			);
		}
		if (TYPE_PURCHASE_ORDER.equals(approval.getApprovalType())) {
			PoRequisition requisition = getRequisition(approval);
			ProjectExecutionBudgetItem item = budgetItemRepository.findById(requisition.getExecutionItemId())
					.orElseThrow(() -> new EntityNotFoundException("실행항목을 찾을 수 없습니다: " + requisition.getExecutionItemId()));
			VendorComparison selected = comparisonRepository.findById(requisition.getSelectedComparisonId())
					.orElseThrow(() -> new EntityNotFoundException("선정 비교견적을 찾을 수 없습니다: " + requisition.getSelectedComparisonId()));
			List<VendorComparisonResponse.Item> comparisons = comparisonRepository.findByProjectIdAndExecutionItemIdOrderByComparisonIdAsc(approval.getProjectId(), requisition.getExecutionItemId()).stream()
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
							defaultZero(comparison.getQuotedAmount()).subtract(defaultZero(item.getApprovedBudgetAmount()))
					))
					.toList();
			return new ApprovalResponse.PurchaseOrderDetail(
					item.getItemId(),
					item.getCategoryName(),
					item.getItemName(),
					defaultZero(item.getApprovedBudgetAmount()),
					selected.getVendorName(),
					requisition.getPoAmount(),
					requisition.getBudgetVariance(),
					requisition.getSelectionReason(),
					comparisons
			);
		}
		return null;
	}

	private Approval getRequestedApproval(String approvalId) {
		Approval approval = getApprovalEntity(approvalId);
		if (approval.getStatus() != ApprovalStatus.REQUESTED) {
			throw new IllegalStateException("결재 대기 상태의 건만 처리할 수 있습니다.");
		}
		return approval;
	}

	private Approval getApprovalEntity(String approvalId) {
		return approvalRepository.findById(approvalId)
				.orElseThrow(() -> new EntityNotFoundException("결재 건을 찾을 수 없습니다: " + approvalId));
	}

	private PoRequisition getRequisition(Approval approval) {
		return requisitionRepository.findByApprovalId(approval.getApprovalId())
				.orElseGet(() -> requisitionRepository.findById(approval.getTargetId())
						.orElseThrow(() -> new EntityNotFoundException("발주품의를 찾을 수 없습니다: " + approval.getTargetId())));
	}

	private Project getProject(String projectId) {
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
	}

	private ApprovalStatus parseStatus(String status) {
		if (!StringUtils.hasText(status)) {
			return null;
		}
		return ApprovalStatus.valueOf(status);
	}

	private String nextApprovalId() {
		return "APR-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}

	private String nextPurchaseOrderId() {
		return "PO-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}

	private static String requestUser(ApprovalRequest request) {
		return request != null && StringUtils.hasText(request.requestUser()) ? request.requestUser() : "system";
	}

	private static String decisionUser(ApprovalDecisionRequest request) {
		return request != null && StringUtils.hasText(request.approver()) ? request.approver() : "대표";
	}

	private static BigDecimal defaultZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
