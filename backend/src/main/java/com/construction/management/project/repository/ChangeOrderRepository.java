package com.construction.management.project.repository;

import com.construction.management.project.domain.ChangeOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeOrderRepository extends JpaRepository<ChangeOrder, String> {
	List<ChangeOrder> findByProjectIdOrderByCreatedAtDesc(String projectId);
	boolean existsByProjectId(String projectId);
	void deleteByProjectId(String projectId);
	List<ChangeOrder> findByChangeOrderIdStartingWithOrderByChangeOrderIdDesc(String prefix);
}
