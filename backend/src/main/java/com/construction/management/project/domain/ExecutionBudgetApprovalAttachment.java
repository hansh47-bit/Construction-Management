package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "execution_budget_approval_attachments")
public class ExecutionBudgetApprovalAttachment extends com.construction.management.auth.AuthoredEntity {

	@Id
	@Column(name = "attachment_id", nullable = false, length = 64)
	private String attachmentId;

	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "approval_id", nullable = false, length = 64)
	private String approvalId;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(name = "file_path", nullable = false, length = 1000)
	private String filePath;

	@Column(name = "content_type", length = 255)
	private String contentType;

	@Column(name = "uploaded_at", nullable = false, updatable = false)
	private LocalDateTime uploadedAt;

	protected ExecutionBudgetApprovalAttachment() {
	}

	public ExecutionBudgetApprovalAttachment(String attachmentId, String projectId, String approvalId, String fileName, String filePath, String contentType) {
		this.attachmentId = attachmentId;
		this.projectId = projectId;
		this.approvalId = approvalId;
		this.fileName = fileName;
		this.filePath = filePath;
		this.contentType = contentType;
	}

	@PrePersist
	void onCreate() {
		this.uploadedAt = LocalDateTime.now();
	}

	public String getAttachmentId() { return attachmentId; }
	public String getProjectId() { return projectId; }
	public String getApprovalId() { return approvalId; }
	public String getFileName() { return fileName; }
	public String getFilePath() { return filePath; }
	public String getContentType() { return contentType; }
}
