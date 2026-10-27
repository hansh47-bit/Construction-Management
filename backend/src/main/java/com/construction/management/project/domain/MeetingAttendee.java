package com.construction.management.project.domain;

import jakarta.persistence.*;

@Entity @Table(name = "meeting_attendees")
public class MeetingAttendee {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@Column(name = "meeting_id", nullable = false, length = 64) private String meetingId;
	@Column(name = "organization_type", nullable = false, length = 32) private String organizationType;
	@Column(name = "organization_name", nullable = false) private String organizationName;
	@Column(name = "attendee_name", nullable = false) private String attendeeName;
	@Column(name = "position_name") private String positionName;
	@Column(length = 100) private String contact;
	@Column(name = "sort_order", nullable = false) private int sortOrder;
	protected MeetingAttendee() { }
	public MeetingAttendee(String meetingId, String organizationType, String organizationName, String attendeeName, String positionName, String contact, int sortOrder) { this.meetingId = meetingId; this.organizationType = organizationType; this.organizationName = organizationName; this.attendeeName = attendeeName; this.positionName = positionName; this.contact = contact; this.sortOrder = sortOrder; }
	public Long getId() { return id; } public String getOrganizationType() { return organizationType; } public String getOrganizationName() { return organizationName; } public String getAttendeeName() { return attendeeName; } public String getPositionName() { return positionName; } public String getContact() { return contact; } public int getSortOrder() { return sortOrder; }
}
