package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record AiProductionGenerateRequest(
        @JsonProperty("request_id") String requestId,
        @JsonProperty("recommendation_id") String recommendationId,
        @JsonProperty("calendar_item_id") String calendarItemId,
        @JsonProperty("recommendation_title") String recommendationTitle,
        @JsonProperty("recommendation_angle") String recommendationAngle,
        @JsonProperty("content_type") String contentType,
        String platform,
        @JsonProperty("target_audience") String targetAudience,
        @JsonProperty("problem_addressed") String problemAddressed,
        @JsonProperty("key_points") List<String> keyPoints,
        @JsonProperty("topic_name") String topicName,
        @JsonProperty("topic_keywords") List<String> topicKeywords,
        List<AiEvidenceItem> evidence,
        String mode,
        @JsonProperty("requested_asset_types") List<String> requestedAssetTypes
) {}
