package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
public record DailyLogRequest(
		@JsonProperty("log_date") @NotNull LocalDate logDate,
		@JsonProperty("weather_condition") String weatherCondition,
		@JsonProperty("temperature_c") BigDecimal temperatureC,
		@JsonProperty("site_manager_name") @NotBlank String siteManagerName,
		@JsonProperty("today_work_content") String todayWorkContent,
		@JsonProperty("tomorrow_plan") String tomorrowPlan,
		@JsonProperty("safety_training_completed") boolean safetyTrainingCompleted,
		@JsonProperty("safety_training_note") String safetyTrainingNote,
		@JsonProperty("complaint_note") String complaintNote,
		@JsonProperty("design_change_issue_note") String designChangeIssueNote,
		@JsonProperty("other_note") String otherNote,
		String status,
		@JsonProperty("created_by") String createdBy,
		@Valid @JsonProperty("labor_entries") List<LaborEntry> laborEntries,
		@Valid @JsonProperty("equipment_entries") List<EquipmentEntry> equipmentEntries,
		@Valid @JsonProperty("material_entries") List<MaterialEntry> materialEntries
) {
	// The editor starts with blank rows for convenience. They are optional and the
	// service omits rows without a trade, equipment name, or material name.
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
public record LaborEntry(@JsonProperty("trade_name") String tradeName, @JsonProperty("workforce_type") String workforceType, @JsonProperty("vendor_name") String vendorName, BigDecimal headcount, String note) { }
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
public record EquipmentEntry(@JsonProperty("equipment_name") String equipmentName, BigDecimal quantity, String note) { }
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
public record MaterialEntry(@JsonProperty("entry_type") String entryType, @JsonProperty("item_name") String itemName, BigDecimal quantity, String unit, String note) { }
}
