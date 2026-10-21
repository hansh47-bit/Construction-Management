package com.construction.management.project.api;

import java.util.List;

public record ProjectRevisionBudgetView(
		int revisionNumber,
		List<ExecutionBudgetDetailResponse.Detail> details
) { }
