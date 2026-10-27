package com.construction.management.project.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "meeting_attachments")
public class MeetingAttachment {
	@Id @Column(name = "attachment_id", length = 64) private String attachmentId;
	@Column(name = "meeting_id", nullable = false, length = 64) private String meetingId;
	@Column(name = "storage_key", nullable = false, length = 1000) private String storageKey;
	@Column(name = "original_file_name", nullable = false, length = 1000) private String originalFileName;
	@Column(name = "mime_type", nullable = false, length = 255) private String mimeType;
	@Column(name = "file_size", nullable = false) private long fileSize;
	@Column(name = "uploaded_by", nullable = false, length = 255) private String uploadedBy;
	@Column(name = "uploaded_at", nullable = false) private LocalDateTime uploadedAt;
	protected MeetingAttachment() { }
	public MeetingAttachment(String attachmentId, String meetingId, String storageKey, String originalFileName, String mimeType, long fileSize, String uploadedBy) { this.attachmentId = attachmentId; this.meetingId = meetingId; this.storageKey = storageKey; this.originalFileName = originalFileName; this.mimeType = mimeType; this.fileSize = fileSize; this.uploadedBy = uploadedBy; }
	@PrePersist void onCreate() { uploadedAt = LocalDateTime.now(); }
	public String getAttachmentId() { return attachmentId; } public String getMeetingId() { return meetingId; } public String getStorageKey() { return storageKey; } public String getOriginalFileName() { return originalFileName; } public String getMimeType() { return mimeType; } public long getFileSize() { return fileSize; } public LocalDateTime getUploadedAt() { return uploadedAt; }
}
