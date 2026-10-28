package com.construction.management.project.api;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
public record ContractChangeRequest(@NotNull BigDecimal changeAmount, String reason) { }
