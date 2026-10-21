package com.construction.management.project.repository;

import com.construction.management.project.domain.PoRequisition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoRequisitionRepository extends JpaRepository<PoRequisition, String> {
	List<PoRequisition> findByProjectIdOrderByCreatedAtDesc(String projectId);
	List<PoRequisition> findByProjectIdAndExecutionItemIdAndRevisionIdAndApprovalStatus(String projectId, String executionItemId, String revisionId, String approvalStatus);
	Optional<PoRequisition> findTopByProjectIdAndExecutionItemIdAndRevisionIdOrderByCreatedAtDesc(String projectId, String executionItemId, String revisionId);
	Optional<PoRequisition> findByApprovalId(String approvalId);
	boolean existsByProjectId(String projectId);
	void deleteByProjectId(String projectId);
}
