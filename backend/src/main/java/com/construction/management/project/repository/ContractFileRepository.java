package com.construction.management.project.repository;

import com.construction.management.project.domain.ContractFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractFileRepository extends JpaRepository<ContractFile, String> {
	List<ContractFile> findByProjectIdOrderByUploadedAtDesc(String projectId);
}
