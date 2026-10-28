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
import com.construction.management.project.domain.ProjectSettlement;
import com.construction.management.project.domain.ProjectExecutionBudgetDetail;
import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import com.construction.management.project.domain.ProjectStatus;
import com.construction.management.project.domain.ProgressClaim;
import com.construction.management.project.domain.PurchaseOrder;
import com.construction.management.project.domain.VendorComparison;
import com.construction.management.project.repository.ApprovalRepository;
import com.construction.management.project.repository.PoRequisitionRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetDetailRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetItemRepository;
import com.construction.management.project.repository.ProjectRepository;
import com.construction.management.project.repository.ProjectSettlementRepository;
import com.construction.management.project.repository.ProgressClaimRepository;
import com.construction.management.project.repository.PurchaseOrderRepository;
import com.construction.management.project.repository.VendorComparisonRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROJECT_READ')")
public class ApprovalService {
	@org.springframework.beans.factory.annotation.Autowired private com.construction.management.auth.BusinessPolicy businessPolicy;

	public static final String TYPE_EXECUTION_BUDGET = "EXECUTION_BUDGET";
	public static final String TYPE_PURCHASE_ORDER = "PURCHASE_ORDER";
	public static final String TYPE_PROGRESS_CLAIM = "PROGRESS_CLAIM";
	public static final String TYPE_CONTRACT_CHANGE = "CONTRACT_CHANGE";
	public static final String TYPE_BUDGET_CHANGE = "BUDGET_CHANGE";
	public static final String TYPE_PO_CHANGE = "PO_CHANGE";
	public static final String TYPE_PROJECT_REVISION = "PROJECT_REVISION";
	public static final String TYPE_FINAL_SETTLEMENT = "FINAL_SETTLEMENT";

	private final ApprovalRepository approvalRepository;
	private final ProjectRepository projectRepository;
	private final ProjectExecutionBudgetItemRepository budgetItemRepository;
	private final ProjectExecutionBudgetDetailRepository budgetDetailRepository;
	private final PoRequisitionRepository requisitionRepository;
	private final VendorComparisonRepository comparisonRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final ProgressClaimRepository progressClaimRepository;
	private final ProjectSettlementRepository settlementRepository;
	private final ExecutionBudgetDetailService executionBudgetDetailService;
	private final ProjectSettlementService projectSettlementService;
	private final PaymentRequestService paymentRequestService;
	private final ExecutionBudgetApprovalAttachmentService attachmentService;
	private final VendorComparisonAttachmentService vendorComparisonAttachmentService;
	private final ChangeManagementService changeManagementService;
	private final ProjectRevisionService projectRevisionService;

	public ApprovalService(
			ApprovalRepository approvalRepository,
			ProjectRepository projectRepository,
			ProjectExecutionBudgetItemRepository budgetItemRepository,
			ProjectExecutionBudgetDetailRepository budgetDetailRepository,
			PoRequisitionRepository requisitionRepository,
			VendorComparisonRepository comparisonRepository,
			PurchaseOrderRepository purchaseOrderRepository,
			ProgressClaimRepository progressClaimRepository,
			ProjectSettlementRepository settlementRepository,
			ExecutionBudgetDetailService executionBudgetDetailService,
			ProjectSettlementService projectSettlementService,
			PaymentRequestService paymentRequestService,
			ExecutionBudgetApprovalAttachmentService attachmentService,
			VendorComparisonAttachmentService vendorComparisonAttachmentService,
			ChangeManagementService changeManagementService,
			ProjectRevisionService projectRevisionService
	) {
		this.approvalRepository = approvalRepository;
		this.projectRepository = projectRepository;
		this.budgetItemRepository = budgetItemRepository;
		this.budgetDetailRepository = budgetDetailRepository;
		this.requisitionRepository = requisitionRepository;
		this.comparisonRepository = comparisonRepository;
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.progressClaimRepository = progressClaimRepository;
		this.settlementRepository = settlementRepository;
		this.executionBudgetDetailService = executionBudgetDetailService;
		this.projectSettlementService = projectSettlementService;
		this.paymentRequestService = paymentRequestService;
		this.attachmentService = attachmentService;
		this.vendorComparisonAttachmentService = vendorComparisonAttachmentService;
		this.changeManagementService = changeManagementService;
		this.projectRevisionService = projectRevisionService;
	}

	@Transactional(readOnly = true)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('APPROVAL_READ')")
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
				.filter(businessPolicy::canSeeApproval)
				.map(approval -> ApprovalResponse.from(approval, getProject(approval.getProjectId())))
				.toList();
	}

	@Transactional(readOnly = true)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('APPROVAL_READ')")
	public ApprovalResponse getApproval(String approvalId) {
		businessPolicy.approval(approvalId);
		Approval approval = getApprovalEntity(approvalId);
		return ApprovalResponse.from(
				approval,
				getProject(approval.getProjectId()),
				detailFor(approval),
				attachmentsFor(approval)
		);
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('BUDGET_REQUEST')")
	@com.construction.management.auth.BusinessAction
	public ApprovalResponse requestExecutionBudget(String projectId, ApprovalRequest request) {
		businessPolicy.draft("BUDGET", projectId, null);
		return requestExecutionBudget(projectId, request, List.of());
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('BUDGET_REQUEST')")
	@com.construction.management.auth.BusinessAction
	public ApprovalResponse requestExecutionBudget(String projectId, ApprovalRequest request, List<MultipartFile> files) {
		businessPolicy.draft("BUDGET", projectId, null);
		projectSettlementService.assertNotSettled(projectId);
		Project project = getProject(projectId);
		if (project.getStatus() == ProjectStatus.견적) {
			throw new IllegalStateException("고객 계약을 저장하여 계약 상태로 변경한 후 실행예산 결재를 요청할 수 있습니다.");
		}
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
		boolean rewriteAvailable = projectRevisionService.isExecutionBudgetRewriteAvailable(projectId);
		if (approvalRepository.existsByProjectIdAndApprovalTypeAndStatus(projectId, TYPE_EXECUTION_BUDGET, ApprovalStatus.APPROVED) && !rewriteAvailable) {
			throw new IllegalArgumentException("이미 승인된 최초 실행예산이 있습니다. 변경은 변경관리에서 처리해야 합니다.");
		}

		// A request is based on the detail rows.  Synchronize the item summary first
		// so the approval snapshot and the project summary can never diverge.
		synchronizeBudgetItemSummaries(projectId, details);

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
		attachmentService.saveAll(projectId, approval.getApprovalId(), files);
		budgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId).forEach(item -> {
			item.markApprovalRequested(approval.getApprovalId());
			budgetItemRepository.save(item);
		});
		if (rewriteAvailable) {
			projectRevisionService.consumeExecutionBudgetRewrite(projectId);
		}
		return ApprovalResponse.from(
				approval,
				project,
				detailFor(approval),
				attachmentService.getResponses(approval.getApprovalId(), projectId)
		);
	}

	@Transactional
	private void synchronizeBudgetItemSummaries(String projectId, List<ProjectExecutionBudgetDetail> details) {
		Map<String, List<ProjectExecutionBudgetDetail>> detailsByItem = details.stream()
				.collect(Collectors.groupingBy(ProjectExecutionBudgetDetail::getBudgetItemId, LinkedHashMap::new, Collectors.toList()));
		Map<String, ProjectExecutionBudgetItem> itemsByBudgetItemId = budgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId).stream()
				.collect(Collectors.toMap(
						item -> ProjectExecutionBudgetItem.budgetItemId(projectId, item.getItemId()),
						item -> item,
						(left, right) -> left,
						LinkedHashMap::new
				));

		for (Map.Entry<String, List<ProjectExecutionBudgetDetail>> entry : detailsByItem.entrySet()) {
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
					.map(ApprovalService::defaultZero)
					.reduce(BigDecimal.ZERO, BigDecimal::add));
			budgetItemRepository.save(item);
		}

		budgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId).stream()
				.filter(item -> !detailsByItem.containsKey(ProjectExecutionBudgetItem.budgetItemId(projectId, item.getItemId())))
				.forEach(item -> {
					item.updateBudgetAmount(BigDecimal.ZERO);
					budgetItemRepository.save(item);
				});
	}

	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROCUREMENT_REQUEST')")
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
				com.construction.management.auth.CurrentActor.name(),
				requestReason
		);
		approvalRepository.save(approval);
		requisition.linkApproval(approval.getApprovalId());
		requisitionRepository.save(requisition);
		return approval;
	}

	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROGRESS_WRITE')")
	public Approval createProgressClaimApproval(Project project, PurchaseOrder purchaseOrder, ProgressClaim claim, BigDecimal approvedClaimTotal) {
		Approval approval = new Approval(
				nextApprovalId(),
				project.getProjectId(),
				TYPE_PROGRESS_CLAIM,
				claim.getClaimId(),
				purchaseOrder.getVendorName() + " " + claim.getDegree() + "차 기성 승인 요청",
				claim.getClaimAmount(),
				defaultZero(approvedClaimTotal),
				claim.getClaimAmount().subtract(defaultZero(approvedClaimTotal)),
				com.construction.management.auth.CurrentActor.name(),
				purchaseOrder.getPoId() + " 기성 " + claim.getDegree() + "차 승인 요청"
		);
		approvalRepository.save(approval);
		claim.linkApproval(approval.getApprovalId());
		progressClaimRepository.save(claim);
		return approval;
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('APPROVAL_DECIDE')")
	@com.construction.management.auth.BusinessAction
	public ApprovalResponse approve(String approvalId, ApprovalDecisionRequest request) {
		Approval approval = getRequestedApproval(approvalId);
		if (TYPE_EXECUTION_BUDGET.equals(approval.getApprovalType())) {
			approveExecutionBudget(approval);
		} else if (TYPE_PURCHASE_ORDER.equals(approval.getApprovalType())) {
			approvePurchaseOrder(approval, request);
		} else if (TYPE_PROGRESS_CLAIM.equals(approval.getApprovalType())) {
			approveProgressClaim(approval);
		} else if (PaymentRequestService.TYPE_PAYMENT.equals(approval.getApprovalType())) {
			paymentRequestService.approve(approval, request);
		} else if (isChangeApproval(approval)) {
			changeManagementService.approveChangeApproval(approval);
		} else if (TYPE_PROJECT_REVISION.equals(approval.getApprovalType())) {
			projectRevisionService.approveRevision(approval.getProjectId(), approval.getTargetId(), approval.getApprovalId());
		} else if (TYPE_FINAL_SETTLEMENT.equals(approval.getApprovalType())) {
			projectSettlementService.approveRequestedSettlement(approval.getProjectId(), approval.getTargetId(), decisionUser(request));
		} else {
			throw new IllegalArgumentException("지원하지 않는 결재 유형입니다: " + approval.getApprovalType());
		}
		approval.approve(decisionUser(request), request == null ? null : request.comment());
		approvalRepository.save(approval);
		return ApprovalResponse.from(approval, getProject(approval.getProjectId()), detailFor(approval), attachmentsFor(approval));
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('APPROVAL_DECIDE')")
	@com.construction.management.auth.BusinessAction
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
		} else if (TYPE_PROGRESS_CLAIM.equals(approval.getApprovalType())) {
			ProgressClaim claim = getProgressClaim(approval);
			claim.reject();
			progressClaimRepository.save(claim);
		} else if (PaymentRequestService.TYPE_PAYMENT.equals(approval.getApprovalType())) {
			paymentRequestService.reject(approval);
		} else if (isChangeApproval(approval)) {
			changeManagementService.rejectChangeApproval(approval);
		} else if (TYPE_PROJECT_REVISION.equals(approval.getApprovalType())) {
			projectRevisionService.rejectRevision(approval.getProjectId(), approval.getTargetId());
		} else if (TYPE_FINAL_SETTLEMENT.equals(approval.getApprovalType())) {
			projectSettlementService.rejectRequestedSettlement(approval.getProjectId(), approval.getTargetId());
		}
		approval.reject(decisionUser(request), request.comment());
		approvalRepository.save(approval);
		return ApprovalResponse.from(approval, getProject(approval.getProjectId()), detailFor(approval), attachmentsFor(approval));
	}

	private static boolean isChangeApproval(Approval approval) {
		return TYPE_CONTRACT_CHANGE.equals(approval.getApprovalType())
				|| TYPE_BUDGET_CHANGE.equals(approval.getApprovalType())
				|| TYPE_PO_CHANGE.equals(approval.getApprovalType());
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('APPROVAL_DECIDE')")
	@com.construction.management.auth.BusinessAction
	public ApprovalResponse cancelApprovedExecutionBudget(String approvalId, ApprovalDecisionRequest request) {
		Approval approval = getApprovalEntity(approvalId);
		if (!TYPE_EXECUTION_BUDGET.equals(approval.getApprovalType())) {
			throw new IllegalArgumentException("승인취소 후 재작업은 실행예산 결재에만 사용할 수 있습니다.");
		}
		if (approval.getStatus() != ApprovalStatus.APPROVED) {
			throw new IllegalStateException("승인 완료된 실행예산 결재만 승인취소할 수 있습니다.");
		}
		if (request == null || !StringUtils.hasText(request.comment())) {
			throw new IllegalArgumentException("승인취소 사유를 입력해야 합니다.");
		}
		projectSettlementService.assertNotSettled(approval.getProjectId());
		if (purchaseOrderRepository.existsByProjectId(approval.getProjectId())) {
			throw new IllegalStateException("발주가 생성된 실행예산은 승인취소할 수 없습니다. 변경관리에서 실행예산 변경결재로 처리하세요.");
		}

		budgetItemRepository.findByProjectIdOrderByItemIdAsc(approval.getProjectId()).forEach(item -> {
			item.reject(approval.getApprovalId(), "승인취소: " + request.comment());
			budgetItemRepository.save(item);
		});
		approval.cancel(decisionUser(request), request.comment());
		approvalRepository.save(approval);
		return ApprovalResponse.from(approval, getProject(approval.getProjectId()), detailFor(approval), attachmentsFor(approval));
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
		Project project = getProject(approval.getProjectId());
		if (project.getStatus() == ProjectStatus.계약) {
			project.changeStatus(ProjectStatus.공사준비);
			projectRepository.save(project);
		}
	}

	private void approvePurchaseOrder(Approval approval, ApprovalDecisionRequest request) {
		PoRequisition requisition = getRequisition(approval);
		if (!ApprovalStatus.REQUESTED.name().equals(requisition.getApprovalStatus())) {
			throw new IllegalStateException("결재 대기 상태의 발주품의만 승인할 수 있습니다.");
		}
		ProjectExecutionBudgetItem item = budgetItemRepository.findByProjectIdAndBudgetItemId(approval.getProjectId(), requisition.getExecutionItemId())
				.orElseThrow(() -> new EntityNotFoundException("실행항목을 찾을 수 없습니다: " + requisition.getExecutionItemId()));
		if (!item.isApproved()) {
			throw new IllegalStateException("실행예산 승인 후 발주를 승인할 수 있습니다.");
		}
		if (purchaseOrderRepository.existsByProjectIdAndBudgetIdAndRevisionId(approval.getProjectId(), requisition.getExecutionItemId(), requisition.getRevisionId())) {
			throw new IllegalStateException("이미 승인된 발주가 있는 실행항목입니다.");
		}
		VendorComparison selected = comparisonRepository.findById(requisition.getSelectedComparisonId())
				.orElseThrow(() -> new EntityNotFoundException("선정 비교견적을 찾을 수 없습니다: " + requisition.getSelectedComparisonId()));
		if (!selected.isSelected()) {
			throw new IllegalStateException("선정되지 않은 비교견적으로 발주를 승인할 수 없습니다.");
		}
		if (defaultZero(selected.getQuotedAmount()).compareTo(defaultZero(requisition.getPoAmount())) != 0
				|| !Objects.equals(selected.getSelectionReason(), requisition.getSelectionReason())) {
			throw new IllegalStateException("선정 비교견적과 발주품의 내용이 일치하지 않습니다. 발주품의를 다시 요청해야 합니다.");
		}
		String approver = decisionUser(request);
		String comment = request == null ? null : request.comment();
		purchaseOrderRepository.save(new PurchaseOrder(nextPurchaseOrderId(), approval.getProjectId(), requisition.getExecutionItemId(), requisition, selected, approver, comment));
		requisition.approve(approver, comment);
		requisitionRepository.save(requisition);
	}

	private void approveProgressClaim(Approval approval) {
		ProgressClaim claim = getProgressClaim(approval);
		if (claim.getApprovalStatus() != ApprovalStatus.REQUESTED) {
			throw new IllegalStateException("결재 대기 상태의 기성만 승인할 수 있습니다.");
		}
		claim.approve();
		progressClaimRepository.save(claim);
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
			ProjectExecutionBudgetItem item = budgetItemRepository.findByProjectIdAndBudgetItemId(approval.getProjectId(), requisition.getExecutionItemId())
					.orElseThrow(() -> new EntityNotFoundException("실행항목을 찾을 수 없습니다: " + requisition.getExecutionItemId()));
			VendorComparison selected = comparisonRepository.findById(requisition.getSelectedComparisonId())
					.orElseThrow(() -> new EntityNotFoundException("선정 비교견적을 찾을 수 없습니다: " + requisition.getSelectedComparisonId()));
			List<VendorComparisonResponse.Item> comparisons = comparisonRepository.findByProjectIdAndExecutionItemIdAndRevisionIdOrderByComparisonIdAsc(
					approval.getProjectId(), requisition.getExecutionItemId(), requisition.getRevisionId()).stream()
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
							vendorComparisonAttachmentService.getResponses(approval.getProjectId(), requisition.getExecutionItemId(), comparison.getComparisonId()),
							comparison.isSelected(),
							comparison.getSelectionReason(),
							defaultZero(comparison.getQuotedAmount()).subtract(defaultZero(item.getApprovedBudgetAmount()))
					))
					.toList();
			return new ApprovalResponse.PurchaseOrderDetail(
					purchaseOrderRepository.findByRequisitionId(requisition.getRequisitionId()).map(PurchaseOrder::getPoId).orElse(null),
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
		if (TYPE_PROGRESS_CLAIM.equals(approval.getApprovalType())) {
			ProgressClaim claim = getProgressClaim(approval);
			PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(claim.getPoId())
					.orElseThrow(() -> new EntityNotFoundException("발주를 찾을 수 없습니다: " + claim.getPoId()));
			ProjectExecutionBudgetItem item = budgetItemRepository.findByProjectIdAndBudgetItemId(approval.getProjectId(), purchaseOrder.getBudgetId()).orElse(null);
			BigDecimal approvedClaimTotal = progressClaimRepository.findByPoId(claim.getPoId()).stream()
					.filter(candidate -> candidate.getApprovalStatus() == ApprovalStatus.APPROVED)
					.map(ProgressClaim::getClaimAmount).map(ApprovalService::defaultZero).reduce(BigDecimal.ZERO, BigDecimal::add);
			return new ApprovalResponse.ProgressClaimDetail(
					claim.getClaimId(), purchaseOrder.getPoId(), purchaseOrder.getVendorName(), purchaseOrder.getBudgetId(),
					item == null ? null : item.getCategoryName(), item == null ? null : item.getItemName(), claim.getDegree(), claim.getClaimAmount(), approvedClaimTotal);
		}
		if (TYPE_FINAL_SETTLEMENT.equals(approval.getApprovalType())) {
			ProjectSettlement settlement = settlementRepository.findById(approval.getTargetId())
					.orElseThrow(() -> new EntityNotFoundException("준공정산 요청을 찾을 수 없습니다: " + approval.getTargetId()));
			ProjectSettlement.ProjectSettlementSnapshot snapshot = settlement.snapshot();
			BigDecimal settlementSiteExpense = settlement.getSettlementSiteExpense();
			BigDecimal settlementLaborExpense = settlement.getSettlementLaborExpense();
			if (settlementSiteExpense.add(settlementLaborExpense).signum() == 0 && defaultZero(settlement.getSettlementExpense()).signum() != 0) {
				settlementSiteExpense = settlement.getSettlementExpense();
			}
			return new ApprovalResponse.FinalSettlementDetail(
					snapshot.finalContractAmount(), snapshot.finalBudgetAmount(), snapshot.finalPoAmount(),
					settlement.getSettlementExpense(), settlementSiteExpense, settlementLaborExpense, settlement.getSettlementTax(), snapshot.unpaidBalance(),
					snapshot.finalCost(), snapshot.finalProfit(), snapshot.profitMarginRate()
			);
		}
		return null;
	}

	private List<ApprovalResponse.Attachment> attachmentsFor(Approval approval) {
		if (TYPE_EXECUTION_BUDGET.equals(approval.getApprovalType())) {
			return attachmentService.getResponses(approval.getApprovalId(), approval.getProjectId());
		}
		if (TYPE_PURCHASE_ORDER.equals(approval.getApprovalType())) {
			PoRequisition requisition = getRequisition(approval);
			return comparisonRepository.findByProjectIdAndExecutionItemIdAndRevisionIdOrderByComparisonIdAsc(
					approval.getProjectId(), requisition.getExecutionItemId(), requisition.getRevisionId())
					.stream()
					.flatMap(comparison -> vendorComparisonAttachmentService.getResponses(
							approval.getProjectId(), requisition.getExecutionItemId(), comparison.getComparisonId()).stream())
					.toList();
		}
		return List.of();
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

	private ProgressClaim getProgressClaim(Approval approval) {
		return progressClaimRepository.findById(approval.getTargetId())
				.orElseThrow(() -> new EntityNotFoundException("기성 정보를 찾을 수 없습니다: " + approval.getTargetId()));
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
		String prefix = "PO-" + Year.now().getValue() + "-";
		int nextSequence = purchaseOrderRepository.findByPoIdStartingWithOrderByPoIdDesc(prefix).stream()
				.map(PurchaseOrder::getPoId)
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

	private static String requestUser(ApprovalRequest request) {
		return com.construction.management.auth.CurrentActor.name();
	}

	private static String decisionUser(ApprovalDecisionRequest request) { return com.construction.management.auth.CurrentActor.name(); }

	private static BigDecimal defaultZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
