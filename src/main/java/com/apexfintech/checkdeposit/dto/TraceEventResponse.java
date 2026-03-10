package com.apexfintech.checkdeposit.dto;

import com.apexfintech.checkdeposit.domain.TraceStage;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/** Response DTO for a trace event in GET /deposits/{transferId}/trace. */
public record TraceEventResponse(
    TraceStage stage, String outcome, JsonNode detail, Instant timestamp) {}
