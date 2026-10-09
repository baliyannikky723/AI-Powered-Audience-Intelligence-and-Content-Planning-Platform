package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record AiRecommendationRepairResponse(
        @JsonProperty("request_id") String requestId,
        @JsonProperty("repaired_draft") AiRecommendationDraft repairedDraft,
        @JsonProperty("repair_explanation") String repairExplanation,
        @JsonProperty("execution_time_ms") Double executionTimeMs
) {}
