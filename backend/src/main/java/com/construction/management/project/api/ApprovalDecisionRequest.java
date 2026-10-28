package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
public record ApprovalDecisionRequest(
		String approver,
		String comment
) {
}
