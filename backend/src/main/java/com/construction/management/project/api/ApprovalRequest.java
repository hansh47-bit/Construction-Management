package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApprovalRequest(
		@JsonProperty("request_user")
		String requestUser,
		@JsonProperty("request_reason")
		String requestReason
) {
}
