package com.construction.management.project.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "meeting_minutes")
public class MeetingMinutes {
	@Id @Column(name = "meeting_id", nullable = false, length = 64) private String meetingId;
	@Column(name = "project_id", nullable = false, length = 64) private String projectId;
	@Column(name = "meeting_at", nullable = false) private LocalDateTime meetingAt;
	@Column(length = 500) private String location;
	@Column(nullable = false, length = 1000) private String subject;
	@Column(name = "agenda_content", length = 16000) private String agendaContent;
	@Column(name = "decision_content", length = 16000) private String decisionContent;
	@Column(nullable = false, length = 32) private String status = "DRAFT";
	@Column(name = "created_by", nullable = false, length = 255) private String createdBy;
	@Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
	@Column(name = "updated_by", length = 255) private String updatedBy;
	@Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
	protected MeetingMinutes() { }
	public MeetingMinutes(String meetingId, String projectId, LocalDateTime meetingAt, String createdBy) { this.meetingId = meetingId; this.projectId = projectId; this.meetingAt = meetingAt; this.createdBy = createdBy; }
	public MeetingMinutes update(LocalDateTime meetingAt, String location, String subject, String agendaContent, String decisionContent, String status, String updatedBy) { this.meetingAt = meetingAt; this.location = location; this.subject = subject; this.agendaContent = agendaContent; this.decisionContent = decisionContent; this.status = status; this.updatedBy = updatedBy; return this; }
	@PrePersist void onCreate() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
	@PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
	public String getMeetingId() { return meetingId; } public String getProjectId() { return projectId; } public LocalDateTime getMeetingAt() { return meetingAt; } public String getLocation() { return location; } public String getSubject() { return subject; } public String getAgendaContent() { return agendaContent; } public String getDecisionContent() { return decisionContent; } public String getStatus() { return status; } public String getCreatedBy() { return createdBy; } public LocalDateTime getCreatedAt() { return createdAt; } public String getUpdatedBy() { return updatedBy; } public LocalDateTime getUpdatedAt() { return updatedAt; }
}
