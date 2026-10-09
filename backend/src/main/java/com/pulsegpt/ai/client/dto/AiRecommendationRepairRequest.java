package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record AiRecommendationRepairRequest(
        @JsonProperty("request_id") String requestId,
        @JsonProperty("original_draft") AiRecommendationDraft originalDraft,
        @JsonProperty("failed_checks") List<Map<String, Object>> failedChecks,
        List<AiEvidenceItem> evidence,
        String mode
) {}
