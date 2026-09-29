package com.construction.management.project.repository;

import com.construction.management.project.domain.VendorComparison;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorComparisonRepository extends JpaRepository<VendorComparison, String> {
	List<VendorComparison> findByProjectIdAndExecutionItemIdOrderByComparisonIdAsc(String projectId, String executionItemId);
	Optional<VendorComparison> findByProjectIdAndExecutionItemIdAndSelectedTrue(String projectId, String executionItemId);
	void deleteByProjectIdAndExecutionItemId(String projectId, String executionItemId);
}
