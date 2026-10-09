package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record AiProductionRepairRequest(
        @JsonProperty("request_id") String requestId,
        @JsonProperty("original_draft") Map<String, Object> originalDraft,
        @JsonProperty("failed_checks") List<Map<String, Object>> failedChecks,
        List<AiEvidenceItem> evidence,
        @JsonProperty("recommendation_title") String recommendationTitle,
        @JsonProperty("content_type") String contentType,
        String platform
) {}
