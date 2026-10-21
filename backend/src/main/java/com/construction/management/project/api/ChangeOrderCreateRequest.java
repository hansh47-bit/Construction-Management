package com.construction.management.project.api;

import jakarta.validation.constraints.NotBlank;

public record ChangeOrderCreateRequest(
		@NotBlank String title,
		@NotBlank String reason,
		String requestedBy,
		Boolean contractChangeEnabled,
		Boolean budgetChangeEnabled
) { }
