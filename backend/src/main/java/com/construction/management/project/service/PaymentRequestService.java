package com.construction.management.project.service;

import com.construction.management.project.api.ApprovalDecisionRequest;
import com.construction.management.project.api.PaymentRequestCreateRequest;
import com.construction.management.project.domain.ActualPayment;
import com.construction.management.project.domain.Approval;
import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.PaymentRequest;
import com.construction.management.project.domain.ProgressClaim;
import com.construction.management.project.domain.Project;
import com.construction.management.project.domain.PurchaseOrder;
import com.construction.management.project.repository.ActualPaymentRepository;
import com.construction.management.project.repository.ApprovalRepository;
import com.construction.management.project.repository.PaymentRequestRepository;
import com.construction.management.project.repository.ProgressClaimRepository;
import com.construction.management.project.repository.ProjectRepository;
import com.construction.management.project.repository.PurchaseOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentRequestService {
	public static final String TYPE_PAYMENT = "PAYMENT";
	private final ProjectRepository projectRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final ProgressClaimRepository progressClaimRepository;
	private final ActualPaymentRepository actualPaymentRepository;
	private final PaymentRequestRepository paymentRequestRepository;
	private final ApprovalRepository approvalRepository;
	private final ProjectSettlementService projectSettlementService;

	public PaymentRequestService(ProjectRepository projectRepository, PurchaseOrderRepository purchaseOrderRepository, ProgressClaimRepository progressClaimRepository, ActualPaymentRepository actualPaymentRepository, PaymentRequestRepository paymentRequestRepository, ApprovalRepository approvalRepository, ProjectSettlementService projectSettlementService) {
		this.projectRepository = projectRepository;
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.progressClaimRepository = progressClaimRepository;
		this.actualPaymentRepository = actualPaymentRepository;
		this.paymentRequestRepository = paymentRequestRepository;
		this.approvalRepository = approvalRepository;
		this.projectSettlementService = projectSettlementService;
	}

	@Transactional
	public void requestPayments(String projectId, PaymentRequestCreateRequest request) {
		projectSettlementService.assertNotSettled(projectId);
		Project project = projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
		List<String> poIds = request == null || request.poIds() == null ? List.of() : request.poIds().stream().distinct().toList();
		if (poIds.isEmpty()) throw new IllegalArgumentException("지급요청할 업체를 선택해야 합니다.");
		for (String poId : poIds) {
			PurchaseOrder po = purchaseOrderRepository.findById(poId).orElseThrow(() -> new EntityNotFoundException("발주를 찾을 수 없습니다: " + poId));
			if (!projectId.equals(po.getProjectId())) throw new IllegalArgumentException("다른 프로젝트의 업체는 지급요청할 수 없습니다.");
			if (paymentRequestRepository.existsByProjectIdAndPoIdAndApprovalStatus(projectId, poId, ApprovalStatus.REQUESTED.name())) throw new IllegalArgumentException("이미 지급결재 대기 중인 업체입니다: " + po.getVendorName());
			List<ProgressClaim> payableClaims = progressClaimRepository.findByPoId(poId).stream()
					.filter(claim -> claim.getApprovalStatus() == ApprovalStatus.APPROVED)
					.filter(claim -> remaining(claim).signum() > 0)
					.toList();
			BigDecimal requestedAmount = payableClaims.stream().map(this::remaining).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (requestedAmount.signum() <= 0) throw new IllegalArgumentException("지급 가능한 승인 기성이 없습니다: " + po.getVendorName());
			String approvalId = nextApprovalId();
			String paymentRequestId = "PAY-REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
			paymentRequestRepository.save(new PaymentRequest(paymentRequestId, projectId, poId, payableClaims.stream().map(ProgressClaim::getClaimId).collect(Collectors.joining(",")), requestedAmount, approvalId));
			approvalRepository.save(new Approval(approvalId, projectId, TYPE_PAYMENT, paymentRequestId, po.getVendorName() + " 지급 승인 요청", requestedAmount, BigDecimal.ZERO, requestedAmount, "system", "승인 기성 미지급분 지급 요청"));
		}
	}

	@Transactional
	public void approve(Approval approval, ApprovalDecisionRequest request) {
		PaymentRequest paymentRequest = getRequest(approval);
		if (!ApprovalStatus.REQUESTED.name().equals(paymentRequest.getApprovalStatus())) throw new IllegalStateException("결재 대기 상태의 지급요청만 승인할 수 있습니다.");
		List<ProgressClaim> claims = List.of(paymentRequest.getClaimIds().split(",")).stream().map(claimId -> progressClaimRepository.findById(claimId).orElseThrow(() -> new EntityNotFoundException("기성을 찾을 수 없습니다: " + claimId))).toList();
		BigDecimal currentAmount = claims.stream().filter(claim -> claim.getApprovalStatus() == ApprovalStatus.APPROVED).map(this::remaining).reduce(BigDecimal.ZERO, BigDecimal::add);
		if (currentAmount.compareTo(paymentRequest.getRequestedAmount()) != 0) throw new IllegalStateException("지급 대상 금액이 변경되었습니다. 기존 요청을 반려하고 다시 요청해야 합니다.");
		for (ProgressClaim claim : claims) {
			BigDecimal amount = remaining(claim);
			if (amount.signum() > 0) actualPaymentRepository.save(new ActualPayment("PAY-" + UUID.randomUUID().toString().substring(0, 8), claim.getClaimId(), amount, LocalDate.now(), "지급결재 승인", request == null ? null : request.comment()));
		}
		paymentRequest.approve();
		paymentRequestRepository.save(paymentRequest);
	}

	@Transactional
	public void reject(Approval approval) {
		PaymentRequest paymentRequest = getRequest(approval);
		paymentRequest.reject();
		paymentRequestRepository.save(paymentRequest);
	}

	private PaymentRequest getRequest(Approval approval) {
		return paymentRequestRepository.findByApprovalId(approval.getApprovalId()).orElseGet(() -> paymentRequestRepository.findById(approval.getTargetId()).orElseThrow(() -> new EntityNotFoundException("지급요청을 찾을 수 없습니다: " + approval.getTargetId())));
	}

	private BigDecimal remaining(ProgressClaim claim) {
		BigDecimal paid = actualPaymentRepository.findByClaimId(claim.getClaimId()).stream().map(ActualPayment::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		return claim.getClaimAmount().subtract(paid);
	}

	private String nextApprovalId() {
		return "APR-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}
}
