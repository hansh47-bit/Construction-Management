package com.construction.management.project.repository;

import com.construction.management.project.domain.VendorComparisonAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorComparisonAttachmentRepository extends JpaRepository<VendorComparisonAttachment, String> {
	List<VendorComparisonAttachment> findByProjectIdAndExecutionItemIdAndComparisonIdOrderByUploadedAtAsc(
			String projectId, String executionItemId, String comparisonId
	);
	List<VendorComparisonAttachment> findByProjectIdOrderByUploadedAtAsc(String projectId);
	void deleteByProjectId(String projectId);
}
