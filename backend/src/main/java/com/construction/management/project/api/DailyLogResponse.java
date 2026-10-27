package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public record DailyLogResponse(
		@JsonProperty("daily_log_id") String dailyLogId, @JsonProperty("project_id") String projectId, @JsonProperty("log_date") LocalDate logDate,
		@JsonProperty("weather_condition") String weatherCondition, @JsonProperty("temperature_c") BigDecimal temperatureC,
		@JsonProperty("site_manager_name") String siteManagerName, @JsonProperty("today_work_content") String todayWorkContent,
		@JsonProperty("tomorrow_plan") String tomorrowPlan, @JsonProperty("safety_training_completed") boolean safetyTrainingCompleted,
		@JsonProperty("safety_training_note") String safetyTrainingNote, @JsonProperty("complaint_note") String complaintNote,
		@JsonProperty("design_change_issue_note") String designChangeIssueNote, @JsonProperty("other_note") String otherNote, String status,
		@JsonProperty("created_by") String createdBy, @JsonProperty("created_at") LocalDateTime createdAt, @JsonProperty("updated_at") LocalDateTime updatedAt,
		@JsonProperty("labor_entries") List<LaborEntry> laborEntries, @JsonProperty("equipment_entries") List<EquipmentEntry> equipmentEntries,
		@JsonProperty("material_entries") List<MaterialEntry> materialEntries, List<Photo> photos
) {
	public record LaborEntry(Long id, @JsonProperty("trade_name") String tradeName, @JsonProperty("workforce_type") String workforceType, @JsonProperty("vendor_name") String vendorName, BigDecimal headcount, String note) { }
	public record EquipmentEntry(Long id, @JsonProperty("equipment_name") String equipmentName, BigDecimal quantity, String note) { }
	public record MaterialEntry(Long id, @JsonProperty("entry_type") String entryType, @JsonProperty("item_name") String itemName, BigDecimal quantity, String unit, String note) { }
	public record Photo(@JsonProperty("photo_id") String photoId, String phase, @JsonProperty("file_name") String fileName, @JsonProperty("mime_type") String mimeType, @JsonProperty("file_size") long fileSize, String caption, @JsonProperty("download_url") String downloadUrl, @JsonProperty("uploaded_at") LocalDateTime uploadedAt) { }
}
