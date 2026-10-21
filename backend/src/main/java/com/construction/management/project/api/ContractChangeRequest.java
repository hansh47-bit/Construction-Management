package com.construction.management.project.api;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ContractChangeRequest(@NotNull BigDecimal changeAmount, String reason) { }
