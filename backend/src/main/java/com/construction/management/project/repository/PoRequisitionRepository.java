package com.construction.management.project.repository;

import com.construction.management.project.domain.PoRequisition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoRequisitionRepository extends JpaRepository<PoRequisition, String> {
	List<PoRequisition> findByProjectIdAndExecutionItemIdAndApprovalStatus(String projectId, String executionItemId, String approvalStatus);
	Optional<PoRequisition> findByApprovalId(String approvalId);
}
