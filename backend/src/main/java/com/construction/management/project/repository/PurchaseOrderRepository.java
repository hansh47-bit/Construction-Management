package com.construction.management.project.repository;

import com.construction.management.project.domain.PurchaseOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {
	List<PurchaseOrder> findByProjectId(String projectId);
	boolean existsByProjectIdAndBudgetId(String projectId, String budgetId);
}
