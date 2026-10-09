package com.pulsegpt.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record AiRecommendationGenerateRequest(
        @JsonProperty("request_id") String requestId,
        @JsonProperty("topic_id") String topicId,
        @JsonProperty("topic_name") String topicName,
        @JsonProperty("content_type") String contentType,
        String goal,
        @JsonProperty("target_audience") String targetAudience,
        @JsonProperty("max_ideas") Integer maxIdeas,
        String mode,
        List<AiEvidenceItem> evidence,
        @JsonProperty("content_history") List<Map<String, Object>> contentHistory,
        @JsonProperty("previous_recommendations") List<Map<String, Object>> previousRecommendations
) {}
