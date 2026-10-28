package com.construction.management.project.api;

import jakarta.validation.constraints.NotBlank;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
public record ProjectRevisionCreateRequest(
		@NotBlank String title,
		@NotBlank String reason
) { }
