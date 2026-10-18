package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PaymentRequestCreateRequest(@JsonProperty("po_ids") List<String> poIds) { }
