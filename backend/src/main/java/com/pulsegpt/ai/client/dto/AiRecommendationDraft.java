package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record AiRecommendationDraft(
        String title,
        @JsonProperty("content_type") String contentType,
        String angle,
        @JsonProperty("target_audience") String targetAudience,
        @JsonProperty("problem_addressed") String problemAddressed,
        @JsonProperty("key_points") List<String> keyPoints,
        String hook,
        @JsonProperty("call_to_action") String callToAction,
        @JsonProperty("evidence_ids") List<String> evidenceIds,
        Double confidence,
        String reason
) {}
