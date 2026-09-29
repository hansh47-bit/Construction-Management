package com.construction.management.project.repository;

import com.construction.management.project.domain.ProjectSettlement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectSettlementRepository extends JpaRepository<ProjectSettlement, String> {
	Optional<ProjectSettlement> findByProjectId(String projectId);
}
