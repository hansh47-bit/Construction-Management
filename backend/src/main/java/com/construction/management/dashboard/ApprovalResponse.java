package com.construction.management.dashboard;

public record ApprovalResponse(
		String id,
		String title,
		String requester,
		String amount,
		String status
) {
}
