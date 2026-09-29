package com.construction.management.project.repository;

import com.construction.management.project.domain.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, String> {
	boolean existsByProjectCode(String projectCode);

	List<Project> findTop20ByProjectNameContainingIgnoreCaseOrProjectCodeContainingIgnoreCaseOrderByCreatedAtDesc(
			String projectName,
			String projectCode
	);

	List<Project> findTop20ByOrderByCreatedAtDesc();
}
