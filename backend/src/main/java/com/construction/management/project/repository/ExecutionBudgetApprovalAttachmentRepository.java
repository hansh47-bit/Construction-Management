package com.construction.management.project.repository;

import com.construction.management.project.domain.ExecutionBudgetApprovalAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionBudgetApprovalAttachmentRepository extends JpaRepository<ExecutionBudgetApprovalAttachment, String> {
	List<ExecutionBudgetApprovalAttachment> findByApprovalIdOrderByUploadedAtAsc(String approvalId);
	List<ExecutionBudgetApprovalAttachment> findByProjectIdOrderByUploadedAtAsc(String projectId);
	void deleteByProjectId(String projectId);
}
