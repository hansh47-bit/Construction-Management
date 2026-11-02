package com.construction.management.project.api;

import java.util.List;

public record ProjectRevisionBudgetView(
		String revisionId,
		int revisionNumber,
		String status,
		List<ExecutionBudgetDetailResponse.Detail> details
) { }
