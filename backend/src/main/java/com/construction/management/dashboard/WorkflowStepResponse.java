package com.construction.management.dashboard;

public record WorkflowStepResponse(
		int order,
		String phase,
		String title,
		String owner,
		String status
) {
}
