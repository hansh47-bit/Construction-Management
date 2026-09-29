package com.construction.management.project.repository;

import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.PoChange;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoChangeRepository extends JpaRepository<PoChange, String> {
	List<PoChange> findByPoIdInAndApprovalStatus(List<String> poIds, ApprovalStatus approvalStatus);
}
