package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.*;
import java.util.List;

public record MeetingMinutesRequest(
		@JsonProperty("meeting_at") @NotNull LocalDateTime meetingAt, String location, @NotBlank String subject,
		@JsonProperty("agenda_content") String agendaContent, @JsonProperty("decision_content") String decisionContent, String status,
		@JsonProperty("created_by") String createdBy, @Valid List<Attendee> attendees, @Valid @JsonProperty("action_items") List<ActionItem> actionItems
) {
	public record Attendee(@JsonProperty("organization_type") String organizationType, @JsonProperty("organization_name") @NotBlank String organizationName, @JsonProperty("attendee_name") @NotBlank String attendeeName, @JsonProperty("position_name") String positionName, String contact) { }
	public record ActionItem(@JsonProperty("action_content") @NotBlank String actionContent, @JsonProperty("assignee_name") String assigneeName, @JsonProperty("assignee_user_id") String assigneeUserId, @JsonProperty("due_date") LocalDate dueDate, String status) { }
}
