package com.construction.management.project.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record BudgetChangeProposalRequest(@NotEmpty List<@Valid Item> items) {
	public record Item(@NotBlank String budgetItemId, @NotNull BigDecimal changeAmount) { }
}
