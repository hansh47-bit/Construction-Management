package com.construction.management.project.repository;

import com.construction.management.project.domain.ProjectExecutionBudgetDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectExecutionBudgetDetailRepository extends JpaRepository<ProjectExecutionBudgetDetail, String> {
	List<ProjectExecutionBudgetDetail> findByProjectIdOrderByBudgetItemIdAscDetailIdAsc(String projectId);

	void deleteByProjectId(String projectId);
}
