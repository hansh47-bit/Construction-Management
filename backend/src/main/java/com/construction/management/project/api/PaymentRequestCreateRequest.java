package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
public record PaymentRequestCreateRequest(@JsonProperty("po_ids") List<String> poIds) { }
