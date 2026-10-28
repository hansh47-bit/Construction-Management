package com.construction.management.project.service;

import com.construction.management.project.api.ChangeOrderResponse;
import com.construction.management.project.api.ChangeOrderCreateRequest;
import com.construction.management.project.api.ContractChangeRequest;
import com.construction.management.project.api.BudgetChangeProposalRequest;
import com.construction.management.project.api.ExecutiveCorrectionRequest;
import com.construction.management.project.domain.ActualPayment;
import com.construction.management.project.domain.Approval;
import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.BudgetChange;
import com.construction.management.project.domain.ChangeOrder;
import com.construction.management.project.domain.ContractChange;
import com.construction.management.project.domain.CustomerContractItem;
import com.construction.management.project.domain.PoChange;
import com.construction.management.project.domain.ProgressClaim;
import com.construction.management.project.domain.Project;
import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import com.construction.management.project.domain.PurchaseOrder;
import com.construction.management.project.repository.ActualPaymentRepository;
import com.construction.management.project.repository.ApprovalRepository;
import com.construction.management.project.repository.BudgetChangeRepository;
import com.construction.management.project.repository.ChangeOrderRepository;
import com.construction.management.project.repository.ContractChangeRepository;
import com.construction.management.project.repository.CustomerContractItemRepository;
import com.construction.management.project.repository.PoChangeRepository;
import com.construction.management.project.repository.ProgressClaimRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetItemRepository;
import com.construction.management.project.repository.ProjectRepository;
import com.construction.management.project.repository.PurchaseOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records contract, budget and purchase-order changes as immutable deltas.
 * A decrease may never lower a PO below approved progress claims or actual payments.
 */
@Service
@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROJECT_READ')")
public class ChangeManagementService {
	@org.springframework.beans.factory.annotation.Autowired private com.construction.management.auth.BusinessPolicy businessPolicy;

	private final ProjectRepository projectRepository;
	private final CustomerContractItemRepository customerContractItemRepository;
	private final ProjectExecutionBudgetItemRepository budgetItemRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final ChangeOrderRepository changeOrderRepository;
	private final ContractChangeRepository contractChangeRepository;
	private final BudgetChangeRepository budgetChangeRepository;
	private final PoChangeRepository poChangeRepository;
	private final ProgressClaimRepository progressClaimRepository;
	private final ActualPaymentRepository actualPaymentRepository;
	private final ApprovalRepository approvalRepository;
	private final ProjectSettlementService projectSettlementService;

	public ChangeManagementService(
			ProjectRepository projectRepository,
			CustomerContractItemRepository customerContractItemRepository,
			ProjectExecutionBudgetItemRepository budgetItemRepository,
			PurchaseOrderRepository purchaseOrderRepository,
			ChangeOrderRepository changeOrderRepository,
			ContractChangeRepository contractChangeRepository,
			BudgetChangeRepository budgetChangeRepository,
			PoChangeRepository poChangeRepository,
			ProgressClaimRepository progressClaimRepository,
			ActualPaymentRepository actualPaymentRepository,
			ApprovalRepository approvalRepository,
			ProjectSettlementService projectSettlementService
	) {
		this.projectRepository = projectRepository;
		this.customerContractItemRepository = customerContractItemRepository;
		this.budgetItemRepository = budgetItemRepository;
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.changeOrderRepository = changeOrderRepository;
		this.contractChangeRepository = contractChangeRepository;
		this.budgetChangeRepository = budgetChangeRepository;
		this.poChangeRepository = poChangeRepository;
		this.progressClaimRepository = progressClaimRepository;
		this.actualPaymentRepository = actualPaymentRepository;
		this.approvalRepository = approvalRepository;
		this.projectSettlementService = projectSettlementService;
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_WRITE')")
	@com.construction.management.auth.BusinessAction
	public ChangeOrderResponse createChangeOrder(String projectId, ChangeOrderCreateRequest request) {
		projectSettlementService.assertNotSettled(projectId);
		projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));
		boolean contractChangeEnabled = Boolean.TRUE.equals(request.contractChangeEnabled());
		boolean budgetChangeEnabled = Boolean.TRUE.equals(request.budgetChangeEnabled());
		if (!contractChangeEnabled && !budgetChangeEnabled) {
			throw new IllegalArgumentException("At least one change scope must be selected.");
		}
		ChangeOrder order = changeOrderRepository.save(new ChangeOrder(
				nextChangeCode(), projectId, request.title(), request.reason(),
				com.construction.management.auth.CurrentActor.name(),
				nextRevisionNumber(projectId), contractChangeEnabled, budgetChangeEnabled));
		return response(order, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, null, List.of());
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_WRITE')")
	@com.construction.management.auth.BusinessAction
	public ChangeOrderResponse saveContractProposal(String projectId, String changeOrderId, ContractChangeRequest request) {
		businessPolicy.ownEntity(com.construction.management.project.domain.ChangeOrder.class, changeOrderId);
		ChangeOrder order = requiredOrder(projectId, changeOrderId);
		ensureDraft(order);
		if (!order.hasContractChange()) throw new IllegalStateException("This revision does not include a contract change.");
		if (request.changeAmount().signum() == 0) throw new IllegalArgumentException("계약 증감액은 0원이 될 수 없습니다.");
		if (!contractChangeRepository.findByChangeCode(changeOrderId).isEmpty()) throw new IllegalStateException("계약 변경안은 변경 건마다 한 번만 작성할 수 있습니다.");
		contractChangeRepository.save(new ContractChange(id(), projectId, changeOrderId, request.changeAmount(), request.reason(), ApprovalStatus.DRAFT));
		return responseFor(projectId, order);
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_WRITE')")
	@com.construction.management.auth.BusinessAction
	public ChangeOrderResponse saveBudgetProposal(String projectId, String changeOrderId, BudgetChangeProposalRequest request) {
		businessPolicy.ownEntity(com.construction.management.project.domain.ChangeOrder.class, changeOrderId);
		ChangeOrder order = requiredOrder(projectId, changeOrderId);
		ensureDraft(order);
		if (!order.hasBudgetChange()) throw new IllegalStateException("This revision does not include an execution-budget change.");
		if (!order.hasContractChange()) return saveBudgetOnlyProposal(projectId, changeOrderId, request, order);
		if (contractChangeRepository.findByChangeCode(changeOrderId).isEmpty()) throw new IllegalStateException("계약 변경안을 먼저 작성해야 실행예산 변경안을 만들 수 있습니다.");
		if (!budgetChangeRepository.findByChangeCode(changeOrderId).isEmpty()) throw new IllegalStateException("실행예산 변경안은 변경 건마다 한 번만 작성할 수 있습니다.");
		Map<String, ProjectExecutionBudgetItem> budgetItems = budgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId).stream()
				.collect(Collectors.toMap(ProjectExecutionBudgetItem::getItemId, Function.identity()));
		for (BudgetChangeProposalRequest.Item item : request.items()) {
			ProjectExecutionBudgetItem budgetItem = budgetItems.get(item.budgetItemId());
			if (budgetItem == null || !budgetItem.isApproved()) throw new IllegalArgumentException("승인된 프로젝트 실행예산 항목이 아닙니다: " + item.budgetItemId());
			budgetChangeRepository.save(new BudgetChange(id(), item.budgetItemId(), changeOrderId, item.changeAmount(), ApprovalStatus.DRAFT));
		}
		createPurchaseOrderProposals(changeOrderId, purchaseOrderRepository.findByProjectId(projectId), budgetChangeRepository.findByChangeCode(changeOrderId));
		return responseFor(projectId, order);
	}

	private ChangeOrderResponse saveBudgetOnlyProposal(String projectId, String changeOrderId, BudgetChangeProposalRequest request, ChangeOrder order) {
		if (request.items() == null || request.items().isEmpty()) throw new IllegalArgumentException("At least one execution-budget item must be changed.");
		if (!budgetChangeRepository.findByChangeCode(changeOrderId).isEmpty()) throw new IllegalStateException("Only one execution-budget proposal may be created for a revision.");
		Map<String, ProjectExecutionBudgetItem> budgetItems = budgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId).stream()
				.collect(Collectors.toMap(ProjectExecutionBudgetItem::getItemId, Function.identity()));
		for (BudgetChangeProposalRequest.Item item : request.items()) {
			ProjectExecutionBudgetItem budgetItem = budgetItems.get(item.budgetItemId());
			if (budgetItem == null || !budgetItem.isApproved()) throw new IllegalArgumentException("Execution-budget item is not approved: " + item.budgetItemId());
			if (item.changeAmount() == null || item.changeAmount().signum() == 0) throw new IllegalArgumentException("Execution-budget change amount must not be zero.");
			budgetChangeRepository.save(new BudgetChange(id(), item.budgetItemId(), changeOrderId, item.changeAmount(), ApprovalStatus.DRAFT));
		}
		createPurchaseOrderProposals(changeOrderId, purchaseOrderRepository.findByProjectId(projectId), budgetChangeRepository.findByChangeCode(changeOrderId));
		return responseFor(projectId, order);
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_REQUEST')")
	@com.construction.management.auth.BusinessAction
	public ChangeOrderResponse requestApproval(String projectId, String changeOrderId) {
		businessPolicy.ownEntity(com.construction.management.project.domain.ChangeOrder.class, changeOrderId);
		ChangeOrder order = requiredOrder(projectId, changeOrderId);
		ensureDraft(order);
		List<ContractChange> contractChanges = contractChangeRepository.findByChangeCode(changeOrderId);
		List<BudgetChange> budgetChanges = budgetChangeRepository.findByChangeCode(changeOrderId);
		List<PoChange> poChanges = poChangeRepository.findByChangeCode(changeOrderId);
		if (!order.hasContractChange()) return requestBudgetOnlyApproval(projectId, changeOrderId, order, budgetChanges, poChanges);
		if (!order.hasBudgetChange()) return requestContractOnlyApproval(projectId, changeOrderId, order, contractChanges);
		if (contractChanges.size() != 1 || budgetChanges.isEmpty()) throw new IllegalStateException("계약 변경안과 실행예산 변경안을 모두 작성한 후 결재 요청할 수 있습니다.");
		List<String> conflicts = reconciliationConflicts(purchaseOrderRepository.findByProjectId(projectId), poChanges.stream().collect(Collectors.toMap(PoChange::getPoId, PoChange::getChangeAmount)));
		if (!conflicts.isEmpty()) {
			order.markReconciliationRequired();
			changeOrderRepository.save(order);
			return responseFor(projectId, order, String.join(" ", conflicts));
		}
		createApproval(projectId, changeOrderId, "CONTRACT_CHANGE", "고객 계약 변경 결재", contractChanges.get(0).getChangeAmount(), currentContractAmount(projectId), order.getReason());
		createApproval(projectId, changeOrderId, "BUDGET_CHANGE", "실행예산 변경 결재", sumBudgetChanges(budgetChanges), currentExecutionBudget(projectId), order.getReason());
		if (!poChanges.isEmpty()) createApproval(projectId, changeOrderId, "PO_CHANGE", "발주 변경 결재", sumPoChanges(poChanges), currentPurchaseAmount(projectId), order.getReason());
		order.markRequested();
		changeOrderRepository.save(order);
		return responseFor(projectId, order);
	}

	private ChangeOrderResponse requestContractOnlyApproval(String projectId, String changeOrderId, ChangeOrder order, List<ContractChange> contractChanges) {
		if (contractChanges.size() != 1) throw new IllegalStateException("Contract change details must be completed before requesting approval.");
		createApproval(projectId, changeOrderId, "CONTRACT_CHANGE", "Contract change approval", contractChanges.get(0).getChangeAmount(), currentContractAmount(projectId), order.getReason());
		order.markRequested();
		changeOrderRepository.save(order);
		return responseFor(projectId, order);
	}

	private ChangeOrderResponse requestBudgetOnlyApproval(String projectId, String changeOrderId, ChangeOrder order, List<BudgetChange> budgetChanges, List<PoChange> poChanges) {
		if (budgetChanges.isEmpty()) throw new IllegalStateException("Execution-budget change details must be completed before requesting approval.");
		List<String> conflicts = reconciliationConflicts(purchaseOrderRepository.findByProjectId(projectId), poChanges.stream().collect(Collectors.toMap(PoChange::getPoId, PoChange::getChangeAmount)));
		if (!conflicts.isEmpty()) {
			order.markReconciliationRequired();
			changeOrderRepository.save(order);
			return responseFor(projectId, order, String.join(" ", conflicts));
		}
		createApproval(projectId, changeOrderId, "BUDGET_CHANGE", "Execution-budget change approval", sumBudgetChanges(budgetChanges), currentExecutionBudget(projectId), order.getReason());
		if (!poChanges.isEmpty()) createApproval(projectId, changeOrderId, "PO_CHANGE", "Purchase-order change approval", sumPoChanges(poChanges), currentPurchaseAmount(projectId), order.getReason());
		order.markRequested();
		changeOrderRepository.save(order);
		return responseFor(projectId, order);
	}

	@Transactional(readOnly = true)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_READ')")
	public ChangeOrderResponse getImpactSummary(String projectId, String changeOrderId) {
		return responseFor(projectId, requiredOrder(projectId, changeOrderId));
	}

	@Transactional(readOnly = true)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_READ')")
	public ChangeOrderResponse getReconciliation(String projectId, String changeOrderId) {
		ChangeOrder order = requiredOrder(projectId, changeOrderId);
		List<PoChange> changes = poChangeRepository.findByChangeCode(changeOrderId);
		List<String> conflicts = reconciliationConflicts(purchaseOrderRepository.findByProjectId(projectId), changes.stream().collect(Collectors.toMap(PoChange::getPoId, PoChange::getChangeAmount)));
		return responseFor(projectId, order, conflicts.isEmpty() ? null : String.join(" ", conflicts));
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('APPROVAL_DECIDE')")
	public void approveChangeApproval(Approval approval) {
		String code = approval.getTargetId();
		if ("CONTRACT_CHANGE".equals(approval.getApprovalType())) contractChangeRepository.findByChangeCode(code).forEach(change -> { change.approve(); contractChangeRepository.save(change); });
		if ("BUDGET_CHANGE".equals(approval.getApprovalType())) budgetChangeRepository.findByChangeCode(code).forEach(change -> { change.approve(); budgetChangeRepository.save(change); });
		if ("PO_CHANGE".equals(approval.getApprovalType())) poChangeRepository.findByChangeCode(code).forEach(change -> { change.approve(); poChangeRepository.save(change); });
		refreshStatus(approval.getProjectId(), code);
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('APPROVAL_DECIDE')")
	public void rejectChangeApproval(Approval approval) {
		String code = approval.getTargetId();
		if ("CONTRACT_CHANGE".equals(approval.getApprovalType())) contractChangeRepository.findByChangeCode(code).forEach(change -> { change.reject(); contractChangeRepository.save(change); });
		if ("BUDGET_CHANGE".equals(approval.getApprovalType())) budgetChangeRepository.findByChangeCode(code).forEach(change -> { change.reject(); budgetChangeRepository.save(change); });
		if ("PO_CHANGE".equals(approval.getApprovalType())) poChangeRepository.findByChangeCode(code).forEach(change -> { change.reject(); poChangeRepository.save(change); });
		refreshStatus(approval.getProjectId(), code);
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('EXECUTIVE_CORRECT')")
	@com.construction.management.auth.BusinessAction
	public ChangeOrderResponse correctByExecutive(String projectId, ExecutiveCorrectionRequest request) {
		projectSettlementService.assertNotSettled(projectId);
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));
		BigDecimal currentContract = currentContractAmount(projectId);
		BigDecimal contractDelta = zero(request.targetContractSupplyAmount()).subtract(currentContract);
		if (contractDelta.signum() == 0) throw new IllegalArgumentException("Current contract amount is unchanged.");

		String changeCode = nextChangeCode();
		ChangeOrder order = changeOrderRepository.save(new ChangeOrder(changeCode, projectId, "Contract change", request.reason(), com.construction.management.auth.CurrentActor.name()));
		List<ProjectExecutionBudgetItem> budgetItems = Boolean.FALSE.equals(request.applyExecutionBudget())
				? List.of()
				: budgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId).stream().filter(ProjectExecutionBudgetItem::isApproved).toList();
		BigDecimal budgetDelta = budgetItems.isEmpty() ? BigDecimal.ZERO : contractDelta
				.multiply(BigDecimal.ONE.subtract(zero(project.getTargetProfitRate()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
				.setScale(2, RoundingMode.HALF_UP);
		Map<String, BigDecimal> budgetDeltas = allocate(budgetItems, ProjectExecutionBudgetItem::getApprovedBudgetAmount, budgetDelta);

		List<PurchaseOrder> purchaseOrders = Boolean.FALSE.equals(request.applyPurchaseOrders()) ? List.of() : purchaseOrderRepository.findByProjectId(projectId);
		Map<String, List<PurchaseOrder>> ordersByBudget = purchaseOrders.stream().collect(Collectors.groupingBy(PurchaseOrder::getBudgetId));
		Map<String, BigDecimal> poDeltas = new java.util.LinkedHashMap<>();
		for (ProjectExecutionBudgetItem item : budgetItems) {
			List<PurchaseOrder> linkedOrders = ordersByBudget.getOrDefault(item.getItemId(), List.of());
			if (!linkedOrders.isEmpty()) poDeltas.putAll(allocate(linkedOrders, PurchaseOrder::getInitialPoAmount, budgetDeltas.getOrDefault(item.getItemId(), BigDecimal.ZERO)));
		}
		BigDecimal poDelta = poDeltas.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		List<ChangeOrderResponse.Impact> impacts = impacts(projectId, contractDelta, budgetItems, budgetDeltas, purchaseOrders, poDeltas);
		List<String> conflicts = reconciliationConflicts(purchaseOrders, poDeltas);

		if (!conflicts.isEmpty()) {
			order.markReconciliationRequired();
			changeOrderRepository.save(order);
			persistPlannedChanges(projectId, changeCode, request.reason(), contractDelta, budgetDeltas, poDeltas);
			return response(order, contractDelta, budgetDelta, poDelta, true, String.join(" ", conflicts), impacts);
		}

		contractChangeRepository.save(new ContractChange(id(), projectId, changeCode, contractDelta, request.reason(), ApprovalStatus.APPROVED));
		budgetDeltas.forEach((budgetId, delta) -> budgetChangeRepository.save(new BudgetChange(id(), budgetId, changeCode, delta, ApprovalStatus.APPROVED)));
		poDeltas.forEach((poId, delta) -> poChangeRepository.save(new PoChange(id(), poId, changeCode, delta, ApprovalStatus.APPROVED)));
		order.markApproved();
		changeOrderRepository.save(order);
		return response(order, contractDelta, budgetDelta, poDelta, false, null, impacts);
	}

	@Transactional(readOnly = true)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_READ')")
	public List<ChangeOrderResponse> getHistory(String projectId) {
		List<ProjectExecutionBudgetItem> budgetItems = budgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId);
		Map<String, ProjectExecutionBudgetItem> budgetById = budgetItems.stream().collect(Collectors.toMap(ProjectExecutionBudgetItem::getItemId, Function.identity()));
		List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findByProjectId(projectId);
		Map<String, PurchaseOrder> poById = purchaseOrders.stream().collect(Collectors.toMap(PurchaseOrder::getPoId, Function.identity()));
		List<ContractChange> contractChanges = contractChangeRepository.findByProjectId(projectId);
		List<BudgetChange> budgetChanges = budgetItems.isEmpty() ? List.of() : budgetChangeRepository.findByBudgetIdIn(budgetItems.stream().map(ProjectExecutionBudgetItem::getItemId).toList());
		List<PoChange> poChanges = purchaseOrders.isEmpty() ? List.of() : poChangeRepository.findByPoIdIn(purchaseOrders.stream().map(PurchaseOrder::getPoId).toList());

		return changeOrderRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(order -> {
			List<ChangeOrderResponse.Impact> impacts = new ArrayList<>();
			contractChanges.stream().filter(change -> order.getChangeOrderId().equals(change.getChangeCode()))
					.forEach(change -> impacts.add(new ChangeOrderResponse.Impact("CONTRACT", projectId, "Customer contract", change.getChangeAmount())));
			budgetChanges.stream().filter(change -> order.getChangeOrderId().equals(change.getChangeCode())).forEach(change -> {
				ProjectExecutionBudgetItem item = budgetById.get(change.getBudgetId());
				impacts.add(new ChangeOrderResponse.Impact("BUDGET", change.getBudgetId(), item == null ? change.getBudgetId() : item.getCategoryName() + " / " + item.getItemName(), change.getChangeAmount()));
			});
			poChanges.stream().filter(change -> order.getChangeOrderId().equals(change.getChangeCode())).forEach(change -> {
				PurchaseOrder po = poById.get(change.getPoId());
				impacts.add(new ChangeOrderResponse.Impact("PURCHASE_ORDER", change.getPoId(), po == null ? change.getPoId() : po.getVendorName(), change.getChangeAmount()));
			});
			BigDecimal contractDelta = impacts.stream().filter(impact -> "CONTRACT".equals(impact.category())).map(ChangeOrderResponse.Impact::deltaAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal budgetDelta = impacts.stream().filter(impact -> "BUDGET".equals(impact.category())).map(ChangeOrderResponse.Impact::deltaAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal poDelta = impacts.stream().filter(impact -> "PURCHASE_ORDER".equals(impact.category())).map(ChangeOrderResponse.Impact::deltaAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			boolean reconciliationRequired = "RECONCILIATION_REQUIRED".equals(order.getStatus());
			return response(order, contractDelta, budgetDelta, poDelta, reconciliationRequired,
					reconciliationRequired ? "Approved progress claims or payments must be reconciled before this change can be applied." : null, impacts);
		}).toList();
	}

	private void persistPlannedChanges(String projectId, String changeCode, String reason, BigDecimal contractDelta, Map<String, BigDecimal> budgetDeltas, Map<String, BigDecimal> poDeltas) {
		contractChangeRepository.save(new ContractChange(id(), projectId, changeCode, contractDelta, reason, ApprovalStatus.DRAFT));
		budgetDeltas.forEach((budgetId, delta) -> budgetChangeRepository.save(new BudgetChange(id(), budgetId, changeCode, delta, ApprovalStatus.DRAFT)));
		poDeltas.forEach((poId, delta) -> poChangeRepository.save(new PoChange(id(), poId, changeCode, delta, ApprovalStatus.DRAFT)));
	}

	private List<String> reconciliationConflicts(List<PurchaseOrder> purchaseOrders, Map<String, BigDecimal> poDeltas) {
		if (poDeltas.isEmpty()) return List.of();
		List<String> poIds = purchaseOrders.stream().map(PurchaseOrder::getPoId).toList();
		Map<String, BigDecimal> approvedPoChanges = poChangeRepository.findByPoIdInAndApprovalStatus(poIds, ApprovalStatus.APPROVED).stream()
				.collect(Collectors.groupingBy(PoChange::getPoId, Collectors.mapping(PoChange::getChangeAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
		Map<String, List<ProgressClaim>> claimsByPo = progressClaimRepository.findByPoIdIn(poIds).stream().collect(Collectors.groupingBy(ProgressClaim::getPoId));
		List<String> claimIds = claimsByPo.values().stream().flatMap(List::stream).map(ProgressClaim::getClaimId).toList();
		Map<String, BigDecimal> paidByClaim = claimIds.isEmpty() ? Map.of() : actualPaymentRepository.findByClaimIdIn(claimIds).stream()
				.collect(Collectors.groupingBy(ActualPayment::getClaimId, Collectors.mapping(ActualPayment::getPaidAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
		List<String> conflicts = new ArrayList<>();
		for (PurchaseOrder po : purchaseOrders) {
			if (!poDeltas.containsKey(po.getPoId())) continue;
			List<ProgressClaim> claims = claimsByPo.getOrDefault(po.getPoId(), List.of());
			BigDecimal approvedClaims = claims.stream().filter(claim -> claim.getApprovalStatus() == ApprovalStatus.APPROVED).map(ProgressClaim::getClaimAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal paid = claims.stream().map(ProgressClaim::getClaimId).map(id -> paidByClaim.getOrDefault(id, BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal proposed = zero(po.getInitialPoAmount()).add(approvedPoChanges.getOrDefault(po.getPoId(), BigDecimal.ZERO)).add(poDeltas.get(po.getPoId()));
			BigDecimal committed = approvedClaims.max(paid);
			if (proposed.compareTo(committed) < 0) conflicts.add(po.getPoId() + " change amount is below approved progress/payment total (" + committed + ").");
		}
		return conflicts;
	}

	private List<ChangeOrderResponse.Impact> impacts(String projectId, BigDecimal contractDelta, List<ProjectExecutionBudgetItem> budgetItems, Map<String, BigDecimal> budgetDeltas, List<PurchaseOrder> purchaseOrders, Map<String, BigDecimal> poDeltas) {
		List<ChangeOrderResponse.Impact> impacts = new ArrayList<>();
		impacts.add(new ChangeOrderResponse.Impact("CONTRACT", projectId, "Customer contract", contractDelta));
		budgetItems.forEach(item -> impacts.add(new ChangeOrderResponse.Impact("BUDGET", item.getItemId(), item.getCategoryName() + " / " + item.getItemName(), budgetDeltas.getOrDefault(item.getItemId(), BigDecimal.ZERO))));
		Map<String, PurchaseOrder> poById = purchaseOrders.stream().collect(Collectors.toMap(PurchaseOrder::getPoId, Function.identity()));
		poDeltas.forEach((poId, delta) -> impacts.add(new ChangeOrderResponse.Impact("PURCHASE_ORDER", poId, poById.get(poId).getVendorName(), delta)));
		return impacts;
	}

	private ChangeOrderResponse response(ChangeOrder order, BigDecimal contractDelta, BigDecimal budgetDelta, BigDecimal poDelta, boolean requiresReconciliation, String message, List<ChangeOrderResponse.Impact> impacts) {
		return new ChangeOrderResponse(order.getChangeOrderId(), order.getProjectId(), order.getTitle(), order.getReason(), order.getCreatedUserId()==null ? "기존 기록 · "+order.getRequestedBy() : order.getRequestedBy(), order.getStatus(), order.getCreatedAt(), order.getRevisionNumber(), order.hasContractChange(), order.hasBudgetChange(), contractDelta, budgetDelta, poDelta, requiresReconciliation, message, impacts);
	}

	private ChangeOrder requiredOrder(String projectId, String changeOrderId) {
		ChangeOrder order = changeOrderRepository.findById(changeOrderId).orElseThrow(() -> new EntityNotFoundException("변경 건을 찾을 수 없습니다: " + changeOrderId));
		if (!projectId.equals(order.getProjectId())) throw new IllegalArgumentException("다른 프로젝트의 변경 건입니다.");
		return order;
	}

	private static void ensureDraft(ChangeOrder order) {
		if (!"DRAFT".equals(order.getStatus())) throw new IllegalStateException("작성 중인 변경 건만 수정할 수 있습니다.");
	}

	private void createPurchaseOrderProposals(String changeCode, List<PurchaseOrder> orders, List<BudgetChange> budgetChanges) {
		Map<String, List<PurchaseOrder>> byBudget = orders.stream().collect(Collectors.groupingBy(PurchaseOrder::getBudgetId));
		for (BudgetChange budgetChange : budgetChanges) {
			List<PurchaseOrder> linked = byBudget.getOrDefault(budgetChange.getBudgetId(), List.of());
			if (linked.isEmpty()) continue;
			allocate(linked, PurchaseOrder::getInitialPoAmount, budgetChange.getChangeAmount()).forEach((poId, amount) ->
					poChangeRepository.save(new PoChange(id(), poId, changeCode, amount, ApprovalStatus.DRAFT)));
		}
	}

	private void createApproval(String projectId, String changeCode, String type, String title, BigDecimal delta, BigDecimal previous, String reason) {
		Approval approval = new Approval(id(), projectId, type, changeCode, changeCode + " " + title,
				previous.add(delta), previous, delta, com.construction.management.auth.CurrentActor.name(), reason);
		approvalRepository.save(approval);
		if ("CONTRACT_CHANGE".equals(type)) contractChangeRepository.findByChangeCode(changeCode).forEach(change -> { change.linkApproval(approval.getApprovalId()); contractChangeRepository.save(change); });
		if ("BUDGET_CHANGE".equals(type)) budgetChangeRepository.findByChangeCode(changeCode).forEach(change -> { change.linkApproval(approval.getApprovalId()); budgetChangeRepository.save(change); });
		if ("PO_CHANGE".equals(type)) poChangeRepository.findByChangeCode(changeCode).forEach(change -> { change.linkApproval(approval.getApprovalId()); poChangeRepository.save(change); });
	}

	private void refreshStatus(String projectId, String changeCode) {
		ChangeOrder order = requiredOrder(projectId, changeCode);
		List<ContractChange> contractChanges = contractChangeRepository.findByChangeCode(changeCode);
		List<BudgetChange> budgetChanges = budgetChangeRepository.findByChangeCode(changeCode);
		List<PoChange> poChanges = poChangeRepository.findByChangeCode(changeCode);
		List<ApprovalStatus> statuses = new ArrayList<>();
		contractChanges.forEach(change -> statuses.add(change.getApprovalStatus()));
		budgetChanges.forEach(change -> statuses.add(change.getApprovalStatus()));
		poChanges.forEach(change -> statuses.add(change.getApprovalStatus()));
		if (statuses.stream().anyMatch(status -> status == ApprovalStatus.REJECTED)) order.markRejected();
		else if (statuses.stream().allMatch(status -> status == ApprovalStatus.APPROVED)) order.markApproved();
		else if (statuses.stream().anyMatch(status -> status == ApprovalStatus.APPROVED)) order.markPartiallyApproved();
		else order.markRequested();
		changeOrderRepository.save(order);
	}

	private ChangeOrderResponse responseFor(String projectId, ChangeOrder order) { return responseFor(projectId, order, null); }

	private ChangeOrderResponse responseFor(String projectId, ChangeOrder order, String reconciliationMessage) {
		List<ProjectExecutionBudgetItem> budgetItems = budgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId);
		Map<String, ProjectExecutionBudgetItem> budgetById = budgetItems.stream().collect(Collectors.toMap(ProjectExecutionBudgetItem::getItemId, Function.identity()));
		List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findByProjectId(projectId);
		Map<String, PurchaseOrder> poById = purchaseOrders.stream().collect(Collectors.toMap(PurchaseOrder::getPoId, Function.identity()));
		List<ChangeOrderResponse.Impact> impacts = new ArrayList<>();
		List<ContractChange> contractChanges = contractChangeRepository.findByChangeCode(order.getChangeOrderId());
		List<BudgetChange> budgetChanges = budgetChangeRepository.findByChangeCode(order.getChangeOrderId());
		List<PoChange> poChanges = poChangeRepository.findByChangeCode(order.getChangeOrderId());
		contractChanges.forEach(change -> impacts.add(new ChangeOrderResponse.Impact("CONTRACT", projectId, "Customer contract", change.getChangeAmount())));
		budgetChanges.forEach(change -> {
			ProjectExecutionBudgetItem item = budgetById.get(change.getBudgetId());
			impacts.add(new ChangeOrderResponse.Impact("BUDGET", change.getBudgetId(), item == null ? change.getBudgetId() : item.getCategoryName() + " / " + item.getItemName(), change.getChangeAmount()));
		});
		poChanges.forEach(change -> {
			PurchaseOrder po = poById.get(change.getPoId());
			impacts.add(new ChangeOrderResponse.Impact("PURCHASE_ORDER", change.getPoId(), po == null ? change.getPoId() : po.getVendorName(), change.getChangeAmount()));
		});
		return response(order, sumContractChanges(contractChanges), sumBudgetChanges(budgetChanges), sumPoChanges(poChanges),
				"RECONCILIATION_REQUIRED".equals(order.getStatus()), reconciliationMessage, impacts);
	}

	private BigDecimal currentExecutionBudget(String projectId) {
		List<ProjectExecutionBudgetItem> items = budgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId);
		List<String> ids = items.stream().map(ProjectExecutionBudgetItem::getItemId).toList();
		Map<String, BigDecimal> changes = ids.isEmpty() ? Map.of() : budgetChangeRepository.findByBudgetIdInAndApprovalStatus(ids, ApprovalStatus.APPROVED).stream()
				.collect(Collectors.groupingBy(BudgetChange::getBudgetId, Collectors.mapping(BudgetChange::getChangeAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
		return items.stream().map(item -> zero(item.getApprovedBudgetAmount()).add(changes.getOrDefault(item.getItemId(), BigDecimal.ZERO))).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal currentPurchaseAmount(String projectId) {
		List<PurchaseOrder> orders = purchaseOrderRepository.findByProjectId(projectId);
		List<String> ids = orders.stream().map(PurchaseOrder::getPoId).toList();
		Map<String, BigDecimal> changes = ids.isEmpty() ? Map.of() : poChangeRepository.findByPoIdInAndApprovalStatus(ids, ApprovalStatus.APPROVED).stream()
				.collect(Collectors.groupingBy(PoChange::getPoId, Collectors.mapping(PoChange::getChangeAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
		return orders.stream().map(order -> zero(order.getInitialPoAmount()).add(changes.getOrDefault(order.getPoId(), BigDecimal.ZERO))).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static BigDecimal sumContractChanges(List<ContractChange> changes) { return changes.stream().map(ContractChange::getChangeAmount).map(ChangeManagementService::zero).reduce(BigDecimal.ZERO, BigDecimal::add); }
	private static BigDecimal sumBudgetChanges(List<BudgetChange> changes) { return changes.stream().map(BudgetChange::getChangeAmount).map(ChangeManagementService::zero).reduce(BigDecimal.ZERO, BigDecimal::add); }
	private static BigDecimal sumPoChanges(List<PoChange> changes) { return changes.stream().map(PoChange::getChangeAmount).map(ChangeManagementService::zero).reduce(BigDecimal.ZERO, BigDecimal::add); }

	private BigDecimal currentContractAmount(String projectId) {
		BigDecimal initial = customerContractItemRepository.findByProjectIdOrderByWorkCategoryAsc(projectId).stream().map(CustomerContractItem::getContractAmount).map(ChangeManagementService::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal changes = contractChangeRepository.findByProjectIdAndApprovalStatus(projectId, ApprovalStatus.APPROVED).stream().map(ContractChange::getChangeAmount).map(ChangeManagementService::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
		return initial.add(changes);
	}

	private String nextChangeCode() {
		String prefix = "CHG-" + Year.now().getValue() + "-";
		int seq = changeOrderRepository.findByChangeOrderIdStartingWithOrderByChangeOrderIdDesc(prefix).stream().map(ChangeOrder::getChangeOrderId).map(code -> code.substring(prefix.length())).mapToInt(value -> { try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return 0; } }).max().orElse(0) + 1;
		return "%s%03d".formatted(prefix, seq);
	}

	private int nextRevisionNumber(String projectId) {
		return changeOrderRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
				.map(ChangeOrder::getRevisionNumber)
				.filter(java.util.Objects::nonNull)
				.mapToInt(Integer::intValue)
				.max()
				.orElse(1) + 1;
	}

	private static <T> Map<String, BigDecimal> allocate(List<T> targets, Function<T, BigDecimal> amount, BigDecimal total) {
		BigDecimal base = targets.stream().map(amount).map(ChangeManagementService::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
		Map<String, BigDecimal> result = new java.util.LinkedHashMap<>();
		BigDecimal assigned = BigDecimal.ZERO;
		for (int i = 0; i < targets.size(); i++) {
			T target = targets.get(i);
			String key = target instanceof ProjectExecutionBudgetItem item ? item.getItemId() : ((PurchaseOrder) target).getPoId();
			BigDecimal value = i == targets.size() - 1 ? total.subtract(assigned) : (base.signum() == 0 ? BigDecimal.ZERO : total.multiply(zero(amount.apply(target))).divide(base, 2, RoundingMode.HALF_UP));
			result.put(key, value);
			assigned = assigned.add(value);
		}
		return result;
	}

	private static String id() { return "chg_" + UUID.randomUUID().toString().replace("-", ""); }
	private static BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
