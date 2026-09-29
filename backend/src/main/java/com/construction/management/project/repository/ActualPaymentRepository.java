package com.construction.management.project.repository;

import com.construction.management.project.domain.ActualPayment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActualPaymentRepository extends JpaRepository<ActualPayment, String> {
	List<ActualPayment> findByClaimIdIn(List<String> claimIds);
	List<ActualPayment> findByClaimId(String claimId);
}
