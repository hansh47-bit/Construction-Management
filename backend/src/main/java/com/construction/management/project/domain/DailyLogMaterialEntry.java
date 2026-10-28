package com.construction.management.project.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "daily_log_material_entries")
public class DailyLogMaterialEntry extends com.construction.management.auth.AuthoredEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@Column(name = "daily_log_id", nullable = false, length = 64) private String dailyLogId;
	@Column(name = "entry_type", nullable = false, length = 32) private String entryType;
	@Column(name = "item_name", nullable = false) private String itemName;
	@Column(precision = 12, scale = 3) private BigDecimal quantity;
	@Column(length = 64) private String unit;
	@Column(length = 1000) private String note;
	@Column(name = "sort_order", nullable = false) private int sortOrder;
	protected DailyLogMaterialEntry() { }
	public DailyLogMaterialEntry(String dailyLogId, String entryType, String itemName, BigDecimal quantity, String unit, String note, int sortOrder) { this.dailyLogId = dailyLogId; this.entryType = entryType; this.itemName = itemName; this.quantity = quantity; this.unit = unit; this.note = note; this.sortOrder = sortOrder; }
	public Long getId() { return id; } public String getEntryType() { return entryType; } public String getItemName() { return itemName; } public BigDecimal getQuantity() { return quantity; } public String getUnit() { return unit; } public String getNote() { return note; } public int getSortOrder() { return sortOrder; }
}
