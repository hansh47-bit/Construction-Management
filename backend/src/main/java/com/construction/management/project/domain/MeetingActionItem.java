package com.construction.management.project.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "meeting_action_items")
public class MeetingActionItem extends com.construction.management.auth.AuthoredEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@Column(name = "meeting_id", nullable = false, length = 64) private String meetingId;
	@Column(name = "action_content", nullable = false, length = 4000) private String actionContent;
	@Column(name = "assignee_name") private String assigneeName;
	@Column(name = "assignee_user_id", length = 255) private String assigneeUserId;
	@Column(name = "due_date") private LocalDate dueDate;
	@Column(nullable = false, length = 32) private String status;
	@Column(name = "completed_at") private LocalDateTime completedAt;
	@Column(name = "sort_order", nullable = false) private int sortOrder;
	protected MeetingActionItem() { }
	public MeetingActionItem(String meetingId, String actionContent, String assigneeName, String assigneeUserId, LocalDate dueDate, String status, int sortOrder) { this.meetingId = meetingId; this.actionContent = actionContent; this.assigneeName = assigneeName; this.assigneeUserId = assigneeUserId; this.dueDate = dueDate; this.status = status; this.completedAt = "DONE".equals(status) ? LocalDateTime.now() : null; this.sortOrder = sortOrder; }
	public Long getId() { return id; } public String getActionContent() { return actionContent; } public String getAssigneeName() { return assigneeName; } public String getAssigneeUserId() { return assigneeUserId; } public LocalDate getDueDate() { return dueDate; } public String getStatus() { return status; } public LocalDateTime getCompletedAt() { return completedAt; } public int getSortOrder() { return sortOrder; }
}
