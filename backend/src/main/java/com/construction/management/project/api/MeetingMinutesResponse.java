package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.*;
import java.util.List;

public record MeetingMinutesResponse(
		@JsonProperty("meeting_id") String meetingId, @JsonProperty("project_id") String projectId, @JsonProperty("meeting_at") LocalDateTime meetingAt,
		String location, String subject, @JsonProperty("agenda_content") String agendaContent, @JsonProperty("decision_content") String decisionContent,
		String status, @JsonProperty("created_by") String createdBy,
        @JsonProperty("created_user_id") String createdUserId, @JsonProperty("created_at") LocalDateTime createdAt,
		@JsonProperty("updated_at") LocalDateTime updatedAt, @JsonProperty("open_action_item_count") long openActionItemCount,
		List<Attendee> attendees, @JsonProperty("action_items") List<ActionItem> actionItems, List<Attachment> attachments
) {
	public record Attendee(Long id, @JsonProperty("organization_type") String organizationType, @JsonProperty("organization_name") String organizationName, @JsonProperty("attendee_name") String attendeeName, @JsonProperty("position_name") String positionName, String contact) { }
	public record ActionItem(Long id, @JsonProperty("action_content") String actionContent, @JsonProperty("assignee_name") String assigneeName, @JsonProperty("assignee_user_id") String assigneeUserId, @JsonProperty("due_date") LocalDate dueDate, String status, @JsonProperty("completed_at") LocalDateTime completedAt) { }
	public record Attachment(@JsonProperty("attachment_id") String attachmentId, @JsonProperty("file_name") String fileName, @JsonProperty("mime_type") String mimeType, @JsonProperty("file_size") long fileSize, @JsonProperty("download_url") String downloadUrl, @JsonProperty("uploaded_at") LocalDateTime uploadedAt) { }
}
