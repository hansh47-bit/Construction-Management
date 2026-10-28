package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_logs")
public class DailyLog extends com.construction.management.auth.AuthoredEntity {

	@Id
	@Column(name = "daily_log_id", nullable = false, length = 64)
	private String dailyLogId;
	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;
	@Column(name = "log_date", nullable = false)
	private LocalDate logDate;
	@Column(name = "weather_condition", length = 100)
	private String weatherCondition;
	@Column(name = "temperature_c")
	private java.math.BigDecimal temperatureC;
	@Column(name = "site_manager_name", nullable = false, length = 255)
	private String siteManagerName;
	@Column(name = "today_work_content", length = 8000)
	private String todayWorkContent;
	@Column(name = "tomorrow_plan", length = 8000)
	private String tomorrowPlan;
	@Column(name = "safety_training_completed", nullable = false)
	private boolean safetyTrainingCompleted;
	@Column(name = "safety_training_note", length = 2000)
	private String safetyTrainingNote;
	@Column(name = "complaint_note", length = 4000)
	private String complaintNote;
	@Column(name = "design_change_issue_note", length = 4000)
	private String designChangeIssueNote;
	@Column(name = "other_note", length = 4000)
	private String otherNote;
	@Column(nullable = false, length = 32)
	private String status = "DRAFT";
	@Column(name = "created_by", nullable = false, length = 255)
	private String createdBy;
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
	@Column(name = "updated_by", length = 255)
	private String updatedBy;
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected DailyLog() { }

	public DailyLog(String dailyLogId, String projectId, LocalDate logDate, String createdBy) {
		this.dailyLogId = dailyLogId;
		this.projectId = projectId;
		this.logDate = logDate;
		this.createdBy = createdBy;
	}

	public DailyLog update(String weatherCondition, java.math.BigDecimal temperatureC, String siteManagerName,
			String todayWorkContent, String tomorrowPlan, boolean safetyTrainingCompleted, String safetyTrainingNote,
			String complaintNote, String designChangeIssueNote, String otherNote, String status, String updatedBy) {
		this.weatherCondition = weatherCondition;
		this.temperatureC = temperatureC;
		this.siteManagerName = siteManagerName;
		this.todayWorkContent = todayWorkContent;
		this.tomorrowPlan = tomorrowPlan;
		this.safetyTrainingCompleted = safetyTrainingCompleted;
		this.safetyTrainingNote = safetyTrainingNote;
		this.complaintNote = complaintNote;
		this.designChangeIssueNote = designChangeIssueNote;
		this.otherNote = otherNote;
		this.status = status;
		this.updatedBy = updatedBy;
		return this;
	}

	@PrePersist void onCreate() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
	@PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
	public String getDailyLogId() { return dailyLogId; }
	public String getProjectId() { return projectId; }
	public LocalDate getLogDate() { return logDate; }
	public String getWeatherCondition() { return weatherCondition; }
	public java.math.BigDecimal getTemperatureC() { return temperatureC; }
	public String getSiteManagerName() { return siteManagerName; }
	public String getTodayWorkContent() { return todayWorkContent; }
	public String getTomorrowPlan() { return tomorrowPlan; }
	public boolean isSafetyTrainingCompleted() { return safetyTrainingCompleted; }
	public String getSafetyTrainingNote() { return safetyTrainingNote; }
	public String getComplaintNote() { return complaintNote; }
	public String getDesignChangeIssueNote() { return designChangeIssueNote; }
	public String getOtherNote() { return otherNote; }
	public String getStatus() { return status; }
	public String getCreatedBy() { return createdBy; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public String getUpdatedBy() { return updatedBy; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
}
