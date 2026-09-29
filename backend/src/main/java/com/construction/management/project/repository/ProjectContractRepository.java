package com.construction.management.project.repository;

import com.construction.management.project.domain.ProjectContract;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectContractRepository extends JpaRepository<ProjectContract, String> {
	List<ProjectContract> findByProjectId(String projectId);
}
