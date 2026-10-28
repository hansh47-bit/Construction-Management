package com.construction.management.project.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "daily_log_equipment_entries")
public class DailyLogEquipmentEntry extends com.construction.management.auth.AuthoredEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@Column(name = "daily_log_id", nullable = false, length = 64) private String dailyLogId;
	@Column(name = "equipment_name", nullable = false) private String equipmentName;
	@Column(nullable = false, precision = 8, scale = 2) private BigDecimal quantity;
	@Column(length = 1000) private String note;
	@Column(name = "sort_order", nullable = false) private int sortOrder;
	protected DailyLogEquipmentEntry() { }
	public DailyLogEquipmentEntry(String dailyLogId, String equipmentName, BigDecimal quantity, String note, int sortOrder) { this.dailyLogId = dailyLogId; this.equipmentName = equipmentName; this.quantity = quantity; this.note = note; this.sortOrder = sortOrder; }
	public Long getId() { return id; } public String getEquipmentName() { return equipmentName; } public BigDecimal getQuantity() { return quantity; } public String getNote() { return note; } public int getSortOrder() { return sortOrder; }
}
