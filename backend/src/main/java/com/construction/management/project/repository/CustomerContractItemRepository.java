package com.construction.management.project.repository;

import com.construction.management.project.domain.CustomerContractItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerContractItemRepository extends JpaRepository<CustomerContractItem, String> {
	List<CustomerContractItem> findByProjectIdOrderByWorkCategoryAsc(String projectId);

	void deleteByProjectId(String projectId);
}
