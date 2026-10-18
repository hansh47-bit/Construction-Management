package com.construction.management.project.repository;

import com.construction.management.project.domain.PurchaseOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {
	List<PurchaseOrder> findByProjectId(String projectId);
	List<PurchaseOrder> findByProjectIdOrderByPoIdDesc(String projectId);
	Optional<PurchaseOrder> findByRequisitionId(String requisitionId);
	List<PurchaseOrder> findByPoIdStartingWithOrderByPoIdDesc(String poIdPrefix);
	boolean existsByProjectIdAndBudgetId(String projectId, String budgetId);
}
