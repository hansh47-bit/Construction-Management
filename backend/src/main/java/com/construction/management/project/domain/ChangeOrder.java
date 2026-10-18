package com.construction.management.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "change_orders")
public class ChangeOrder {

	@Id
	@Column(name = "change_order_id", nullable = false, length = 64)
	private String changeOrderId;

	@Column(name = "project_id", nullable = false, length = 64)
	private String projectId;

	@Column(name = "title", nullable = false, length = 255)
	private String title;

	@Column(name = "reason", nullable = false, length = 1000)
	private String reason;

	@Column(name = "requested_by", nullable = false, length = 255)
	private String requestedBy;

	@Column(name = "status", nullable = false, length = 32)
	private String status;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	protected ChangeOrder() { }

	public ChangeOrder(String changeOrderId, String projectId, String title, String reason, String requestedBy) {
		this.changeOrderId = changeOrderId;
		this.projectId = projectId;
		this.title = title;
		this.reason = reason;
		this.requestedBy = requestedBy;
		this.status = "APPROVED";
		this.createdAt = LocalDateTime.now();
	}

	public String getChangeOrderId() { return changeOrderId; }
	public String getProjectId() { return projectId; }
	public String getTitle() { return title; }
	public String getReason() { return reason; }
	public String getRequestedBy() { return requestedBy; }
	public String getStatus() { return status; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
