package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.Map;

@Builder
public record AiEvidenceItem(
        @JsonProperty("evidence_id") String evidenceId,
        @JsonProperty("source_type") String sourceType,
        @JsonProperty("source_id") String sourceId,
        String summary,
        @JsonProperty("relevance_score") Double relevanceScore,
        Map<String, Object> metadata
) {}
