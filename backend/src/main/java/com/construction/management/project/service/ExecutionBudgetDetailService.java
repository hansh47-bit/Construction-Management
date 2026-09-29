package com.construction.management.project.service;

import com.construction.management.project.api.ExecutionBudgetDetailRequest;
import com.construction.management.project.api.ExecutionBudgetDetailResponse;
import com.construction.management.project.api.ExecutionBudgetDetailResponse.Detail;
import com.construction.management.project.api.ExecutionBudgetDetailResponse.SummaryItem;
import com.construction.management.project.domain.CustomerContractItem;
import com.construction.management.project.domain.Project;
import com.construction.management.project.domain.ProjectExecutionBudgetDetail;
import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import com.construction.management.project.repository.CustomerContractItemRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetDetailRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetItemRepository;
import com.construction.management.project.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ExecutionBudgetDetailService {

	private final ProjectRepository projectRepository;
	private final CustomerContractItemRepository customerContractItemRepository;
	private final ProjectExecutionBudgetItemRepository itemRepository;
	private final ProjectExecutionBudgetDetailRepository detailRepository;

	public ExecutionBudgetDetailService(
			ProjectRepository projectRepository,
			CustomerContractItemRepository customerContractItemRepository,
			ProjectExecutionBudgetItemRepository itemRepository,
			ProjectExecutionBudgetDetailRepository detailRepository
	) {
		this.projectRepository = projectRepository;
		this.customerContractItemRepository = customerContractItemRepository;
		this.itemRepository = itemRepository;
		this.detailRepository = detailRepository;
	}

	@Transactional(readOnly = true)
	public ExecutionBudgetDetailResponse getDetails(String projectId) {
		Project project = getProject(projectId);
		return toResponse(project);
	}

	@Transactional
	public ExecutionBudgetDetailResponse saveDetails(String projectId, ExecutionBudgetDetailRequest request) {
		Project project = getProject(projectId);
		detailRepository.deleteByProjectId(projectId);

		List<ProjectExecutionBudgetDetail> details = request.details().stream()
				.filter(detail -> StringUtils.hasText(detail.budgetItemId()) && StringUtils.hasText(detail.categoryName()) && StringUtils.hasText(detail.itemName()))
				.map(detail -> new ProjectExecutionBudgetDetail(
						"dtl_" + UUID.randomUUID().toString().replace("-", ""),
						projectId,
						detail.budgetItemId(),
						detail.categoryName(),
						detail.itemName(),
						StringUtils.hasText(detail.costType()) ? detail.costType() : "외주비",
						detail.vendorDescription(),
						detail.unit(),
						detail.quantity(),
						detail.unitPrice(),
						detail.evidenceLink(),
						detail.note()
				))
				.toList();
		detailRepository.saveAll(details);
		rollup(projectId, details);

		return toResponse(project);
	}

	private void rollup(String projectId, List<ProjectExecutionBudgetDetail> details) {
		Map<String, List<ProjectExecutionBudgetDetail>> byItem = details.stream()
				.collect(Collectors.groupingBy(ProjectExecutionBudgetDetail::getBudgetItemId, LinkedHashMap::new, Collectors.toList()));

		for (Map.Entry<String, List<ProjectExecutionBudgetDetail>> entry : byItem.entrySet()) {
			ProjectExecutionBudgetDetail first = entry.getValue().get(0);
			ProjectExecutionBudgetItem item = itemRepository.findById(entry.getKey())
					.orElseGet(() -> new ProjectExecutionBudgetItem(entry.getKey(), projectId, first.getCategoryName(), first.getItemName()));
			item.updateInfo(first.getCategoryName(), first.getItemName());
			item.updateBudgetAmount(entry.getValue().stream()
					.map(ProjectExecutionBudgetDetail::getAmount)
					.map(ExecutionBudgetDetailService::defaultZero)
					.reduce(BigDecimal.ZERO, BigDecimal::add));
			itemRepository.save(item);
		}

		itemRepository.findByProjectIdOrderByItemIdAsc(projectId).stream()
				.filter(item -> !byItem.containsKey(item.getItemId()))
				.forEach(item -> {
					item.updateBudgetAmount(BigDecimal.ZERO);
					itemRepository.save(item);
				});
	}

	private ExecutionBudgetDetailResponse toResponse(Project project) {
		List<ProjectExecutionBudgetDetail> details = detailRepository.findByProjectIdOrderByBudgetItemIdAscDetailIdAsc(project.getProjectId());
		List<ProjectExecutionBudgetItem> items = itemRepository.findByProjectIdOrderByItemIdAsc(project.getProjectId());
		BigDecimal totalDetailAmount = details.stream()
				.map(ProjectExecutionBudgetDetail::getAmount)
				.map(ExecutionBudgetDetailService::defaultZero)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal contractSupplyAmount = customerContractItemRepository.findByProjectIdOrderByWorkCategoryAsc(project.getProjectId()).stream()
				.map(CustomerContractItem::getContractAmount)
				.map(ExecutionBudgetDetailService::defaultZero)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal targetCostLimit = contractSupplyAmount
				.multiply(BigDecimal.ONE.subtract(defaultZero(project.getTargetProfitRate()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
				.setScale(0, RoundingMode.HALF_UP);

		return new ExecutionBudgetDetailResponse(
				project.getProjectId(),
				project.getProjectCode(),
				project.getProjectName(),
				contractSupplyAmount,
				targetCostLimit,
				"작성중",
				totalDetailAmount,
				targetCostLimit.compareTo(BigDecimal.ZERO) > 0 && totalDetailAmount.compareTo(targetCostLimit) > 0,
				items.stream()
						.sorted(Comparator.comparing(ProjectExecutionBudgetItem::getItemId))
						.map(item -> new SummaryItem(item.getItemId(), item.getCategoryName(), item.getItemName(), item.getBudgetAmount()))
						.toList(),
				details.stream()
						.map(detail -> new Detail(
								detail.getDetailId(),
								detail.getBudgetItemId(),
								detail.getCategoryName(),
								detail.getItemName(),
								detail.getCostType(),
								detail.getVendorDescription(),
								detail.getUnit(),
								detail.getQuantity(),
								detail.getUnitPrice(),
								detail.getAmount(),
								detail.getEvidenceLink(),
								detail.getNote()
						))
						.toList()
		);
	}

	private Project getProject(String projectId) {
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
	}

	private static BigDecimal defaultZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
