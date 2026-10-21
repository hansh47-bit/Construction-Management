package com.construction.management.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ProjectRevisionProcurementResponse(
		@JsonProperty("has_approved_revision") boolean hasApprovedRevision,
		@JsonProperty("revision_id") String revisionId,
		@JsonProperty("item_ids") List<String> itemIds
) { }
