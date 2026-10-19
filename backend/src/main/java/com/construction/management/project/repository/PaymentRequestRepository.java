package com.construction.management.project.repository;

import com.construction.management.project.domain.PaymentRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, String> {
	Optional<PaymentRequest> findByApprovalId(String approvalId);
	List<PaymentRequest> findByProjectId(String projectId);
	boolean existsByProjectId(String projectId);
	void deleteByProjectId(String projectId);
	boolean existsByProjectIdAndPoIdAndApprovalStatus(String projectId, String poId, String approvalStatus);
}
