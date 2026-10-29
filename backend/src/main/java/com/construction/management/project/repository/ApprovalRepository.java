package com.construction.management.project.repository;

import com.construction.management.project.domain.Approval;
import com.construction.management.project.domain.ApprovalStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRepository extends JpaRepository<Approval, String> {
	@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@org.springframework.data.jpa.repository.Query("select a from Approval a where a.approvalId=:id")
	java.util.Optional<Approval> findForUpdate(@org.springframework.data.repository.query.Param("id") String id);
	List<Approval> findByOrderByRequestedAtDesc();
	List<Approval> findByStatusOrderByRequestedAtDesc(ApprovalStatus status);
	List<Approval> findByApprovalTypeOrderByRequestedAtDesc(String approvalType);
	List<Approval> findByProjectIdOrderByRequestedAtDesc(String projectId);
	boolean existsByProjectId(String projectId);
	void deleteByProjectId(String projectId);
	List<Approval> findByStatusAndApprovalTypeOrderByRequestedAtDesc(ApprovalStatus status, String approvalType);
	List<Approval> findByStatusAndProjectIdOrderByRequestedAtDesc(ApprovalStatus status, String projectId);
	List<Approval> findByApprovalTypeAndProjectIdOrderByRequestedAtDesc(String approvalType, String projectId);
	List<Approval> findByStatusAndApprovalTypeAndProjectIdOrderByRequestedAtDesc(ApprovalStatus status, String approvalType, String projectId);
	boolean existsByProjectIdAndApprovalTypeAndStatus(String projectId, String approvalType, ApprovalStatus status);
	boolean existsByProjectIdAndApprovalTypeAndStatusIn(String projectId, String approvalType, List<ApprovalStatus> statuses);
	boolean existsByProjectIdAndApprovalTypeAndTargetIdAndStatus(String projectId, String approvalType, String targetId, ApprovalStatus status);
}
