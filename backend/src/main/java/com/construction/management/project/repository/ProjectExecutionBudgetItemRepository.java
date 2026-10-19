package com.construction.management.project.repository;

import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectExecutionBudgetItemRepository extends JpaRepository<ProjectExecutionBudgetItem, String> {
	List<ProjectExecutionBudgetItem> findByProjectIdOrderByItemIdAsc(String projectId);
	Optional<ProjectExecutionBudgetItem> findByProjectIdAndItemId(String projectId, String itemId);

	default Optional<ProjectExecutionBudgetItem> findByProjectIdAndBudgetItemId(String projectId, String budgetItemId) {
		return findById(ProjectExecutionBudgetItem.storageId(projectId, budgetItemId))
				.or(() -> findByProjectIdAndItemId(projectId, budgetItemId));
	}
	boolean existsByProjectId(String projectId);
	void deleteByProjectId(String projectId);
}
