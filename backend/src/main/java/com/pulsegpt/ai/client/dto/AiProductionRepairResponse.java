package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.Map;

@Builder
public record AiProductionRepairResponse(
        @JsonProperty("request_id") String requestId,
        @JsonProperty("repaired_draft") Map<String, Object> repairedDraft,
        @JsonProperty("repair_explanation") String repairExplanation,
        @JsonProperty("execution_time_ms") Double executionTimeMs
) {}
