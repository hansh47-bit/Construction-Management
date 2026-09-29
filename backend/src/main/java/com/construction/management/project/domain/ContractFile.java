package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract_files")
public class ContractFile {

	@Id
	@Column(name = "file_id", nullable = false, length = 64)
	private String fileId;

	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(name = "file_path", nullable = false, length = 1000)
	private String filePath;

	@Column(name = "file_type")
	private String fileType;

	@Column(name = "uploaded_at", nullable = false, updatable = false)
	private LocalDateTime uploadedAt;

	protected ContractFile() {
	}

	public ContractFile(String fileId, String projectId, String fileName, String filePath, String fileType) {
		this.fileId = fileId;
		this.projectId = projectId;
		this.fileName = fileName;
		this.filePath = filePath;
		this.fileType = fileType;
	}

	@PrePersist
	void onCreate() {
		this.uploadedAt = LocalDateTime.now();
	}

	public String getFileId() {
		return fileId;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getFileName() {
		return fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getFileType() {
		return fileType;
	}

	public LocalDateTime getUploadedAt() {
		return uploadedAt;
	}
}
