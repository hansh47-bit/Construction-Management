package com.construction.management.project.repository;

import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.ProgressClaim;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressClaimRepository extends JpaRepository<ProgressClaim, String> {
	List<ProgressClaim> findByPoIdInAndApprovalStatus(List<String> poIds, ApprovalStatus approvalStatus);
	List<ProgressClaim> findByPoId(String poId);
	List<ProgressClaim> findByPoIdIn(List<String> poIds);
}
