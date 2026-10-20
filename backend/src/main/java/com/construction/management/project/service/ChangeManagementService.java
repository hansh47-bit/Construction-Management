package com.construction.management.project.service;

import com.construction.management.project.api.ChangeOrderResponse;
import com.construction.management.project.api.ExecutiveCorrectionRequest;
import com.construction.management.project.domain.ActualPayment;
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
public class ChangeManagementService {
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
		this.projectSettlementService = projectSettlementService;
	}

	@Transactional
	public ChangeOrderResponse correctByExecutive(String projectId, ExecutiveCorrectionRequest request) {
		projectSettlementService.assertNotSettled(projectId);
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));
		BigDecimal currentContract = currentContractAmount(projectId);
		BigDecimal contractDelta = zero(request.targetContractSupplyAmount()).subtract(currentContract);
		if (contractDelta.signum() == 0) throw new IllegalArgumentException("Current contract amount is unchanged.");

		String changeCode = nextChangeCode();
		ChangeOrder order = changeOrderRepository.save(new ChangeOrder(changeCode, projectId, "Contract change", request.reason(), "executive"));
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
		return response(order, contractDelta, budgetDelta, poDelta, false, null, impacts);
	}

	@Transactional(readOnly = true)
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
		return new ChangeOrderResponse(order.getChangeOrderId(), order.getProjectId(), order.getTitle(), order.getReason(), order.getRequestedBy(), order.getStatus(), order.getCreatedAt(), contractDelta, budgetDelta, poDelta, requiresReconciliation, message, impacts);
	}

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
