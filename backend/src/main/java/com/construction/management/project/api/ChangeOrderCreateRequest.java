package com.construction.management.project.api;

import jakarta.validation.constraints.NotBlank;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
public record ChangeOrderCreateRequest(
		@NotBlank String title,
		@NotBlank String reason,
		String requestedBy,
		Boolean contractChangeEnabled,
		Boolean budgetChangeEnabled
) { }
