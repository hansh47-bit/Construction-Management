package com.construction.management.project.repository;

import com.construction.management.project.domain.ExecutionBudget;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionBudgetRepository extends JpaRepository<ExecutionBudget, String> {
	List<ExecutionBudget> findByProjectId(String projectId);
}
