package com.construction.management.project.repository;

import com.construction.management.project.domain.ProjectRevision;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRevisionRepository extends JpaRepository<ProjectRevision, String> {
	List<ProjectRevision> findByProjectIdOrderByRevisionNumberDesc(String projectId);
	Optional<ProjectRevision> findTopByProjectIdAndStatusOrderByRevisionNumberDesc(String projectId, String status);
	void deleteByProjectId(String projectId);
}
