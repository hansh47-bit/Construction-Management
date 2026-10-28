package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
public record ApprovalRequest(
		@JsonProperty("request_user")
		String requestUser,
		@JsonProperty("request_reason")
		String requestReason
) {
}
