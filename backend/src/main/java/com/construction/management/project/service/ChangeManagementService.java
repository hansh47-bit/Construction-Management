package com.construction.management.project.service;

import com.construction.management.project.api.ChangeOrderResponse;
import com.construction.management.project.api.ExecutiveCorrectionRequest;
import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.BudgetChange;
import com.construction.management.project.domain.ChangeOrder;
import com.construction.management.project.domain.ContractChange;
import com.construction.management.project.domain.CustomerContractItem;
import com.construction.management.project.domain.PoChange;
import com.construction.management.project.domain.Project;
import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import com.construction.management.project.domain.PurchaseOrder;
import com.construction.management.project.repository.BudgetChangeRepository;
import com.construction.management.project.repository.ChangeOrderRepository;
import com.construction.management.project.repository.ContractChangeRepository;
import com.construction.management.project.repository.CustomerContractItemRepository;
import com.construction.management.project.repository.PoChangeRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetItemRepository;
import com.construction.management.project.repository.ProjectRepository;
import com.construction.management.project.repository.PurchaseOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CEO corrections create approved deltas. Original contracts/budgets/POs are never overwritten. */
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

	public ChangeManagementService(ProjectRepository projectRepository, CustomerContractItemRepository customerContractItemRepository,
			ProjectExecutionBudgetItemRepository budgetItemRepository, PurchaseOrderRepository purchaseOrderRepository,
			ChangeOrderRepository changeOrderRepository, ContractChangeRepository contractChangeRepository,
			BudgetChangeRepository budgetChangeRepository, PoChangeRepository poChangeRepository) {
		this.projectRepository = projectRepository;
		this.customerContractItemRepository = customerContractItemRepository;
		this.budgetItemRepository = budgetItemRepository;
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.changeOrderRepository = changeOrderRepository;
		this.contractChangeRepository = contractChangeRepository;
		this.budgetChangeRepository = budgetChangeRepository;
		this.poChangeRepository = poChangeRepository;
	}

	@Transactional
	public ChangeOrderResponse correctByExecutive(String projectId, ExecutiveCorrectionRequest request) {
		Project project = projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
		BigDecimal currentContract = currentContractAmount(projectId);
		BigDecimal contractDelta = zero(request.targetContractSupplyAmount()).subtract(currentContract);
		if (contractDelta.compareTo(BigDecimal.ZERO) == 0) throw new IllegalArgumentException("현재 계약 공급가액과 동일합니다.");

		String changeCode = nextChangeCode();
		ChangeOrder order = changeOrderRepository.save(new ChangeOrder(changeCode, projectId, "대표이사 금액 정정", request.reason(), "대표이사"));
		contractChangeRepository.save(new ContractChange(id(), projectId, changeCode, contractDelta, request.reason(), ApprovalStatus.APPROVED));
		List<ChangeOrderResponse.Impact> impacts = new ArrayList<>();
		impacts.add(new ChangeOrderResponse.Impact("CONTRACT", projectId, "고객 계약 공급가액", contractDelta));

		BigDecimal budgetDelta = BigDecimal.ZERO;
		BigDecimal poDelta = BigDecimal.ZERO;
		if (!Boolean.FALSE.equals(request.applyExecutionBudget())) {
			List<ProjectExecutionBudgetItem> items = budgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId).stream()
					.filter(ProjectExecutionBudgetItem::isApproved).toList();
			if (!items.isEmpty()) {
				budgetDelta = contractDelta.multiply(BigDecimal.ONE.subtract(zero(project.getTargetProfitRate()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))).setScale(2, RoundingMode.HALF_UP);
				Map<String, BigDecimal> changes = allocate(items, ProjectExecutionBudgetItem::getApprovedBudgetAmount, budgetDelta);
				for (ProjectExecutionBudgetItem item : items) {
					BigDecimal delta = changes.get(item.getItemId());
					budgetChangeRepository.save(new BudgetChange(id(), item.getItemId(), changeCode, delta, ApprovalStatus.APPROVED));
					impacts.add(new ChangeOrderResponse.Impact("BUDGET", item.getItemId(), item.getCategoryName() + " · " + item.getItemName(), delta));
				}
				if (!Boolean.FALSE.equals(request.applyPurchaseOrders())) {
					List<PurchaseOrder> orders = purchaseOrderRepository.findByProjectId(projectId);
					for (ProjectExecutionBudgetItem item : items) {
						List<PurchaseOrder> linked = orders.stream().filter(po -> item.getItemId().equals(po.getBudgetId())).toList();
						if (linked.isEmpty()) continue;
						Map<String, BigDecimal> poChanges = allocate(linked, PurchaseOrder::getInitialPoAmount, changes.get(item.getItemId()));
						for (PurchaseOrder po : linked) {
							BigDecimal delta = poChanges.get(po.getPoId());
							poChangeRepository.save(new PoChange(id(), po.getPoId(), changeCode, delta, ApprovalStatus.APPROVED));
							impacts.add(new ChangeOrderResponse.Impact("PURCHASE_ORDER", po.getPoId(), po.getVendorName(), delta));
							poDelta = poDelta.add(delta);
						}
					}
				}
			}
		}
		return new ChangeOrderResponse(order.getChangeOrderId(), projectId, order.getTitle(), order.getReason(), order.getRequestedBy(), order.getStatus(), order.getCreatedAt(), contractDelta, budgetDelta, poDelta, impacts);
	}

	@Transactional(readOnly = true)
	public List<ChangeOrderResponse> getHistory(String projectId) {
		return changeOrderRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(order -> {
			List<ChangeOrderResponse.Impact> impacts = new ArrayList<>();
			BigDecimal contract = contractChangeRepository.findByProjectIdAndApprovalStatus(projectId, ApprovalStatus.APPROVED).stream()
					.filter(c -> order.getChangeOrderId().equals(c.getChangeCode())).map(ContractChange::getChangeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (contract.compareTo(BigDecimal.ZERO) != 0) impacts.add(new ChangeOrderResponse.Impact("CONTRACT", projectId, "고객 계약 공급가액", contract));
			List<ProjectExecutionBudgetItem> items = budgetItemRepository.findByProjectIdOrderByItemIdAsc(projectId);
			Map<String, ProjectExecutionBudgetItem> itemMap = items.stream().collect(Collectors.toMap(ProjectExecutionBudgetItem::getItemId, Function.identity()));
			List<BudgetChange> budgetChanges = items.isEmpty() ? List.of() : budgetChangeRepository.findByBudgetIdInAndApprovalStatus(items.stream().map(ProjectExecutionBudgetItem::getItemId).toList(), ApprovalStatus.APPROVED);
			budgetChanges.stream().filter(c -> order.getChangeOrderId().equals(c.getChangeCode())).forEach(c -> {
				ProjectExecutionBudgetItem item = itemMap.get(c.getBudgetId());
				impacts.add(new ChangeOrderResponse.Impact("BUDGET", c.getBudgetId(), item == null ? c.getBudgetId() : item.getCategoryName() + " · " + item.getItemName(), c.getChangeAmount()));
			});
			List<PurchaseOrder> pos = purchaseOrderRepository.findByProjectId(projectId);
			Map<String, PurchaseOrder> poMap = pos.stream().collect(Collectors.toMap(PurchaseOrder::getPoId, Function.identity()));
			List<PoChange> poChanges = pos.isEmpty() ? List.of() : poChangeRepository.findByPoIdInAndApprovalStatus(pos.stream().map(PurchaseOrder::getPoId).toList(), ApprovalStatus.APPROVED);
			poChanges.stream().filter(c -> order.getChangeOrderId().equals(c.getChangeCode())).forEach(c -> {
				PurchaseOrder po = poMap.get(c.getPoId());
				impacts.add(new ChangeOrderResponse.Impact("PURCHASE_ORDER", c.getPoId(), po == null ? c.getPoId() : po.getVendorName(), c.getChangeAmount()));
			});
			BigDecimal budget = impacts.stream().filter(i -> "BUDGET".equals(i.category())).map(ChangeOrderResponse.Impact::deltaAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal po = impacts.stream().filter(i -> "PURCHASE_ORDER".equals(i.category())).map(ChangeOrderResponse.Impact::deltaAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			return new ChangeOrderResponse(order.getChangeOrderId(), projectId, order.getTitle(), order.getReason(), order.getRequestedBy(), order.getStatus(), order.getCreatedAt(), contract, budget, po, impacts);
		}).toList();
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
			result.put(key, value); assigned = assigned.add(value);
		}
		return result;
	}

	private static String id() { return "chg_" + UUID.randomUUID().toString().replace("-", ""); }
	private static BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
