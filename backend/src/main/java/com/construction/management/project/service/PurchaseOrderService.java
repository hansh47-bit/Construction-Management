package com.construction.management.project.service;

import com.construction.management.project.api.PurchaseOrderResponse;
import com.construction.management.project.api.PurchaseOrderResponse.Item;
import com.construction.management.project.domain.ActualPayment;
import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.BudgetChange;
import com.construction.management.project.domain.PoChange;
import com.construction.management.project.domain.ProgressClaim;
import com.construction.management.project.domain.Project;
import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import com.construction.management.project.domain.PurchaseOrder;
import com.construction.management.project.domain.ProjectRevision;
import com.construction.management.project.repository.ActualPaymentRepository;
import com.construction.management.project.repository.BudgetChangeRepository;
import com.construction.management.project.repository.PoChangeRepository;
import com.construction.management.project.repository.ProgressClaimRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetItemRepository;
import com.construction.management.project.repository.ProjectRepository;
import com.construction.management.project.repository.ProjectRevisionRepository;
import com.construction.management.project.repository.PurchaseOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROJECT_READ')")
public class PurchaseOrderService {
	@org.springframework.beans.factory.annotation.Autowired private com.construction.management.auth.BusinessPolicy businessPolicy;

	private final ProjectRepository projectRepository;
	private final ProjectRevisionRepository revisionRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final ProjectExecutionBudgetItemRepository budgetItemRepository;
	private final PoChangeRepository poChangeRepository;
	private final ProgressClaimRepository progressClaimRepository;
	private final ActualPaymentRepository actualPaymentRepository;
	private final BudgetChangeRepository budgetChangeRepository;

	public PurchaseOrderService(ProjectRepository projectRepository, ProjectRevisionRepository revisionRepository, PurchaseOrderRepository purchaseOrderRepository, ProjectExecutionBudgetItemRepository budgetItemRepository, PoChangeRepository poChangeRepository, ProgressClaimRepository progressClaimRepository, ActualPaymentRepository actualPaymentRepository, BudgetChangeRepository budgetChangeRepository) {
		this.projectRepository = projectRepository;
		this.revisionRepository = revisionRepository;
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.budgetItemRepository = budgetItemRepository;
		this.poChangeRepository = poChangeRepository;
		this.progressClaimRepository = progressClaimRepository;
		this.actualPaymentRepository = actualPaymentRepository;
		this.budgetChangeRepository = budgetChangeRepository;
	}

	@Transactional(readOnly = true)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROCUREMENT_READ')")
	public PurchaseOrderResponse getPurchaseOrders(String projectId) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
		ProjectRevision currentRevision = revisionRepository.findTopByProjectIdAndStatusOrderByRevisionNumberDesc(projectId, "APPROVED").orElse(null);
		List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findByProjectIdAndRevisionIdOrderByPoIdDesc(
				projectId, currentRevision == null ? null : currentRevision.getRevisionId());
		List<String> poIds = purchaseOrders.stream().map(PurchaseOrder::getPoId).toList();
		Map<String, BigDecimal> changesByPo = poIds.isEmpty() ? Map.of() : poChangeRepository.findByPoIdInAndApprovalStatus(poIds, ApprovalStatus.APPROVED).stream()
				.collect(Collectors.groupingBy(PoChange::getPoId, Collectors.mapping(change -> zero(change.getChangeAmount()), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
		List<String> budgetIds = purchaseOrders.stream().map(PurchaseOrder::getBudgetId).filter(id -> id != null && !id.isBlank()).distinct().toList();
		Map<String, BigDecimal> changesByBudget = budgetIds.isEmpty() ? Map.of() : budgetChangeRepository.findByBudgetIdInAndApprovalStatus(budgetIds, ApprovalStatus.APPROVED).stream()
				.collect(Collectors.groupingBy(BudgetChange::getBudgetId, Collectors.mapping(change -> zero(change.getChangeAmount()), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
		List<ProgressClaim> claims = poIds.isEmpty() ? List.of() : progressClaimRepository.findByPoIdIn(poIds);
		Map<String, List<ProgressClaim>> claimsByPo = claims.stream().collect(Collectors.groupingBy(ProgressClaim::getPoId));
		List<String> claimIds = claims.stream().map(ProgressClaim::getClaimId).toList();
		Map<String, List<ActualPayment>> paymentsByClaim = claimIds.isEmpty() ? Map.of() : actualPaymentRepository.findByClaimIdIn(claimIds).stream().collect(Collectors.groupingBy(ActualPayment::getClaimId));

		return new PurchaseOrderResponse(projectId, project.getProjectCode(), currentRevision == null ? null : currentRevision.getRevisionNumber(), purchaseOrders.stream().map(po -> {
			ProjectExecutionBudgetItem budgetItem = budgetItemRepository.findByProjectIdAndBudgetItemId(projectId, po.getBudgetId()).orElse(null);
			BigDecimal currentPoAmount = zero(po.getInitialPoAmount()).add(changesByPo.getOrDefault(po.getPoId(), BigDecimal.ZERO));
			BigDecimal currentBudgetAmount = budgetItem == null ? zero(po.getApprovedBudgetAmount()) : zero(budgetItem.getApprovedBudgetAmount()).add(changesByBudget.getOrDefault(po.getBudgetId(), BigDecimal.ZERO));
			List<ProgressClaim> poClaims = claimsByPo.getOrDefault(po.getPoId(), List.of());
			BigDecimal approvedClaimTotal = poClaims.stream().filter(claim -> claim.getApprovalStatus() == ApprovalStatus.APPROVED).map(claim -> zero(claim.getClaimAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal actualPaidTotal = poClaims.stream().flatMap(claim -> paymentsByClaim.getOrDefault(claim.getClaimId(), List.of()).stream()).map(payment -> zero(payment.getPaidAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
			return new Item(
					po.getPoId(), po.getRequisitionId(), po.getApprovalId(), po.getSelectedComparisonId(), po.getBudgetId(),
					budgetItem == null ? "-" : budgetItem.getCategoryName(), budgetItem == null ? "-" : budgetItem.getItemName(),
					po.getVendorName(), po.getInitialPoAmount(), currentPoAmount, currentBudgetAmount, currentPoAmount.subtract(currentBudgetAmount),
					po.getSelectionReason(), po.getConstructionPeriod(), po.getScopeAndNotes(), po.getPaymentTerms(), po.getEstimateFileUrl(),
					po.getApprovedAt(), po.getApprover(), po.getApprovalComment(), approvedClaimTotal, actualPaidTotal, currentPoAmount.subtract(actualPaidTotal)
			);
		}).toList());
	}

	private static BigDecimal zero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
