package com.construction.management.project.repository;

import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectExecutionBudgetItemRepository extends JpaRepository<ProjectExecutionBudgetItem, String> {
	List<ProjectExecutionBudgetItem> findByProjectIdOrderByItemIdAsc(String projectId);
}
