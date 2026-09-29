package com.construction.management.dashboard;

import java.util.List;

public record DashboardResponse(
		List<MetricResponse> metrics,
		List<WorkflowStepResponse> workflow,
		List<ApprovalResponse> approvals
) {
}
