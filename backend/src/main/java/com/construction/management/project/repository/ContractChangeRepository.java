package com.construction.management.project.repository;

import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.ContractChange;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractChangeRepository extends JpaRepository<ContractChange, String> {
	List<ContractChange> findByProjectIdAndApprovalStatus(String projectId, ApprovalStatus approvalStatus);
}
