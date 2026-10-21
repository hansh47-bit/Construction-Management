package com.construction.management.project.api;

import jakarta.validation.constraints.NotBlank;

public record ProjectRevisionCreateRequest(
		@NotBlank String title,
		@NotBlank String reason
) { }
