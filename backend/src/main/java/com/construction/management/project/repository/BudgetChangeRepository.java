package com.construction.management.project.repository;

import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.BudgetChange;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetChangeRepository extends JpaRepository<BudgetChange, String> {
	List<BudgetChange> findByBudgetIdInAndApprovalStatus(List<String> budgetIds, ApprovalStatus approvalStatus);
}
