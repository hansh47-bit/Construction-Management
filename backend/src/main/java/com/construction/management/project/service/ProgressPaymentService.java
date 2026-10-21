package com.construction.management.project.service;

import com.construction.management.project.api.ProgressPaymentRequest;
import com.construction.management.project.api.ProgressPaymentResponse;
import com.construction.management.project.api.ProgressPaymentResponse.ClaimStatus;
import com.construction.management.project.api.ProgressPaymentResponse.PaymentStatus;
import com.construction.management.project.api.ProgressPaymentResponse.PurchaseOrderStatus;
import com.construction.management.project.domain.ActualPayment;
import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.PoChange;
import com.construction.management.project.domain.PaymentRequest;
import com.construction.management.project.domain.ProgressClaim;
import com.construction.management.project.domain.Project;
import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import com.construction.management.project.domain.ProjectRevision;
import com.construction.management.project.domain.PurchaseOrder;
import com.construction.management.project.repository.ActualPaymentRepository;
import com.construction.management.project.repository.PoChangeRepository;
import com.construction.management.project.repository.PaymentRequestRepository;
import com.construction.management.project.repository.ProgressClaimRepository;
import com.construction.management.project.repository.ProjectRepository;
import com.construction.management.project.repository.ProjectRevisionRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetItemRepository;
import com.construction.management.project.repository.PurchaseOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProgressPaymentService {
	private final ProjectRepository projectRepository;
	private final ProjectRevisionRepository revisionRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final PoChangeRepository poChangeRepository;
	private final ProgressClaimRepository progressClaimRepository;
	private final ActualPaymentRepository actualPaymentRepository;
	private final ProjectSettlementService projectSettlementService;
	private final PaymentRequestRepository paymentRequestRepository;
	private final ProjectExecutionBudgetItemRepository budgetItemRepository;
	private final ApprovalService approvalService;

	public ProgressPaymentService(ProjectRepository projectRepository, ProjectRevisionRepository revisionRepository, PurchaseOrderRepository purchaseOrderRepository, PoChangeRepository poChangeRepository, ProgressClaimRepository progressClaimRepository, ActualPaymentRepository actualPaymentRepository, ProjectSettlementService projectSettlementService, PaymentRequestRepository paymentRequestRepository, ProjectExecutionBudgetItemRepository budgetItemRepository, ApprovalService approvalService) {
		this.projectRepository = projectRepository;
		this.revisionRepository = revisionRepository;
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.poChangeRepository = poChangeRepository;
		this.progressClaimRepository = progressClaimRepository;
		this.actualPaymentRepository = actualPaymentRepository;
		this.projectSettlementService = projectSettlementService;
		this.paymentRequestRepository = paymentRequestRepository;
		this.budgetItemRepository = budgetItemRepository;
		this.approvalService = approvalService;
	}

	@Transactional
	public ProgressPaymentResponse getStatus(String projectId) {
		Project project = projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
		ensurePendingClaimApprovals(project);
		return toResponse(project);
	}

	@Transactional
	public ProgressPaymentResponse createClaim(String projectId, ProgressPaymentRequest.Claim request) {
		projectSettlementService.assertNotSettled(projectId);
		Project project = getProject(projectId);
		PurchaseOrder po = getPo(projectId, request.poId());
		BigDecimal currentPoAmount = currentPoAmount(po);
		BigDecimal approvedClaimTotal = progressClaimRepository.findByPoId(po.getPoId()).stream()
				.filter(claim -> claim.getApprovalStatus() == ApprovalStatus.APPROVED)
				.map(ProgressClaim::getClaimAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal newAmount = defaultZero(request.claimAmount());
		if (approvedClaimTotal.add(newAmount).compareTo(currentPoAmount) > 0) {
			throw new IllegalArgumentException("기성 신청 금액이 현재 발주금액을 초과합니다.");
		}
		ProgressClaim claim = progressClaimRepository.save(new ProgressClaim("CLM-" + UUID.randomUUID().toString().substring(0, 8), po.getPoId(), request.degree(), newAmount, ApprovalStatus.REQUESTED));
		approvalService.createProgressClaimApproval(project, po, claim, approvedClaimTotal);
		return toResponse(project);
	}

	@Transactional
	public ProgressPaymentResponse createPayment(String projectId, String claimId, ProgressPaymentRequest.Payment request) {
		projectSettlementService.assertNotSettled(projectId);
		Project project = getProject(projectId);
		ProgressClaim claim = progressClaimRepository.findById(claimId).orElseThrow(() -> new EntityNotFoundException("기성을 찾을 수 없습니다: " + claimId));
		getPo(projectId, claim.getPoId());
		if (claim.getApprovalStatus() != ApprovalStatus.APPROVED) {
			throw new IllegalArgumentException("승인된 기성만 지급 처리할 수 있습니다.");
		}
		BigDecimal existingPaid = actualPaymentRepository.findByClaimId(claimId).stream()
				.map(ActualPayment::getPaidAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal paidAmount = defaultZero(request.paidAmount());
		if (existingPaid.add(paidAmount).compareTo(claim.getClaimAmount()) > 0) {
			throw new IllegalArgumentException("실제 지급 금액이 승인 기성금액을 초과합니다.");
		}
		actualPaymentRepository.save(new ActualPayment("PAY-" + UUID.randomUUID().toString().substring(0, 8), claimId, paidAmount, request.paidDate(), request.accountInfo(), request.receiptLink()));
		return toResponse(project);
	}

	private ProgressPaymentResponse toResponse(Project project) {
		ProjectRevision currentRevision = currentRevision(project.getProjectId());
		List<PurchaseOrder> pos = purchaseOrderRepository.findByProjectIdAndRevisionIdOrderByPoIdDesc(
				project.getProjectId(), currentRevision == null ? null : currentRevision.getRevisionId());
		Map<String, ProjectExecutionBudgetItem> budgetItemsById = budgetItemRepository.findByProjectIdOrderByItemIdAsc(project.getProjectId()).stream()
				.collect(Collectors.toMap(item -> ProjectExecutionBudgetItem.budgetItemId(project.getProjectId(), item.getItemId()), item -> item));
		List<String> poIds = pos.stream().map(PurchaseOrder::getPoId).toList();
		Map<String, BigDecimal> poChanges = poIds.isEmpty() ? Map.of() : poChangeRepository.findByPoIdInAndApprovalStatus(poIds, ApprovalStatus.APPROVED).stream()
				.collect(Collectors.groupingBy(PoChange::getPoId, Collectors.mapping(change -> defaultZero(change.getChangeAmount()), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
		List<ProgressClaim> claims = poIds.isEmpty() ? List.of() : progressClaimRepository.findByPoIdIn(poIds);
		Map<String, List<ProgressClaim>> claimsByPo = claims.stream().collect(Collectors.groupingBy(ProgressClaim::getPoId));
		List<String> claimIds = claims.stream().map(ProgressClaim::getClaimId).toList();
		Map<String, List<ActualPayment>> paymentsByClaim = claimIds.isEmpty() ? Map.of() : actualPaymentRepository.findByClaimIdIn(claimIds).stream().collect(Collectors.groupingBy(ActualPayment::getClaimId));
		Map<String, List<PaymentRequest>> paymentRequestsByPo = paymentRequestRepository.findByProjectId(project.getProjectId()).stream().collect(Collectors.groupingBy(PaymentRequest::getPoId));

		List<PurchaseOrderStatus> purchaseOrderStatuses = pos.stream().map(po -> {
			ProjectExecutionBudgetItem budgetItem = budgetItemsById.get(po.getBudgetId());
			BigDecimal currentPo = defaultZero(po.getInitialPoAmount()).add(poChanges.getOrDefault(po.getPoId(), BigDecimal.ZERO));
			List<ProgressClaim> poClaims = claimsByPo.getOrDefault(po.getPoId(), List.of()).stream().sorted(Comparator.comparing(ProgressClaim::getDegree)).toList();
			BigDecimal approvedClaimTotal = poClaims.stream().filter(claim -> claim.getApprovalStatus() == ApprovalStatus.APPROVED).map(ProgressClaim::getClaimAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal paidTotal = poClaims.stream().flatMap(claim -> paymentsByClaim.getOrDefault(claim.getClaimId(), List.of()).stream()).map(ActualPayment::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			String paymentRequestStatus = paymentRequestsByPo.getOrDefault(po.getPoId(), List.of()).stream()
					.map(PaymentRequest::getApprovalStatus)
					.filter(status -> ApprovalStatus.REQUESTED.name().equals(status))
					.findFirst()
					.orElse(approvedClaimTotal.signum() > 0 && paidTotal.compareTo(approvedClaimTotal) >= 0 ? "PAID" : "READY");
			return new PurchaseOrderStatus(po.getPoId(), po.getVendorName(), po.getBudgetId(), budgetItem == null ? null : budgetItem.getCategoryName(), budgetItem == null ? null : budgetItem.getItemName(), currentPo, approvedClaimTotal, paidTotal, currentPo.subtract(approvedClaimTotal), approvedClaimTotal.subtract(paidTotal), currentPo.subtract(paidTotal), paymentRequestStatus, poClaims.stream().map(claim -> {
				List<ActualPayment> payments = paymentsByClaim.getOrDefault(claim.getClaimId(), List.of());
				BigDecimal claimPaid = payments.stream().map(ActualPayment::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
				String paymentState = claim.getApprovalStatus() == ApprovalStatus.REQUESTED ? "REQUESTED" : claimPaid.compareTo(claim.getClaimAmount()) >= 0 ? "PAID" : ApprovalStatus.REQUESTED.name().equals(paymentRequestStatus) ? "PAYMENT_APPROVAL_REQUESTED" : "WAITING_FOR_PAYMENT";
				return new ClaimStatus(claim.getClaimId(), claim.getDegree(), claim.getClaimAmount(), claim.getApprovalStatus().name(), paymentState, claimPaid, claim.getClaimAmount().subtract(claimPaid), payments.stream().map(payment -> new PaymentStatus(payment.getPaymentId(), payment.getPaidAmount(), payment.getPaidDate(), payment.getPaymentStatus(), payment.getAccountInfo(), payment.getReceiptLink())).toList());
			}).toList());
		}).toList();
		long pendingPaymentApprovalCount = purchaseOrderStatuses.stream().filter(po -> ApprovalStatus.REQUESTED.name().equals(po.paymentRequestStatus())).count();
		boolean allPaymentsApproved = !purchaseOrderStatuses.isEmpty() && purchaseOrderStatuses.stream().allMatch(po -> po.totalUnpaidBalance().signum() == 0);
		return new ProgressPaymentResponse(project.getProjectId(), project.getProjectCode(), currentRevision == null ? null : currentRevision.getRevisionNumber(), allPaymentsApproved, pendingPaymentApprovalCount, purchaseOrderStatuses);
	}

	private void ensurePendingClaimApprovals(Project project) {
		ProjectRevision currentRevision = currentRevision(project.getProjectId());
		List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findByProjectIdAndRevisionIdOrderByPoIdDesc(
				project.getProjectId(), currentRevision == null ? null : currentRevision.getRevisionId());
		Map<String, PurchaseOrder> purchaseOrdersById = purchaseOrders.stream()
				.collect(Collectors.toMap(PurchaseOrder::getPoId, purchaseOrder -> purchaseOrder));
		List<String> poIds = purchaseOrders.stream().map(PurchaseOrder::getPoId).toList();
		if (poIds.isEmpty()) return;

		for (ProgressClaim claim : progressClaimRepository.findByPoIdIn(poIds)) {
			if (claim.getApprovalStatus() != ApprovalStatus.REQUESTED || StringUtils.hasText(claim.getApprovalId())) continue;
			PurchaseOrder purchaseOrder = purchaseOrdersById.get(claim.getPoId());
			if (purchaseOrder == null) continue;
			BigDecimal approvedClaimTotal = progressClaimRepository.findByPoId(claim.getPoId()).stream()
					.filter(candidate -> candidate.getApprovalStatus() == ApprovalStatus.APPROVED)
					.map(ProgressClaim::getClaimAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			approvalService.createProgressClaimApproval(project, purchaseOrder, claim, approvedClaimTotal);
		}
	}

	private Project getProject(String projectId) {
		return projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
	}

	private PurchaseOrder getPo(String projectId, String poId) {
		PurchaseOrder po = purchaseOrderRepository.findById(poId).orElseThrow(() -> new EntityNotFoundException("발주를 찾을 수 없습니다: " + poId));
		if (!projectId.equals(po.getProjectId())) throw new EntityNotFoundException("프로젝트에 속한 발주가 아닙니다.");
		ProjectRevision revision = currentRevision(projectId);
		if (!java.util.Objects.equals(revision == null ? null : revision.getRevisionId(), po.getRevisionId())) {
			throw new IllegalArgumentException("현재 승인 차수의 신규 발주만 기성 및 지급 처리할 수 있습니다.");
		}
		return po;
	}

	private ProjectRevision currentRevision(String projectId) {
		return revisionRepository.findTopByProjectIdAndStatusOrderByRevisionNumberDesc(projectId, "APPROVED").orElse(null);
	}

	private BigDecimal currentPoAmount(PurchaseOrder po) {
		BigDecimal changes = poChangeRepository.findByPoIdInAndApprovalStatus(List.of(po.getPoId()), ApprovalStatus.APPROVED).stream().map(PoChange::getChangeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		return defaultZero(po.getInitialPoAmount()).add(changes);
	}

	private static BigDecimal defaultZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
