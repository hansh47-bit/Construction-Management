package com.construction.management.project.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "daily_log_labor_entries")
public class DailyLogLaborEntry {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@Column(name = "daily_log_id", nullable = false, length = 64) private String dailyLogId;
	@Column(name = "trade_name", nullable = false) private String tradeName;
	@Column(name = "workforce_type", length = 32) private String workforceType;
	@Column(name = "vendor_name") private String vendorName;
	@Column(nullable = false, precision = 5, scale = 1) private BigDecimal headcount;
	@Column(length = 1000) private String note;
	@Column(name = "sort_order", nullable = false) private int sortOrder;
	protected DailyLogLaborEntry() { }
	public DailyLogLaborEntry(String dailyLogId, String tradeName, String workforceType, String vendorName, BigDecimal headcount, String note, int sortOrder) { this.dailyLogId = dailyLogId; this.tradeName = tradeName; this.workforceType = workforceType; this.vendorName = vendorName; this.headcount = headcount; this.note = note; this.sortOrder = sortOrder; }
	public Long getId() { return id; } public String getTradeName() { return tradeName; } public String getWorkforceType() { return workforceType; } public String getVendorName() { return vendorName; } public BigDecimal getHeadcount() { return headcount; } public String getNote() { return note; } public int getSortOrder() { return sortOrder; }
}
