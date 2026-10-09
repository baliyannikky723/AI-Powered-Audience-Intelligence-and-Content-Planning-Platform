package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record AiRecommendationGenerateResponse(
        @JsonProperty("request_id") String requestId,
        @JsonProperty("generation_mode") String generationMode,
        @JsonProperty("prompt_version") String promptVersion,
        @JsonProperty("model_name") String modelName,
        @JsonProperty("model_version") String modelVersion,
        List<AiRecommendationDraft> drafts,
        @JsonProperty("execution_time_ms") Double executionTimeMs
) {}
