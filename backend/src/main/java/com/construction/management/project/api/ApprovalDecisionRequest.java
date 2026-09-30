package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApprovalDecisionRequest(
		String approver,
		String comment
) {
}
