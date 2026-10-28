package com.construction.management.project.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "daily_log_photos")
public class DailyLogPhoto extends com.construction.management.auth.AuthoredEntity {
	@Id @Column(name = "photo_id", length = 64) private String photoId;
	@Column(name = "daily_log_id", nullable = false, length = 64) private String dailyLogId;
	@Column(nullable = false, length = 16) private String phase;
	@Column(name = "storage_key", nullable = false, length = 1000) private String storageKey;
	@Column(name = "original_file_name", nullable = false, length = 1000) private String originalFileName;
	@Column(name = "mime_type", nullable = false, length = 255) private String mimeType;
	@Column(name = "file_size", nullable = false) private long fileSize;
	@Column(length = 1000) private String caption;
	@Column(name = "sort_order", nullable = false) private int sortOrder;
	@Column(name = "uploaded_by", nullable = false, length = 255) private String uploadedBy;
	@Column(name = "uploaded_at", nullable = false) private LocalDateTime uploadedAt;
	protected DailyLogPhoto() { }
	public DailyLogPhoto(String photoId, String dailyLogId, String phase, String storageKey, String originalFileName, String mimeType, long fileSize, String caption, int sortOrder, String uploadedBy) { this.photoId = photoId; this.dailyLogId = dailyLogId; this.phase = phase; this.storageKey = storageKey; this.originalFileName = originalFileName; this.mimeType = mimeType; this.fileSize = fileSize; this.caption = caption; this.sortOrder = sortOrder; this.uploadedBy = uploadedBy; }
	@PrePersist void onCreate() { uploadedAt = LocalDateTime.now(); }
	public String getPhotoId() { return photoId; } public String getDailyLogId() { return dailyLogId; } public String getPhase() { return phase; } public String getStorageKey() { return storageKey; } public String getOriginalFileName() { return originalFileName; } public String getMimeType() { return mimeType; } public long getFileSize() { return fileSize; } public String getCaption() { return caption; } public int getSortOrder() { return sortOrder; } public LocalDateTime getUploadedAt() { return uploadedAt; }
}
